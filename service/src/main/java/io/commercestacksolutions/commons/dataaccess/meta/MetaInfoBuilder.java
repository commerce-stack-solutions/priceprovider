package io.commercestacksolutions.commons.dataaccess.meta;

import io.commercestacksolutions.commons.dataaccess.ReferenceKey;
import io.commercestacksolutions.commons.dataaccess.idgenerator.GeneratedId;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.lang.reflect.Field;
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
 * </ul>
 *
 * <h3>Auto-mandatory rule for @Id fields</h3>
 * <p>A field annotated with {@code @Id} is implicitly mandatory (i.e. the caller must supply it)
 * unless it is also annotated with {@code @GeneratedValue}, which signals that the persistence
 * layer assigns the value automatically.  Adding {@code @MandatoryField} to an {@code @Id}
 * field is therefore redundant and should be avoided.</p>
 *
 * <h3>Similar annotations to consider for future entities</h3>
 * <ul>
 *   <li>{@code @Column(nullable = false)} — DB-level NOT NULL constraint; if a field carries this
 *       and is not auto-generated, it may be worth marking it {@code @MandatoryField} as
 *       well so the API consumer knows it is required.</li>
 *   <li>{@code @NotNull} (Bean Validation) — application-level non-null constraint; fields with
 *       this annotation are semantically mandatory from an API perspective and are also candidates
 *       for {@code @MandatoryField}.</li>
 *   <li>{@code @EmbeddedId} / {@code @IdClass} — composite-key patterns; if used, the builder
 *       may need extending to inspect embedded fields for identity/mandatory detection.</li>
 * </ul>
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

        Class<?> clazz = entityClass;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
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
            }
            clazz = clazz.getSuperclass();
        }

        // Fall back to identity fields when no @ReferenceKey is declared
        if (referenceKeyFields.isEmpty()) {
            referenceKeyFields.addAll(identityFields);
        }

        MetaInfo meta = new MetaInfo(identityFields, mandatoryFields, enumValues.isEmpty() ? null : enumValues);
        meta.setReferenceKeyFields(referenceKeyFields.isEmpty() ? null : referenceKeyFields);
        return meta;
    }
}

