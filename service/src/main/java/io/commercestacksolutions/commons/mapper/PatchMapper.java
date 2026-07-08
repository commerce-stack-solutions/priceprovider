package io.commercestacksolutions.commons.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import com.flipkart.zjsonpatch.JsonPatchApplicationException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface-based mapper for applying JSON Patch operations to entities.
 * Converted from the previous abstract base class to an interface with
 * default implementations. Implementations are responsible for providing
 * an ObjectMapper (typically via a getter that returns an @Autowired
 * ObjectMapper field) or overriding the methods as needed.
 *
 * @param <T> The target entity type to apply patches to
 */
public interface PatchMapper<T> {

    /**
     * Implementations must provide an ObjectMapper instance (for example
     * by returning an @Autowired field).
     */
    ObjectMapper getObjectMapper();

    /**
     * Optionally provide a targetClass to allow the default createTarget()
     * implementation to instantiate a new instance. If null, implementations
     * must override createTarget().
     */
    default Class<T> getTargetClass() {
        return null;
    }

    /**
     * Default createTarget implementation. Tries to instantiate the class
     * returned by getTargetClass(). If that is null, throws a
     * DataMappingException and requires the implementor to override this
     * method.
     */
    default T createTarget() throws DataMappingException {
        Class<T> targetClass = getTargetClass();
        if (targetClass == null) {
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_NO_TARGET_CLASS);
        }
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Map<String, String> params = new HashMap<>();
            params.put("className", targetClass.getName());
            params.put("details", e.getMessage());
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_INSTANTIATION, params, e);
        }
    }

    /**
     * Applies a JSON Patch to the given dataItemToPatch and returns a new
     * target instance with the patched data. Uses the provided
     * ObjectMapper from getObjectMapper().
     */
    default T applyPatch(JsonNode patch, T dataItemToPatch) throws DataMappingException {
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
        } catch (Exception e) {
            Map<String, String> params = new HashMap<>();
            params.put("details", e.getMessage());
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, params, e);
        }
    }
}
