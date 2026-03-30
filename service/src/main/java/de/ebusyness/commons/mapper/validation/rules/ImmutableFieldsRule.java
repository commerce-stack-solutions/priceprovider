package de.ebusyness.commons.mapper.validation.rules;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.mapper.validation.PatchValidationRule;
import de.ebusyness.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validation rule that prevents modification or removal of immutable fields.
 * These are typically identifier fields that should not change after entity creation.
 */
public class ImmutableFieldsRule implements PatchValidationRule {
    
    private final Set<String> immutableFields;
    
    /**
     * Creates a new immutable fields validation rule.
     *
     * @param immutableFields set of field names that cannot be modified or removed
     */
    public ImmutableFieldsRule(Set<String> immutableFields) {
        this.immutableFields = immutableFields;
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
            
            if (path != null) {
                // Extract field name from path (e.g., "/fieldName" -> "fieldName")
                String fieldName = path.startsWith("/") ? path.substring(1) : path;
                
                if (immutableFields.contains(fieldName)) {
                    if ("remove".equals(op)) {
                        errors.add(new Message(
                            Message.MessageType.ERROR,
                            "Field '" + fieldName + "' cannot be removed",
                            List.of(fieldName)
                        ));
                    } else if ("replace".equals(op) || "add".equals(op)) {
                        errors.add(new Message(
                            Message.MessageType.ERROR,
                            "Field '" + fieldName + "' cannot be changed",
                            List.of(fieldName)
                        ));
                    }
                }
            }
        }
        
        return errors;
    }
}
