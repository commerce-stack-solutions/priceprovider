package de.ebusyness.priceproviderservice.service.group;

import de.ebusyness.priceproviderservice.dataaccess.group.GroupEntityRepository;
import de.ebusyness.priceproviderservice.service.group.model.GroupWithDistance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupHierarchyServiceImpl.
 * 
 * Tests the group hierarchy traversal using recursive CTE SQL queries.
 */
@ExtendWith(MockitoExtension.class)
public class GroupHierarchyServiceTest {
    
    @Mock
    private GroupEntityRepository groupEntityRepository;
    
    private GroupHierarchyServiceImpl groupHierarchyService;
    
    @BeforeEach
    public void setup() {
        groupHierarchyService = new GroupHierarchyServiceImpl(groupEntityRepository);
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_NullGroupId_ReturnsEmptyList() {
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(groupEntityRepository, never()).findAllAncestorsWithDistance(anyString());
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_NoParents_ReturnsSelfOnly() {
        String groupId = "GROUP-001";
        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{groupId, 0});
        
        when(groupEntityRepository.findAllAncestorsWithDistance(groupId)).thenReturn(dbResults);
        
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(groupId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(groupId, result.get(0).getGroupId());
        assertEquals(0, result.get(0).getLevel());
        verify(groupEntityRepository).findAllAncestorsWithDistance(groupId);
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_SingleParent_ReturnsGroupAndParent() {
        String groupId = "GROUP-CHILD";
        String parentId = "GROUP-PARENT";
        
        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{groupId, 0});
        dbResults.add(new Object[]{parentId, 1});
        
        when(groupEntityRepository.findAllAncestorsWithDistance(groupId)).thenReturn(dbResults);
        
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(groupId);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        assertEquals(groupId, result.get(0).getGroupId());
        assertEquals(0, result.get(0).getLevel());
        
        assertEquals(parentId, result.get(1).getGroupId());
        assertEquals(1, result.get(1).getLevel());
        
        verify(groupEntityRepository).findAllAncestorsWithDistance(groupId);
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_MultipleGenerations_ReturnsCompleteHierarchy() {
        String groupId = "GROUP-GRANDCHILD";
        String parentId = "GROUP-CHILD";
        String grandparentId = "GROUP-PARENT";
        String greatGrandparentId = "GROUP-ROOT";
        
        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{groupId, 0});
        dbResults.add(new Object[]{parentId, 1});
        dbResults.add(new Object[]{grandparentId, 2});
        dbResults.add(new Object[]{greatGrandparentId, 3});
        
        when(groupEntityRepository.findAllAncestorsWithDistance(groupId)).thenReturn(dbResults);
        
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(groupId);
        
        assertNotNull(result);
        assertEquals(4, result.size());
        
        assertEquals(groupId, result.get(0).getGroupId());
        assertEquals(0, result.get(0).getLevel());
        
        assertEquals(parentId, result.get(1).getGroupId());
        assertEquals(1, result.get(1).getLevel());
        
        assertEquals(grandparentId, result.get(2).getGroupId());
        assertEquals(2, result.get(2).getLevel());
        
        assertEquals(greatGrandparentId, result.get(3).getGroupId());
        assertEquals(3, result.get(3).getLevel());
        
        verify(groupEntityRepository).findAllAncestorsWithDistance(groupId);
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_LongDataType_ConvertsToInteger() {
        String groupId = "GROUP-001";
        
        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{groupId, 0L});
        
        when(groupEntityRepository.findAllAncestorsWithDistance(groupId)).thenReturn(dbResults);
        
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(groupId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(groupId, result.get(0).getGroupId());
        assertEquals(0, result.get(0).getLevel());
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_EmptyResult_ReturnsEmptyList() {
        String groupId = "NON-EXISTENT-GROUP";
        
        when(groupEntityRepository.findAllAncestorsWithDistance(groupId)).thenReturn(new ArrayList<>());
        
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(groupId);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(groupEntityRepository).findAllAncestorsWithDistance(groupId);
    }
    
    @Test
    public void testFindAllAncestorsWithDistance_DistanceLevelsAreCorrect() {
        String groupId = "GROUP-001";
        
        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{groupId, 0});
        dbResults.add(new Object[]{"GROUP-PARENT-1", 1});
        dbResults.add(new Object[]{"GROUP-GRANDPARENT-1", 2});
        
        when(groupEntityRepository.findAllAncestorsWithDistance(groupId)).thenReturn(dbResults);
        
        List<GroupWithDistance> result = groupHierarchyService.findAllAncestorsWithDistance(groupId);
        
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getLevel(), "Self should have level 0");
        assertEquals(1, result.get(1).getLevel(), "Parent should have level 1");
        assertEquals(2, result.get(2).getLevel(), "Grandparent should have level 2");
    }
}
