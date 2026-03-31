package io.commercestacksolutions.commons.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.facade.group.GroupFacade;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AuditableEntity timestamp tracking through the facade layer.
 * Tests that createdAt and lastModifiedAt are correctly set during create and update operations via facades.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class AuditableEntityFacadeTimestampTest {

    @Autowired
    private GroupFacade groupFacade;

    @Test
    public void testGroupFacade_CreateThenUpdate_ShouldPreserveCreatedAt() throws InterruptedException, DataMappingException, NotFoundException, EntityValidationException {
        // Create new group via facade
        String groupId = "TEST-GRP-TIMESTAMP-001";
        GroupRestEntity newGroup = new GroupRestEntity();
        newGroup.setId(groupId);
        newGroup.setName("Test Group for Timestamp");
        
        // Create the group using createOrRecreate (simulating PUT request)
        GroupRestEntity createdGroup = groupFacade.createOrRecreate(groupId, newGroup);
        
        // Get the group with $info expanded to see timestamps
        GroupRestEntity fetchedGroup = groupFacade.getGroup(groupId, Set.of("$info"));
        
        // Verify both timestamps are set
        assertNotNull(fetchedGroup.getInfo());
        assertNotNull(fetchedGroup.getInfo().getCreatedAt());
        assertNotNull(fetchedGroup.getInfo().getLastModifiedAt());
        
        OffsetDateTime originalCreatedAt = fetchedGroup.getInfo().getCreatedAt();
        OffsetDateTime originalLastModifiedAt = fetchedGroup.getInfo().getLastModifiedAt();
        
        // Both should be equal for new entity
        assertEquals(originalCreatedAt, originalLastModifiedAt, "For new entity, createdAt and lastModifiedAt should be identical");
        
        // Wait to ensure timestamp difference
        Thread.sleep(100);
        
        // Update the group via facade
        GroupRestEntity updateRequest = new GroupRestEntity();
        updateRequest.setId(groupId);
        updateRequest.setName("Updated Test Group");
        
        GroupRestEntity updatedGroup = groupFacade.createOrRecreate(groupId, updateRequest);
        
        // Get the updated group with $info expanded to see timestamps
        GroupRestEntity fetchedUpdatedGroup = groupFacade.getGroup(groupId, Set.of("$info"));
        
        // Verify timestamps after update
        assertNotNull(fetchedUpdatedGroup.getInfo());
        assertNotNull(fetchedUpdatedGroup.getInfo().getCreatedAt());
        assertNotNull(fetchedUpdatedGroup.getInfo().getLastModifiedAt());
        
        // createdAt should NOT have changed
        assertEquals(originalCreatedAt, fetchedUpdatedGroup.getInfo().getCreatedAt(), 
            "createdAt must not be changed when entity is updated");
        
        // lastModifiedAt should be updated
        assertTrue(fetchedUpdatedGroup.getInfo().getLastModifiedAt().isAfter(originalLastModifiedAt),
            "lastModifiedAt should be updated to a newer timestamp");
        
        // They should now be different
        assertNotEquals(fetchedUpdatedGroup.getInfo().getCreatedAt(), fetchedUpdatedGroup.getInfo().getLastModifiedAt(),
            "After update, createdAt and lastModifiedAt must be different");
    }

    @Test
    public void testGroupFacade_Patch_ShouldPreserveCreatedAt() throws InterruptedException, DataMappingException, NotFoundException, JsonProcessingException, EntityValidationException {
        // Create new group via facade
        String groupId = "TEST-GRP-TIMESTAMP-PATCH";
        GroupRestEntity newGroup = new GroupRestEntity();
        newGroup.setId(groupId);
        newGroup.setName("Test Group for Patch");
        
        // Create the group
        groupFacade.createOrRecreate(groupId, newGroup);
        
        // Get the group with $info expanded to see timestamps
        GroupRestEntity fetchedGroup = groupFacade.getGroup(groupId, Set.of("$info"));
        
        OffsetDateTime originalCreatedAt = fetchedGroup.getInfo().getCreatedAt();
        OffsetDateTime originalLastModifiedAt = fetchedGroup.getInfo().getLastModifiedAt();
        
        // Both should be equal for new entity
        assertEquals(originalCreatedAt, originalLastModifiedAt, "For new entity, createdAt and lastModifiedAt should be identical");
        
        // Wait to ensure timestamp difference
        Thread.sleep(100);
        
        // Patch the group (update only the name) - create a proper JSON Patch array
        ObjectMapper objectMapper = new ObjectMapper();
        String patchJson = "[{\"op\": \"replace\", \"path\": \"/name\", \"value\": \"Patched Test Group\"}]";
        
        GroupRestEntity patchedGroup = groupFacade.patch(groupId, objectMapper.readTree(patchJson));
        
        // Get the patched group with $info expanded to see timestamps
        GroupRestEntity fetchedPatchedGroup = groupFacade.getGroup(groupId, Set.of("$info"));
        
        // Verify timestamps after patch
        assertNotNull(fetchedPatchedGroup.getInfo());
        assertNotNull(fetchedPatchedGroup.getInfo().getCreatedAt());
        assertNotNull(fetchedPatchedGroup.getInfo().getLastModifiedAt());
        
        // createdAt should NOT have changed
        assertEquals(originalCreatedAt, fetchedPatchedGroup.getInfo().getCreatedAt(), 
            "createdAt must not be changed when entity is patched");
        
        // lastModifiedAt should be updated
        assertTrue(fetchedPatchedGroup.getInfo().getLastModifiedAt().isAfter(originalLastModifiedAt),
            "lastModifiedAt should be updated to a newer timestamp after patch");
        
        // They should now be different
        assertNotEquals(fetchedPatchedGroup.getInfo().getCreatedAt(), fetchedPatchedGroup.getInfo().getLastModifiedAt(),
            "After patch, createdAt and lastModifiedAt must be different");
    }
}
