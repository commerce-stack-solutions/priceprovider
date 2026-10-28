package io.commercestacksolutions.codegen;

import io.commercestacksolutions.codegen.model.CdfConstructor;
import io.commercestacksolutions.codegen.model.CdfDefinition;
import io.commercestacksolutions.codegen.model.CdfField;
import io.commercestacksolutions.codegen.model.CdfMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Merges multiple CDF definitions for the same entity.
 *
 * <p>Merge semantics:
 * <ul>
 *   <li>imports – union (duplicates removed, order preserved)</li>
 *   <li>classAnnotations – union (duplicates removed)</li>
 *   <li>interfaces – union (duplicates removed)</li>
 *   <li>superClass – base wins; extension may only set if base has none</li>
 *   <li>fields – same-name field: type change is NOT allowed; adding annotations is allowed</li>
 *   <li>methods – extension replaces base method of the same name</li>
 *   <li>constructors – all constructors from all definitions are kept</li>
 * </ul>
 *
 * <p>The first definition passed to {@link #merge(List)} is treated as the base.
 * Subsequent definitions are extensions applied in order.
 */
public class CdfMerger {

    private static final Logger LOGGER = Logger.getLogger(CdfMerger.class.getName());

    /**
     * Merges a list of CDF definitions that all describe the same entity.
     *
     * @param definitions ordered list of CDF definitions (base first, extensions after)
     * @return a single merged CDF definition
     * @throws IllegalArgumentException if the list is empty or entity names do not match
     */
    public CdfDefinition merge(List<CdfDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge an empty list of CDF definitions");
        }
        if (definitions.size() == 1) {
            return definitions.get(0);
        }

        String entityName = definitions.get(0).getEntity();
        for (CdfDefinition d : definitions) {
            if (!entityName.equals(d.getEntity())) {
                throw new IllegalArgumentException(
                        "Cannot merge CDF definitions with different entity names: '"
                                + entityName + "' vs '" + d.getEntity() + "'");
            }
        }

        CdfDefinition merged = new CdfDefinition();
        merged.setEntity(entityName);
        merged.setPkg(definitions.get(0).getPkg());

        // Merge imports (union, preserve order)
        Set<String> importSet = new LinkedHashSet<>();
        for (CdfDefinition d : definitions) {
            importSet.addAll(d.getImports());
        }
        merged.setImports(new ArrayList<>(importSet));

        // Merge classAnnotations (union)
        Set<String> annotationSet = new LinkedHashSet<>();
        for (CdfDefinition d : definitions) {
            annotationSet.addAll(d.getClassAnnotations());
        }
        merged.setClassAnnotations(new ArrayList<>(annotationSet));

        // Merge interfaces (union)
        Set<String> interfaceSet = new LinkedHashSet<>();
        for (CdfDefinition d : definitions) {
            interfaceSet.addAll(d.getInterfaces());
        }
        merged.setInterfaces(new ArrayList<>(interfaceSet));

        // superClass: base wins; extension may set only if base has none
        String superClass = definitions.get(0).getSuperClass();
        for (int i = 1; i < definitions.size(); i++) {
            String extSuperClass = definitions.get(i).getSuperClass();
            if (extSuperClass != null && !extSuperClass.isEmpty()) {
                if (superClass == null || superClass.isEmpty()) {
                    superClass = extSuperClass;
                } else if (!superClass.equals(extSuperClass)) {
                    LOGGER.warning("Extension defines superClass '" + extSuperClass
                            + "' but base already has '" + superClass + "'. Base wins.");
                }
            }
        }
        merged.setSuperClass(superClass);

        // Merge fields: preserve base order, add new fields from extensions
        Map<String, CdfField> fieldMap = new LinkedHashMap<>();
        for (CdfDefinition d : definitions) {
            for (CdfField field : d.getFields()) {
                String fieldName = field.getName();
                if (fieldMap.containsKey(fieldName)) {
                    // Merge annotations; type change is not allowed – fail the build
                    CdfField base = fieldMap.get(fieldName);
                    if (!base.getType().equals(field.getType())) {
                        throw new IllegalStateException(
                                "CDF merge error for entity '" + entityName + "': field '"
                                        + fieldName + "' type change from '" + base.getType()
                                        + "' to '" + field.getType() + "' is not allowed. "
                                        + "Only adding annotations to an existing field is permitted.");
                    } else {
                        // Add new annotations from extension
                        Set<String> mergedAnnotations = new LinkedHashSet<>(base.getAnnotations());
                        mergedAnnotations.addAll(field.getAnnotations());
                        base.setAnnotations(new ArrayList<>(mergedAnnotations));
                    }
                } else {
                    fieldMap.put(fieldName, field);
                }
            }
        }
        merged.setFields(new ArrayList<>(fieldMap.values()));

        // Merge methods: extension replaces base method of the same name
        Map<String, CdfMethod> methodMap = new LinkedHashMap<>();
        for (CdfDefinition d : definitions) {
            for (CdfMethod method : d.getMethods()) {
                methodMap.put(method.getName(), method);
            }
        }
        merged.setMethods(new ArrayList<>(methodMap.values()));

        // Constructors: collect all unique signatures
        merged.setConstructors(mergeConstructors(definitions));

        return merged;
    }

    private List<CdfConstructor> mergeConstructors(List<CdfDefinition> definitions) {
        // Deduplicate constructors by parameter-type signature
        Map<String, CdfConstructor> constructorMap = new LinkedHashMap<>();
        for (CdfDefinition d : definitions) {
            for (CdfConstructor ctor : d.getConstructors()) {
                String sig = constructorSignature(ctor);
                constructorMap.putIfAbsent(sig, ctor);
            }
        }
        return new ArrayList<>(constructorMap.values());
    }

    private String constructorSignature(CdfConstructor ctor) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < ctor.getParameters().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ctor.getParameters().get(i).getType());
        }
        sb.append(")");
        return sb.toString();
    }
}
