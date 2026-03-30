package de.ebusyness.commons.service.setup;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ebusyness.commons.service.entity.EntityService;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractSetupDataImporter<T> implements SetupDataImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSetupDataImporter.class);

    // ThreadLocal to override config flags when called programmatically (e.g., from API)
    private static final ThreadLocal<Boolean> FORCE_LOAD = ThreadLocal.withInitial(() -> false);

    private final EntityService<T> entityService;

    @Value("${service-config.initialize.data-folder}")
    private String dataFolder;

    @Value("${service-config.initialize.essential-data-on}")
    private Boolean essentialDataOn;

    @Value("${service-config.initialize.sample-data-on}")
    private Boolean sampleDataOn;

    public AbstractSetupDataImporter(EntityService<T> entityService) {
        this.entityService = entityService;
    }

    /**
     * Returns the entity type name used as the filename prefix for data files.
     * Files must follow the pattern: {EntityTypeName}.{4-digit-number}.{optional-parts}.json
     * Example: "PriceRow" matches files like PriceRow.0010.EUR.SALES_PRICE.json
     */
    public abstract String getEntityTypeName();

    /**
     * Set force load flag to override configuration.
     * When true, data loading will proceed regardless of application.yaml settings.
     */
    public static void setForceLoad(boolean force) {
        FORCE_LOAD.set(force);
    }

    /**
     * Clear force load flag.
     */
    public static void clearForceLoad() {
        FORCE_LOAD.remove();
    }

    @Override
    public void loadEssentialData() {
        if (Boolean.TRUE.equals(FORCE_LOAD.get()) || Boolean.TRUE.equals(essentialDataOn)) {
            importFilesFromDirectory(dataFolder + "essential/");
        }
    }

    @Override
    public void loadSampleData() {
        if (Boolean.TRUE.equals(FORCE_LOAD.get()) || Boolean.TRUE.equals(sampleDataOn)) {
            importFilesFromDirectory(dataFolder + "sample/");
        }
    }

    protected void importFilesFromDirectory(String directory) {
        Path dirPath = Path.of(directory);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            LOGGER.debug("Data directory {} not found. Skipped.", directory);
            return;
        }
        String prefix = getEntityTypeName() + ".";
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(".json");
                    })
                    .sorted()
                    .forEach(p -> importFile(p.toString()));
        } catch (IOException e) {
            LOGGER.error("Error scanning directory {}: {}", directory, e.getMessage());
        }
    }

    protected void importFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            LOGGER.debug("Lookup data file {} ", filePath);
            if (Files.exists(path)) {
                doImport(filePath, path);
            } else {
                LOGGER.debug("Data file {} not found. Skipped.", filePath);
            }
        } catch (IOException e) {
            LOGGER.error("Error occurred while loading data: " + e.getMessage());
        }
    }

    private void doImport(String filePath, Path path) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Class<T> targetClass = entityService.getTargetClass();
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, targetClass);
        List<T> entities = objectMapper.readValue(Files.readString(path), type);
        for (T entity : entities) {
            try {
                entityService.save(entity);
            } catch (RuntimeException | EntityValidationException e) {
                LOGGER.error("Unexpected RuntimeException in {} for entity {}", filePath, entity, e);
            }
        }
        LOGGER.info("Data file {} successfully imported.", filePath);
    }

    protected String getDataFolder() {
        return dataFolder;
    }

    protected Boolean getSampleDataOn() {
        return sampleDataOn;
    }

    protected Boolean getEssentialDataOn() {
        return essentialDataOn;
    }

}