package io.commercestacksolutions.commons.mapper.validation.rules;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.mapper.validation.PatchValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validation rule that prevents removal of mandatory fields.
 * These are fields that must always have a value and cannot be removed.
 */
public class MandatoryFieldsRule implements PatchValidationRule {
    
    private final Set<String> mandatoryFields;
    
    /**
     * Creates a new mandatory fields validation rule.
     *
     * @param mandatoryFields set of field names that cannot be removed
     */
    public MandatoryFieldsRule(Set<String> mandatoryFields) {
        this.mandatoryFields = mandatoryFields;
    }
    
    @Override
    public List<Message> validate(JsonNode patch, Object entityId) {
        List<Message> errors = new ArrayList<>();
        
        if (patch == null || !patch.isArray()) {
            return errors;
        }
        
        for (JsonNode operation : patch) {
            String op = operation.has("op") ? operation.get("op").asText() : null;
            String path = operation.has("path") ? operation.get("path").asText() : null;
            
            if (path != null && "remove".equals(op)) {
                // Extract field name from path (e.g., "/fieldName" -> "fieldName")
                String fieldName = path.startsWith("/") ? path.substring(1) : path;
                
                if (mandatoryFields.contains(fieldName)) {
                    errors.add(new Message(
                        Message.MessageType.ERROR,
                        "Field '" + fieldName + "' is mandatory and cannot be removed",
                        List.of(fieldName)
                    ));
                }
            }
        }
        
        return errors;
    }
}
