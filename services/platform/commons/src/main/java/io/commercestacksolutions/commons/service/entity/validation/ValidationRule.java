package io.commercestacksolutions.commons.service.entity.validation;

import io.commercestacksolutions.commons.web.rest.Message;

import java.util.List;

/**
 * Interface for validation rules that can be applied to entities.
 * Following the Open-Closed Principle, new validation rules can be added
 * without modifying existing code.
 *
 * @param <T> the type of entity to validate
 */
public interface ValidationRule<T> {
    
    /**
     * Validates the given entity and returns any validation error messages.
     *
     * @param entity the entity to validate
     * @return a list of error messages if validation fails, empty list if validation passes
     */
    List<Message> validate(T entity);
    
    /**
     * Returns the name of this validation rule for logging/debugging purposes.
     *
     * @return the rule name
     */
    default String getRuleName() {
        return this.getClass().getSimpleName();
    }
}
