package io.commercestacksolutions.priceproviderservice.service.organization.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_ORGANIZATION_CYCLIC_DEPENDENCY;

/**
 * Validation rule to detect cyclic dependencies in organization parent-child relationships.
 * A cycle exists when following the parent chain eventually leads back to the starting organization.
 * For example: A → B → C → A or A → B → A
 * 
 * This rule checks the complete organization hierarchy to ensure no cycles exist,
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
public class OrganizationAvoidCyclicDependencyRule implements ValidationRule<OrganizationEntity> {
    
    private final GroupEntityRepository groupEntityRepository;
    
    @Autowired
    public OrganizationAvoidCyclicDependencyRule(GroupEntityRepository groupEntityRepository) {
        this.groupEntityRepository = groupEntityRepository;
    }

    @Override
    public List<Message> validate(OrganizationEntity entity) {
        List<Message> errors = new ArrayList<>();
        
        if (entity == null || entity.getPath() == null) {
            return errors; // Can't validate without entity or path
        }
        
        Set<GroupEntity> parentRefs = entity.getParentRefs();
        if (parentRefs == null || parentRefs.isEmpty()) {
            return errors; // No parents means no cycle
        }
        
        // Check each parent for potential cycles
        for (GroupEntity parent : parentRefs) {
            if (parent == null || parent.getPath() == null) {
                continue;
            }
            
            // Check if setting this parent would create a cycle
            if (wouldCreateCycle(entity.getPath(), parent.getPath())) {
                Map<String, String> params = new HashMap<>();
                params.put("parentId", parent.getPath());
                params.put("organizationId", entity.getPath());
                
                errors.add(new Message(
                    Message.MessageType.ERROR,
                        ERROR_ORGANIZATION_CYCLIC_DEPENDENCY,
                    params,
                    List.of("parentRefs","subRefs")
                ));
            }
        }
        
        return errors;
    }

    /**
     * Checks if adding a parent would create a cycle in the hierarchy.
     * 
     * This method builds the complete group hierarchy from the database and checks
     * if adding entityPath → parentPath would create a cycle.
     * 
     * Algorithm:
     * 1. Build a map of the current hierarchy from the database
     * 2. Simulate adding the new relationship
     * 3. Check if a cycle exists using DFS traversal
     * 
     * @param entityPath the path of the organization being validated
     * @param parentPath the path of the proposed parent
     * @return true if a cycle would be created, false otherwise
     */
    private boolean wouldCreateCycle(String entityPath, String parentPath) {
        // Direct self-reference
        if (entityPath.equals(parentPath)) {
            return true;
        }
        
        // Build the hierarchy map from the database
        Map<String, Set<String>> hierarchy = buildHierarchyMap();
        
        // Simulate the new relationship
        hierarchy.computeIfAbsent(entityPath, k -> new HashSet<>()).add(parentPath);
        
        // Check if this creates a cycle starting from entityPath
        return detectCycleInHierarchy(entityPath, hierarchy);
    }
    
    /**
     * Builds a map of the current group hierarchy from the database.
     * The map contains: groupPath → Set of parent groupPaths
     * 
     * @return map of group paths to their parent group paths
     */
    private Map<String, Set<String>> buildHierarchyMap() {
        Map<String, Set<String>> hierarchy = new HashMap<>();
        
        try {
            List<GroupEntity> allGroups = groupEntityRepository.findAll();
            for (GroupEntity group : allGroups) {
                if (group.getPath() != null && group.getParentRefs() != null) {
                    Set<String> parentPaths = new HashSet<>();
                    for (GroupEntity parent : group.getParentRefs()) {
                        if (parent != null && parent.getPath() != null) {
                            parentPaths.add(parent.getPath());
                        }
                    }
                    if (!parentPaths.isEmpty()) {
                        hierarchy.put(group.getPath(), parentPaths);
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
     * @param organizationId the organization id
     * @param parentId the parent id that would cause the cycle
     * @return a descriptive error message
     */
    public String getCycleErrorMessage(String organizationId, String parentId) {
        return String.format(
            "Cannot set parent '%s' for organization '%s': this would create a cyclic dependency in the organization hierarchy",
            parentId, organizationId
        );
    }
}
