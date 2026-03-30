package de.ebusyness.commons.service.entity.validation;

import de.ebusyness.commons.web.rest.Message;
import java.util.List;

/**
 * Validator interface for entity validation operations.
 * This interface defines the contract for entity validation,
 * following Interface Driven Design (IDD) principles.
 *
 * @param <T> the type of entity to validate
 */
public interface Validator<T> {
    
    /**
     * Validates the entity against all registered rules.
     *
     * @param entity the entity to validate
     * @return a list of all error messages from failed validations
     */
    List<Message> validate(T entity);
    
    /**
     * Checks if the entity is valid (no validation errors).
     *
     * @param entity the entity to validate
     * @return true if valid, false if there are any validation errors
     */
    boolean isValid(T entity);
}
