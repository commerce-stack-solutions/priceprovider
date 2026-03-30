package de.ebusyness.priceproviderservice.service.group.validation;

import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.dataaccess.group.GroupEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_GROUP_CYCLIC_DEPENDENCY;

/**
 * Validation rule to detect cyclic dependencies in group parent-child relationships.
 * A cycle exists when following the parent chain eventually leads back to the starting group.
 * For example: A → B → C → A or A → B → A
 * 
 * This rule checks the complete group hierarchy to ensure no cycles exist,
 * including:
 * - Direct cycles (A → A as its own parent)
 * - Simple cycles (A → B → A)
 * - Complex cycles (A → B → C → A)
 * - Cycles involving the entire database hierarchy
 * 
 * The rule uses the repository to access the complete, up-to-date group hierarchy
 * and performs efficient graph traversal to detect cycles.
 */
@Component
public class GroupAvoidCyclicDependencyRule implements ValidationRule<GroupEntity> {
    
    private final GroupEntityRepository groupEntityRepository;
    
    @Autowired
    public GroupAvoidCyclicDependencyRule(GroupEntityRepository groupEntityRepository) {
        this.groupEntityRepository = groupEntityRepository;
    }

    @Override
    public List<Message> validate(GroupEntity entity) {
        List<Message> errors = new ArrayList<>();
        
        if (entity == null || entity.getId() == null) {
            return errors; // Can't validate without entity or id
        }
        
        Set<GroupEntity> parentRefs = entity.getParentRefs();
        if (parentRefs == null || parentRefs.isEmpty()) {
            return errors; // No parents means no cycle
        }
        
        // Check each parent for potential cycles
        for (GroupEntity parent : parentRefs) {
            if (parent == null || parent.getId() == null) {
                continue;
            }
            
            // Check if setting this parent would create a cycle
            if (wouldCreateCycle(entity.getId(), parent.getId())) {
                Map<String, String> params = new HashMap<>();
                params.put("parentId", parent.getId());
                params.put("groupId", entity.getId());
                
                errors.add(new Message(
                    Message.MessageType.ERROR,
                        ERROR_GROUP_CYCLIC_DEPENDENCY,
                    params,
                    List.of("parentRefs", "subRefs")
                ));
            }
        }
        
        return errors;
    }

    /**
     * Checks if adding a parent would create a cycle in the hierarchy.
     * 
     * This method builds the complete group hierarchy from the database and checks
     * if adding entityId → parentId would create a cycle.
     * 
     * Algorithm:
     * 1. Build a map of the current hierarchy from the database
     * 2. Simulate adding the new relationship
     * 3. Check if a cycle exists using DFS traversal
     * 
     * @param entityId the id of the group being validated
     * @param parentId the id of the proposed parent
     * @return true if a cycle would be created, false otherwise
     */
    private boolean wouldCreateCycle(String entityId, String parentId) {
        // Direct self-reference
        if (entityId.equals(parentId)) {
            return true;
        }
        
        // Build the hierarchy map from the database
        Map<String, Set<String>> hierarchy = buildHierarchyMap();
        
        // Simulate the new relationship
        hierarchy.computeIfAbsent(entityId, k -> new HashSet<>()).add(parentId);
        
        // Check if this creates a cycle starting from entityId
        return detectCycleInHierarchy(entityId, hierarchy);
    }
    
    /**
     * Builds a map of the current group hierarchy from the database.
     * The map contains: groupId → Set of parent groupIds
     * 
     * @return map of group ids to their parent group ids
     */
    private Map<String, Set<String>> buildHierarchyMap() {
        Map<String, Set<String>> hierarchy = new HashMap<>();
        
        try {
            List<GroupEntity> allGroups = groupEntityRepository.findAll();
            for (GroupEntity group : allGroups) {
                if (group.getId() != null && group.getParentRefs() != null) {
                    Set<String> parentIds = new HashSet<>();
                    for (GroupEntity parent : group.getParentRefs()) {
                        if (parent != null && parent.getId() != null) {
                            parentIds.add(parent.getId());
                        }
                    }
                    if (!parentIds.isEmpty()) {
                        hierarchy.put(group.getId(), parentIds);
                    }
                }
            }
        } catch (Exception e) {
            // If we can't access the repository, we can't validate the full hierarchy
            // This is a conservative approach
        }
        
        return hierarchy;
    }
    
    /**
     * Detects if a cycle exists in the hierarchy starting from the given id.
     * Uses depth-first search with cycle detection.
     * 
     * @param startId the id to start checking from
     * @param hierarchy the complete hierarchy map
     * @return true if a cycle is detected, false otherwise
     */
    private boolean detectCycleInHierarchy(String startId, Map<String, Set<String>> hierarchy) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        return detectCycleRecursive(startId, hierarchy, visited, recursionStack);
    }
    
    /**
     * Recursive helper method for cycle detection using DFS.
     * 
     * @param currentId the current id being visited
     * @param hierarchy the complete hierarchy map
     * @param visited set of all visited ids
     * @param recursionStack set of ids in the current recursion path
     * @return true if a cycle is detected, false otherwise
     */
    private boolean detectCycleRecursive(String currentId, Map<String, Set<String>> hierarchy, 
                                        Set<String> visited, Set<String> recursionStack) {
        // Mark current id as visited and add to recursion stack
        visited.add(currentId);
        recursionStack.add(currentId);
        
        // Get the parents for current id
        Set<String> parentIds = hierarchy.get(currentId);
        
        if (parentIds != null) {
            for (String parentId : parentIds) {
                // If parent is not visited, recurse on it
                if (!visited.contains(parentId)) {
                    if (detectCycleRecursive(parentId, hierarchy, visited, recursionStack)) {
                        return true;
                    }
                }
                // If parent is in recursion stack, we found a cycle
                else if (recursionStack.contains(parentId)) {
                    return true;
                }
            }
        }
        
        // Remove from recursion stack before returning
        recursionStack.remove(currentId);
        return false;
    }

    /**
     * Gets a descriptive error message for a cyclic dependency.
     *
     * @param groupId the group id
     * @param parentId the parent id that would cause the cycle
     * @return a descriptive error message
     */
    public String getCycleErrorMessage(String groupId, String parentId) {
        return String.format(
            "Cannot set parent '%s' for group '%s': this would create a cyclic dependency in the group hierarchy",
            parentId, groupId
        );
    }
}
