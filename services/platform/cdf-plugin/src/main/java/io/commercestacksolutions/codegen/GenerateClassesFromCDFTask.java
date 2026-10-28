package io.commercestacksolutions.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.codegen.model.CdfDefinition;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gradle task that reads CDF JSON files, merges definitions for the same entity,
 * and generates Java source files in the configured output directory.
 *
 * <p>CDF files from all provided directories ({@link #getCdfDirectories()})
 * are loaded and merged. Classes are generated for all discovered entities.
 */
public abstract class GenerateClassesFromCDFTask extends DefaultTask {

    /**
     * All CDF directories to scan for definitions.
     */
    @InputFiles
    @SkipWhenEmpty
    public abstract ConfigurableFileCollection getCdfDirectories();

    /** Directory where the generated {@code .java} files are written. */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void mergeAndGenerate() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        CdfMerger merger = new CdfMerger();
        JavaCodeGenerator generator = new JavaCodeGenerator();

        Map<String, List<CdfDefinition>> allDefinitions = new LinkedHashMap<>();
        Set<String> allEntities = new LinkedHashSet<>();

        getCdfDirectories().forEach(dir -> {
            if (dir.exists() && dir.isDirectory()) {
                loadCdfDirectory(dir, mapper, allDefinitions, allEntities);
            }
        });

        if (allEntities.isEmpty()) {
            getLogger().lifecycle("No CDF files found – skipping generation.");
            return;
        }

        File outputRoot = getOutputDirectory().get().getAsFile();
        int generated = 0;

        for (String entityName : allEntities) {
            List<CdfDefinition> defs = allDefinitions.get(entityName);
            if (defs == null || defs.isEmpty()) continue;

            CdfDefinition merged = merger.merge(defs);
            String javaSource = generator.generate(merged);

            String packagePath = merged.getPkg().replace('.', File.separatorChar);
            File packageDir = new File(outputRoot, packagePath);
            packageDir.mkdirs();

            Path outputFile = new File(packageDir, entityName + ".java").toPath();
            Files.writeString(outputFile, javaSource);
            getLogger().lifecycle("Generated: {}", outputFile);
            generated++;
        }

        getLogger().lifecycle("CDF code generation complete. {} class(es) generated.", generated);
    }

    private void loadCdfDirectory(File dir, ObjectMapper mapper,
                                   Map<String, List<CdfDefinition>> target,
                                   Set<String> entities) {
        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsonFiles == null) return;
        for (File jsonFile : jsonFiles) {
            try {
                CdfDefinition def = mapper.readValue(jsonFile, CdfDefinition.class);
                target.computeIfAbsent(def.getEntity(), k -> new ArrayList<>()).add(def);
                entities.add(def.getEntity());
                getLogger().debug("Loaded CDF: {} from {}", def.getEntity(), jsonFile);
            } catch (IOException e) {
                getLogger().error("Failed to parse CDF file: " + jsonFile, e);
                throw new RuntimeException("Failed to parse CDF file: " + jsonFile, e);
            }
        }
    }

}
