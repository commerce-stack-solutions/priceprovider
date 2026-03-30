package de.ebusyness.commons.mapper.validation.rules;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.mapper.validation.PatchValidationRule;
import de.ebusyness.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Validation rule that ensures localized fields (Map<String, String>) contain 
 * values for all mandatory languages.
 * This ensures that fields like 'name' have values in required languages.
 * 
 * Mandatory languages are loaded dynamically from the database each time validation occurs,
 * ensuring that changes to mandatory language settings are immediately reflected in validation.
 */
public class LocalizedFieldValidationRule implements PatchValidationRule {
    
    private final Set<String> localizedFields;
    private final Supplier<Set<String>> mandatoryLanguagesSupplier;
    
    /**
     * Creates a new localized field validation rule with a supplier that dynamically retrieves mandatory languages.
     * 
     * The supplier is invoked each time validation occurs, ensuring that the latest mandatory languages
     * from the database are used for validation. This allows changes to mandatory language settings
     * to take effect immediately without requiring application restart.
     *
     * @param localizedFields set of field names that are localized (e.g., "name")
     * @param mandatoryLanguagesSupplier supplier that provides the set of mandatory language codes from the database
     */
    public LocalizedFieldValidationRule(Set<String> localizedFields, Supplier<Set<String>> mandatoryLanguagesSupplier) {
        this.localizedFields = localizedFields;
        this.mandatoryLanguagesSupplier = mandatoryLanguagesSupplier;
    }
    
    @Override
    public List<Message> validate(JsonNode patch, Object entityId) {
        List<Message> errors = new ArrayList<>();
        
        if (patch == null || !patch.isArray()) {
            return errors;
        }
        
        // Retrieve mandatory languages dynamically from database on each validation
        // This ensures changes to mandatory languages are reflected immediately
        Set<String> mandatoryLanguages = mandatoryLanguagesSupplier.get();
        
        for (JsonNode operation : patch) {
            String op = operation.has("op") ? operation.get("op").asText() : null;
            String path = operation.has("path") ? operation.get("path").asText() : null;
            JsonNode value = operation.has("value") ? operation.get("value") : null;
            
            if (path != null && ("replace".equals(op) || "add".equals(op))) {
                // Extract field name from path
                String fieldName = path.startsWith("/") ? path.substring(1).split("/")[0] : path.split("/")[0];
                
                if (localizedFields.contains(fieldName)) {
                    // Check if this is a modification to the entire localized field
                    if (path.equals("/" + fieldName) && value != null && value.isObject()) {
                        // Validate that all mandatory languages are present
                        for (String language : mandatoryLanguages) {
                            if (!value.has(language) || value.get(language).isNull() || 
                                (value.get(language).isTextual() && value.get(language).asText().trim().isEmpty())) {
                                errors.add(new Message(
                                    Message.MessageType.ERROR,
                                    "Field '" + fieldName + "' must contain a value for language '" + language + "'",
                                    List.of(fieldName)
                                ));
                            }
                        }
                    }
                    // Check if trying to remove a specific language from a localized field
                    else if (path.matches("/" + fieldName + "/[^/]+")) {
                        String language = path.substring(path.lastIndexOf('/') + 1);
                        if (mandatoryLanguages.contains(language)) {
                            if ("remove".equals(op) || (value != null && value.isNull()) || 
                                (value != null && value.isTextual() && value.asText().trim().isEmpty())) {
                                errors.add(new Message(
                                    Message.MessageType.ERROR,
                                    "Field '" + fieldName + "' must contain a value for language '" + language + "'",
                                    List.of(fieldName)
                                ));
                            }
                        }
                    }
                }
            }
            // Check if trying to remove a mandatory language value
            else if (path != null && "remove".equals(op)) {
                String fieldName = path.startsWith("/") ? path.substring(1).split("/")[0] : path.split("/")[0];
                
                if (localizedFields.contains(fieldName) && path.matches("/" + fieldName + "/[^/]+")) {
                    String language = path.substring(path.lastIndexOf('/') + 1);
                    if (mandatoryLanguages.contains(language)) {
                        errors.add(new Message(
                            Message.MessageType.ERROR,
                            "Field '" + fieldName + "' must contain a value for language '" + language + "'",
                            List.of(fieldName)
                        ));
                    }
                }
            }
        }
        
        return errors;
    }
}
