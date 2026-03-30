package de.ebusyness.commons.mapper;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import com.flipkart.zjsonpatch.JsonPatchApplicationException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * Reusable generic implementation of PatchMapper.
 * It is constructed with an ObjectMapper and a target Class<T> and
 * provides enhanced error handling for JSON deserialization errors.
 */
public class GenericPatchMapper<T> implements PatchMapper<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> targetClass;

    public GenericPatchMapper(ObjectMapper objectMapper, Class<T> targetClass) {
        this.objectMapper = objectMapper;
        this.targetClass = targetClass;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public Class<T> getTargetClass() {
        return targetClass;
    }

    @Override
    public T applyPatch(JsonNode patch, T dataItemToPatch) throws DataMappingException {
        try {
            // Convert the dataToPatch entity to JSON
            JsonNode targetJson = getObjectMapper().valueToTree(dataItemToPatch);

            // Apply the patch
            JsonNode patchedJson = JsonPatch.apply(patch, targetJson);

            // Update the dataToPatch entity with the patched values
            T target = createTarget();
            getObjectMapper().readerForUpdating(target).readValue(patchedJson);
            return target;
        } catch (JsonPatchApplicationException e) {
            // Handle JSON Patch application errors (e.g., invalid path, invalid operation)
            Map<String, String> params = new HashMap<>();
            params.put("details", e.getMessage());
            throw new DataMappingException(MessageKeys.ERROR_APPLYING_PATCH, params, e);
        } catch (JsonMappingException e) {
            // Extract field name and create user-friendly error message
            String fieldName = extractFieldNameFromJsonException(e);
            String userMessage = createUserFriendlyMessage(e, fieldName);
            
            // Create DataMappingException with enhanced message
            Map<String, String> params = new HashMap<>();
            params.put("details", userMessage);
            if (fieldName != null) {
                params.put("field", fieldName);
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, params, e);
            }
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, params, e);
        } catch (Exception e) {
            Map<String, String> params = new HashMap<>();
            params.put("details", e.getMessage());
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, params, e);
        }
    }
    
    /**
     * Extracts the field name from a Jackson JsonMappingException
     */
    private String extractFieldNameFromJsonException(JsonMappingException e) {
        if (e.getPath() != null && !e.getPath().isEmpty()) {
            JsonMappingException.Reference ref = e.getPath().get(e.getPath().size() - 1);
            return ref.getFieldName();
        }
        return null;
    }
    
    /**
     * Creates a user-friendly error message from a Jackson exception
     */
    private String createUserFriendlyMessage(JsonMappingException e, String fieldName) {
        String message = e.getMessage();
        
        // Handle boolean deserialization errors
        if (message != null && message.contains("Cannot deserialize value of type `boolean`")) {
            if (fieldName != null) {
                return "Invalid value for field '" + fieldName + "': expected true or false";
            }
            return "Invalid boolean value: expected true or false";
        }
        
        // Handle numeric deserialization errors
        if (message != null && (message.contains("Cannot deserialize value of type `java.math.BigDecimal`") 
                || message.contains("Cannot deserialize value of type `int`")
                || message.contains("Cannot deserialize value of type `long`"))) {
            if (fieldName != null) {
                return "Invalid value for field '" + fieldName + "': expected a number";
            }
            return "Invalid numeric value: expected a number";
        }
        
        // Generic fallback
        if (fieldName != null) {
            return "Invalid value for field '" + fieldName + "'";
        }
        return "Invalid value in patch operation";
    }
}
