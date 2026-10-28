package io.commercestacksolutions.commons.mapper.validation;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.web.rest.Message;

import java.util.List;

/**
 * Interface for JSON Patch validation rules.
 * Following the Open-Closed Principle, new validation rules can be added
 * without modifying existing code.
 */
public interface PatchValidationRule {
    
    /**
     * Validates the given JSON Patch operations and returns any validation error messages.
     *
     * @param patch the JSON Patch to validate
     * @param entityId the ID of the entity being patched (for context)
     * @return a list of error messages if validation fails, empty list if validation passes
     */
    List<Message> validate(JsonNode patch, Object entityId);
    
    /**
     * Returns the name of this validation rule for logging/debugging purposes.
     *
     * @return the rule name
     */
    default String getRuleName() {
        return this.getClass().getSimpleName();
    }
}
