package de.ebusyness.commons.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GenericPatchMapper to verify proper exception handling,
 * especially for JsonPatchApplicationException which should use ERROR_APPLYING_PATCH.
 */
public class GenericPatchMapperTest {

    private ObjectMapper objectMapper;
    private GenericPatchMapper<TestEntity> patchMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        patchMapper = new GenericPatchMapper<>(objectMapper, TestEntity.class);
    }

    @Test
    public void testApplyPatch_ValidPatch_Success() throws Exception {
        // Given
        TestEntity entity = new TestEntity("test-id", "Original Name", 100);
        String patchJson = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Updated Name\"}]";
        JsonNode patch = objectMapper.readTree(patchJson);

        // When
        TestEntity result = patchMapper.applyPatch(patch, entity);

        // Then
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("Updated Name", result.getName());
        assertEquals(100, result.getValue());
    }

    @Test
    public void testApplyPatch_InvalidPath_ThrowsErrorApplyingPatch() throws Exception {
        // Given
        TestEntity entity = new TestEntity("test-id", "Original Name", 100);
        // Invalid path that doesn't exist on the entity
        String patchJson = "[{\"op\":\"replace\",\"path\":\"/nonExistentField\",\"value\":\"test\"}]";
        JsonNode patch = objectMapper.readTree(patchJson);

        // When & Then
        DataMappingException exception = assertThrows(DataMappingException.class, () -> {
            patchMapper.applyPatch(patch, entity);
        });

        // Verify that ERROR_APPLYING_PATCH is used for JsonPatchApplicationException
        assertEquals(MessageKeys.ERROR_APPLYING_PATCH, exception.getMessage());
        assertNotNull(exception.getErrorResponse());
        assertNotNull(exception.getMessages());
        assertFalse(exception.getMessages().isEmpty());
    }

    @Test
    public void testApplyPatch_InvalidOperation_ThrowsErrorApplyingPatch() throws Exception {
        // Given
        TestEntity entity = new TestEntity("test-id", "Original Name", 100);
        // Invalid operation
        String patchJson = "[{\"op\":\"invalid_op\",\"path\":\"/name\",\"value\":\"test\"}]";
        JsonNode patch = objectMapper.readTree(patchJson);

        // When & Then
        DataMappingException exception = assertThrows(DataMappingException.class, () -> {
            patchMapper.applyPatch(patch, entity);
        });

        // Verify that ERROR_APPLYING_PATCH is used
        assertEquals(MessageKeys.ERROR_APPLYING_PATCH, exception.getMessage());
    }

    @Test
    public void testApplyPatch_InvalidValueType_ThrowsErrorMappingPatchOperation() throws Exception {
        // Given
        TestEntity entity = new TestEntity("test-id", "Original Name", 100);
        // Invalid value type - trying to set an integer field to a string
        String patchJson = "[{\"op\":\"replace\",\"path\":\"/value\",\"value\":\"not_a_number\"}]";
        JsonNode patch = objectMapper.readTree(patchJson);

        // When & Then
        DataMappingException exception = assertThrows(DataMappingException.class, () -> {
            patchMapper.applyPatch(patch, entity);
        });

        // This should be ERROR_MAPPING_PATCH_OPERATION because it's a JsonMappingException
        assertEquals(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, exception.getMessage());
        assertNotNull(exception.getErrorResponse());
        assertNotNull(exception.getMessages());
        assertFalse(exception.getMessages().isEmpty());
    }

    @Test
    public void testApplyPatch_MultipleOperations_Success() throws Exception {
        // Given
        TestEntity entity = new TestEntity("test-id", "Original Name", 100);
        String patchJson = "[" +
                "{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"New Name\"}," +
                "{\"op\":\"replace\",\"path\":\"/value\",\"value\":200}" +
                "]";
        JsonNode patch = objectMapper.readTree(patchJson);

        // When
        TestEntity result = patchMapper.applyPatch(patch, entity);

        // Then
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("New Name", result.getName());
        assertEquals(200, result.getValue());
    }

    /**
     * Test entity class for patch mapper testing
     */
    public static class TestEntity {
        private String id;
        private String name;
        private int value;

        public TestEntity() {
        }

        public TestEntity(String id, String name, int value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
