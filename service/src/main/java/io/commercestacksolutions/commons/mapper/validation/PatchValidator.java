package io.commercestacksolutions.commons.mapper.validation;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic JSON Patch validator that applies a list of validation rules.
 * Implementation of PatchValidatorService interface.
 * Following the Open-Closed Principle, new validation rules can be added
 * without modifying this class.
 */
public class PatchValidator implements PatchValidatorService {
    
    private final List<PatchValidationRule> rules;
    
    public PatchValidator(List<PatchValidationRule> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }
    
    /**
     * Validates the JSON Patch against all registered rules.
     *
     * @param patch the JSON Patch to validate
     * @param entityId the ID of the entity being patched (for context)
     * @return a list of all error messages from failed validations
     */
    @Override
    public List<Message> validate(JsonNode patch, Object entityId) {
        List<Message> errors = new ArrayList<>();
        
        for (PatchValidationRule rule : rules) {
            List<Message> ruleErrors = rule.validate(patch, entityId);
            if (ruleErrors != null && !ruleErrors.isEmpty()) {
                errors.addAll(ruleErrors);
            }
        }
        
        return errors;
    }
    
    /**
     * Checks if the patch is valid (no validation errors).
     *
     * @param patch the JSON Patch to validate
     * @param entityId the ID of the entity being patched (for context)
     * @return true if valid, false if there are any validation errors
     */
    @Override
    public boolean isValid(JsonNode patch, Object entityId) {
        return validate(patch, entityId).isEmpty();
    }
}
