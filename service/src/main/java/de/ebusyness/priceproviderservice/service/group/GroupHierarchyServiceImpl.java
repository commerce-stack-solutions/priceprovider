package de.ebusyness.priceproviderservice.service.group;

import de.ebusyness.priceproviderservice.dataaccess.group.GroupEntityRepository;
import de.ebusyness.priceproviderservice.service.group.model.GroupWithDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of GroupHierarchyService.
 * 
 * This service uses a single SQL query with a recursive CTE to efficiently
 * traverse group parent relationships and build complete ancestor hierarchies
 * with distance levels.
 * 
 * This approach avoids the N+1 query problem that would occur with recursive
 * Java traversal, making it much more efficient for deep hierarchies.
 */
@Service
public class GroupHierarchyServiceImpl implements GroupHierarchyService {
    
    private final GroupEntityRepository groupEntityRepository;
    
    @Autowired
    public GroupHierarchyServiceImpl(GroupEntityRepository groupEntityRepository) {
        this.groupEntityRepository = groupEntityRepository;
    }
    
    @Override
    public List<GroupWithDistance> findAllAncestorsWithDistance(String groupId) {
        if (groupId == null) {
            return new ArrayList<>();
        }
        
        // Use single SQL query with recursive CTE to find all ancestors with distances
        List<Object[]> results = groupEntityRepository.findAllAncestorsWithDistance(groupId);
        
        // Convert Object[] results to GroupWithDistance objects
        List<GroupWithDistance> ancestors = new ArrayList<>();
        for (Object[] row : results) {
            String ancestorGroupId = (String) row[0];
            Integer level = ((Number) row[1]).intValue();  // Handle both Integer and Long
            ancestors.add(new GroupWithDistance(ancestorGroupId, level));
        }
        
        return ancestors;
    }
}
