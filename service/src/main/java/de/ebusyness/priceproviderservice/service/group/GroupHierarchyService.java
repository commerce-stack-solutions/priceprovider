package de.ebusyness.priceproviderservice.service.group;

import de.ebusyness.priceproviderservice.service.group.model.GroupWithDistance;

import java.util.List;

/**
 * Service for working with group hierarchies.
 * 
 * Groups can have parent-child relationships forming a network hierarchy.
 * This service provides methods to traverse and analyze these hierarchies.
 */
public interface GroupHierarchyService {
    
    /**
     * Finds all ancestor groups in the hierarchy for a given group with their distance levels.
     * 
     * This method uses a single SQL query with a recursive CTE to traverse the group
     * parent relationships and build a complete list of all ancestor groups with their
     * distance from the starting group.
     * 
     * Distance levels:
     * - Level 0: The group itself
     * - Level 1: Direct parent
     * - Level 2: Grandparent
     * - Level 3+: Further ancestors
     * 
     * The result includes:
     * - The group itself (level 0)
     * - All direct parents (level 1)
     * - All indirect ancestors with their respective levels
     * 
     * This is useful for determining which price rows should be considered
     * when evaluating prices for a specific group, and for sorting them by
     * proximity (nearest group wins).
     * 
     * @param groupId the group ID to find ancestors for (nullable)
     * @return list of GroupWithDistance objects including the group itself and all ancestors,
     *         ordered by distance level (nearest first), empty list if groupId is null
     */
    List<GroupWithDistance> findAllAncestorsWithDistance(String groupId);
}
