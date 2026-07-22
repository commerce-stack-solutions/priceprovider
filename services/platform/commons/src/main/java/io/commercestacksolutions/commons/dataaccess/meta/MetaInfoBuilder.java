package io.commercestacksolutions.commons.dataaccess.meta;

import io.commercestacksolutions.commons.dataaccess.ReferenceKey;
import io.commercestacksolutions.commons.dataaccess.idgenerator.GeneratedId;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that builds MetaInfo by inspecting entity class annotations.
 * Traverses the full class hierarchy to detect:
 * <ul>
 *   <li>Identity fields — fields annotated with {@code jakarta.persistence.@Id}</li>
 *   <li>Mandatory fields — fields annotated with {@link MandatoryField}, <strong>plus</strong>
 *       all {@code @Id} fields that do <em>not</em> also carry {@code @GeneratedValue}
 *       (generated IDs are assigned by the database and must not be supplied by the caller)</li>
 *   <li>Reference key fields — fields annotated with
 *       {@link io.commercestacksolutions.commons.dataaccess.ReferenceKey @ReferenceKey};
 *       if none are found the identity fields are used as fallback</li>
 *   <li>Enum values — ALL enum-typed fields (mandatory or optional) are always included</li>
 *   <li>Field Metadata — Detailed field names, types, readonly, precision and referenced entities properties</li>
 * </ul>
 *
 * <h3>Auto-mandatory rule for @Id fields</h3>
 * <p>A field annotated with {@code @Id} is implicitly mandatory (i.e. the caller must supply it)
 * unless it is also annotated with {@code @GeneratedValue}, which signals that the persistence
 * layer assigns the value automatically.  Adding {@code @MandatoryField} to an {@code @Id}
 * field is therefore redundant and should be avoided.</p>
 */
public class MetaInfoBuilder {

    private MetaInfoBuilder() {
        // Utility class
    }

    /**
     * Builds a MetaInfo instance from the annotations on the given entity class.
     * Traverses the full class hierarchy (including superclasses).
     *
     * @param entityClass the entity class to inspect
     * @return MetaInfo populated from annotations
     */
    public static MetaInfo build(Class<?> entityClass) {
        List<String> identityFields = new ArrayList<>();
        List<String> mandatoryFields = new ArrayList<>();
        List<String> referenceKeyFields = new ArrayList<>();
        Map<String, List<String>> enumValues = new HashMap<>();
        List<MetaInfo.FieldMetadata> fieldsMetadata = new ArrayList<>();

        Class<?> clazz = entityClass;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                // @Id → identity field; also auto-mandatory unless the DB generates the value
                if (field.isAnnotationPresent(Id.class)) {
                    identityFields.add(field.getName());
                    if (!field.isAnnotationPresent(GeneratedValue.class)
                            && !field.isAnnotationPresent(GeneratedId.class)
                            && !mandatoryFields.contains(field.getName())) {
                        mandatoryFields.add(field.getName());
                    }
                }
                // @MandatoryField → explicitly mandatory (use for non-@Id fields)
                if (field.isAnnotationPresent(MandatoryField.class)
                        && !mandatoryFields.contains(field.getName())) {
                    mandatoryFields.add(field.getName());
                }
                // @ReferenceKey → human-readable alternative key used in JSON refs and queries
                if (field.isAnnotationPresent(ReferenceKey.class)
                        && !referenceKeyFields.contains(field.getName())) {
                    referenceKeyFields.add(field.getName());
                }
                // Always include enum values for any enum-typed field (mandatory or optional)
                if (field.getType().isEnum()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) field.getType();
                    List<String> values = Arrays.stream(enumClass.getEnumConstants())
                            .map(Enum::name)
                            .collect(Collectors.toList());
                    enumValues.put(field.getName(), values);
                }

                // Build FieldMetadata
                boolean isReadOnly = "createdAt".equals(field.getName())
                        || "lastModifiedAt".equals(field.getName())
                        || (field.isAnnotationPresent(Id.class) && (field.isAnnotationPresent(GeneratedValue.class) || field.isAnnotationPresent(GeneratedId.class)));

                Integer precision = null;
                if (field.isAnnotationPresent(MetaPrecision.class)) {
                    precision = field.getAnnotation(MetaPrecision.class).value();
                } else if (field.isAnnotationPresent(jakarta.persistence.Column.class)) {
                    jakarta.persistence.Column column = field.getAnnotation(jakarta.persistence.Column.class);
                    if (column.scale() > 0) {
                        precision = column.scale();
                    }
                }

                String determinedType = determineFieldType(field);

                // Discover target referenced entity
                String referencedEntity = null;
                Class<?> targetClass = null;
                if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                    java.lang.reflect.Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType paramType) {
                        java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> argClass) {
                            targetClass = argClass;
                        }
                    }
                } else {
                    targetClass = field.getType();
                }

                if (targetClass != null && isEntityClass(targetClass)) {
                    String name = targetClass.getSimpleName();
                    if (name.endsWith("Entity")) {
                        name = name.substring(0, name.length() - "Entity".length());
                    }
                    referencedEntity = name;
                }

                MetaInfo.FieldMetadata fieldMeta = new MetaInfo.FieldMetadata(
                        field.getName(),
                        determinedType,
                        isReadOnly,
                        precision,
                        referencedEntity
                );

                // Prevent duplicates if overridden in subclass hierarchy (subclass overrides superclass)
                boolean exists = fieldsMetadata.stream().anyMatch(f -> f.getName().equals(field.getName()));
                if (!exists) {
                    fieldsMetadata.add(fieldMeta);
                }
            }
            clazz = clazz.getSuperclass();
        }

        // Fall back to identity fields when no @ReferenceKey is declared
        if (referenceKeyFields.isEmpty()) {
            referenceKeyFields.addAll(identityFields);
        }

        MetaInfo meta = new MetaInfo(identityFields, mandatoryFields, enumValues.isEmpty() ? null : enumValues);
        meta.setReferenceKeyFields(referenceKeyFields.isEmpty() ? null : referenceKeyFields);
        meta.setFields(fieldsMetadata.isEmpty() ? null : fieldsMetadata);
        return meta;
    }

    private static String determineFieldType(Field field) {
        Class<?> type = field.getType();

        // 1. LocalizedString
        if (Map.class.isAssignableFrom(type)) {
            return "LocalizedString";
        }

        // 2. Set<Reference>
        if (java.util.Collection.class.isAssignableFrom(type)) {
            java.lang.reflect.Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> argClass) {
                    if (isEntityClass(argClass)) {
                        return "Set<Reference>";
                    }
                }
            }
            // Fallback check on name ending with "Refs"
            if (field.getName().endsWith("Refs") || field.getName().endsWith("Entities")) {
                return "Set<Reference>";
            }
        }

        // 3. Reference
        if (isEntityClass(type)
                || field.getName().endsWith("Ref")
                || field.isAnnotationPresent(jakarta.persistence.ManyToOne.class)
                || field.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
            return "Reference";
        }

        // 4. Enum
        if (type.isEnum() || field.isAnnotationPresent(MetaDynamicEnum.class)) {
            return "Enum";
        }

        // 5. DateTime
        if (type.equals(java.time.OffsetDateTime.class)
                || type.equals(java.time.LocalDateTime.class)
                || type.equals(java.time.LocalDate.class)
                || type.equals(java.time.Instant.class)
                || type.equals(java.util.Date.class)
                || type.equals(java.time.ZonedDateTime.class)) {
            return "DateTime";
        }

        // 6. Number
        if (Number.class.isAssignableFrom(type)
                || type.equals(int.class)
                || type.equals(long.class)
                || type.equals(double.class)
                || type.equals(float.class)
                || type.equals(short.class)) {
            return "Number";
        }

        // 7. Boolean
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "Boolean";
        }

        // 8. String / fallback
        return "String";
    }

    private static boolean isEntityClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        if (clazz.isAnnotationPresent(jakarta.persistence.Entity.class)) {
            return true;
        }
        String name = clazz.getSimpleName();
        return name.endsWith("Entity");
    }
}
