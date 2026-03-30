package de.ebusyness.commons.service.entity.validation;

import de.ebusyness.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic entity validator that applies a list of validation rules.
 * Implementation of Validator interface.
 * Following the Open-Closed Principle, new validation rules can be added
 * without modifying this class.
 *
 * @param <T> the type of entity to validate
 */
public class EntityValidator<T> implements Validator<T> {
    
    private final List<ValidationRule<T>> rules;
    
    public EntityValidator(List<ValidationRule<T>> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }
    
    /**
     * Validates the entity against all registered rules.
     *
     * @param entity the entity to validate
     * @return a list of all error messages from failed validations
     */
    @Override
    public List<Message> validate(T entity) {
        List<Message> errors = new ArrayList<>();
        
        for (ValidationRule<T> rule : rules) {
            List<Message> ruleErrors = rule.validate(entity);
            if (ruleErrors != null && !ruleErrors.isEmpty()) {
                errors.addAll(ruleErrors);
            }
        }
        
        return errors;
    }
    
    /**
     * Checks if the entity is valid (no validation errors).
     *
     * @param entity the entity to validate
     * @return true if valid, false if there are any validation errors
     */
    @Override
    public boolean isValid(T entity) {
        return validate(entity).isEmpty();
    }
}
