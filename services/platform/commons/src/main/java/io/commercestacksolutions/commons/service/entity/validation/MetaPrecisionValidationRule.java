package io.commercestacksolutions.commons.service.entity.validation;

import io.commercestacksolutions.commons.dataaccess.meta.MetaPrecision;
import io.commercestacksolutions.commons.web.rest.Message;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validation rule that ensures numeric fields annotated with {@link MetaPrecision}
 * do not exceed their declared decimal precision.
 */
@Component
public class MetaPrecisionValidationRule implements ValidationRule<Object> {

    @Override
    public List<Message> validate(Object entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        List<Message> errors = new ArrayList<>();
        Class<?> clazz = entity.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(MetaPrecision.class)) {
                    MetaPrecision annotation = field.getAnnotation(MetaPrecision.class);
                    int maxPrecision = annotation.value();

                    try {
                        field.setAccessible(true);
                        Object value = field.get(entity);

                        if (value instanceof BigDecimal bigDecimal) {
                            BigDecimal stripped = bigDecimal.stripTrailingZeros();
                            if (stripped.scale() > maxPrecision) {
                                errors.add(new Message(
                                        Message.MessageType.ERROR,
                                        String.format("Field '%s' exceeds allowed precision of %d decimal places", field.getName(), maxPrecision),
                                        List.of(field.getName())
                                ));
                            }
                        }
                    } catch (Exception e) {
                        // Ignore reflection access errors
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        return errors;
    }
}
