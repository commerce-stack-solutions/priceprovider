package de.ebusyness.commons.mapper.validation;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.web.rest.Message;
import java.util.List;

/**
 * Validator interface for JSON Patch validation operations.
 * This interface defines the contract for patch validation,
 * following Interface Driven Design (IDD) principles.
 */
public interface PatchValidatorService {
    
    /**
     * Validates the JSON Patch against all registered rules.
     *
     * @param patch the JSON Patch to validate
     * @param entityId the ID of the entity being patched (for context)
     * @return a list of all error messages from failed validations
     */
    List<Message> validate(JsonNode patch, Object entityId);
    
    /**
     * Checks if the patch is valid (no validation errors).
     *
     * @param patch the JSON Patch to validate
     * @param entityId the ID of the entity being patched (for context)
     * @return true if valid, false if there are any validation errors
     */
    boolean isValid(JsonNode patch, Object entityId);
}
