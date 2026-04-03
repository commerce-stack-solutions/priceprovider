package io.commercestacksolutions.priceproviderservice.dataaccess.group;

import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupEntityRepository extends JpaRepository<GroupEntity, UUID>, JpaSpecificationExecutor<GroupEntity> {

    /**
     * Find a group by its path (unique human-readable identifier).
     *
     * @param path the unique path of the group
     * @return optional containing the group entity if found
     */
    Optional<GroupEntity> findByPath(String path);

    /**
     * Check whether a group with the given path exists.
     *
     * @param path the unique path to check
     * @return true if a group with this path exists
     */
    boolean existsByPath(String path);

    /**
     * Finds all ancestor groups using a recursive SQL query.
     * 
     * Returns a list of Object arrays where:
     * - Object[0] = group_id (String representation of UUID)
     * - Object[1] = level (Integer) - distance from the starting group
     * 
     * Level semantics:
     * - 0: The group itself
     * - 1: Direct parent
     * - 2: Grandparent
     * - etc.
     * 
     * Uses a recursive CTE (Common Table Expression) to traverse the group hierarchy
     * in a single query, avoiding N+1 database calls.
     * 
     * @param groupId the starting group ID (UUID as string)
     * @return list of [groupId, level] pairs for the group and all its ancestors
     */
    @Query(value = """
        WITH RECURSIVE ancestors(group_id, level) AS (
            -- Base case: the group itself (level 0)
            SELECT CAST(id AS VARCHAR), 0
            FROM group_entity
            WHERE CAST(id AS VARCHAR) = :groupId
            
            UNION ALL
            
            -- Recursive case: find parents of groups already in the result set
            SELECT CAST(gp.parent_id AS VARCHAR), a.level + 1
            FROM ancestors a
            INNER JOIN group_parents gp ON a.group_id = CAST(gp.group_id AS VARCHAR)
            WHERE a.level < 100  -- Prevent infinite loops, max depth = 100
        )
        SELECT group_id, MIN(level) as level
        FROM ancestors
        GROUP BY group_id
        ORDER BY level, group_id
        """, nativeQuery = true)
    List<Object[]> findAllAncestorsWithDistance(@Param("groupId") String groupId);
}
