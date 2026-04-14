package io.commercestacksolutions.commons.service.entity.validation.rules;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.commons.web.rest.MetaInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generic service-layer validation rule that checks all mandatory fields declared
 * via {@code @MandatoryField} (and auto-mandatory {@code @Id} fields) are
 * populated before an entity is persisted.
 *
 * <p>The list of mandatory fields is read at runtime from the
 * {@link EntityMetaInfoRegistry}, which is pre-built at application startup.
 * This keeps the validation logic in sync with the annotation-based metadata
 * without any hardcoding.</p>
 *
 * <h3>Null / blank detection</h3>
 * <ul>
 *   <li>{@code String} fields — rejected when {@code null} or blank</li>
 *   <li>All other types — rejected when {@code null}</li>
 * </ul>
 *
 * <h3>Spring wiring</h3>
 * <p>This class is <em>not</em> a Spring component itself.  One typed instance per
 * entity must be declared as a {@code @Bean} (e.g. in
 * {@code RequireMandatoryFieldsValidationConfig}) so that Spring can auto-wire it
 * into the corresponding {@code *ServiceImpl} via
 * {@code List<ValidationRule<T>> validationRules}.</p>
 *
 * @param <T> the entity type to validate
 */
public class RequireMandatoryFieldsRule<T> implements ValidationRule<T> {

    private static final String MANDATORY_FIELD_MESSAGE_KEY = "common.errors.validation.mandatoryField";

    private final Class<T> entityClass;
    private final EntityMetaInfoRegistry registry;

    public RequireMandatoryFieldsRule(Class<T> entityClass, EntityMetaInfoRegistry registry) {
        this.entityClass = entityClass;
        this.registry = registry;
    }

    @Override
    public List<Message> validate(T entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        MetaInfo metaInfo = registry.getMetaInfo(entityClass);
        if (metaInfo == null || metaInfo.getMandatoryFields() == null) {
            return Collections.emptyList();
        }

        List<Message> errors = new ArrayList<>();
        for (String fieldName : metaInfo.getMandatoryFields()) {
            Object value = getFieldValue(entity, fieldName);
            if (isNullOrBlank(value)) {
                errors.add(new Message(
                        Message.MessageType.ERROR,
                        MANDATORY_FIELD_MESSAGE_KEY,
                        Map.of("field", fieldName),
                        List.of(fieldName)
                ));
            }
        }
        return errors;
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private Object getFieldValue(T entity, String fieldName) {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(entity);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isNullOrBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isBlank();
        }
        return false;
    }
}
