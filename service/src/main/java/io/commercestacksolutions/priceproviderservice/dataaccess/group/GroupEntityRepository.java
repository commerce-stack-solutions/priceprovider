package io.commercestacksolutions.priceproviderservice.dataaccess.group;

import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupEntityRepository extends JpaRepository<GroupEntity, String>, JpaSpecificationExecutor<GroupEntity> {

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
     * Finds all ancestor groups using a recursive SQL query, starting from a group identified by its path.
     * 
     * Returns a list of Object arrays where:
     * - Object[0] = group_path (String) - the human-readable path of the ancestor
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
     * @param groupPath the path (human-readable identifier) of the starting group
     * @return list of [groupPath, level] pairs for the group and all its ancestors
     */
    @Query(value = """
        WITH RECURSIVE ancestors(group_id, group_path, level) AS (
            -- Base case: the group itself (level 0)
            SELECT id, path, 0
            FROM group_entity
            WHERE path = :groupPath
            
            UNION ALL
            
            -- Recursive case: find parents of groups already in the result set
            SELECT ge.id, ge.path, a.level + 1
            FROM ancestors a
            INNER JOIN group_parents gp ON a.group_id = gp.group_id
            INNER JOIN group_entity ge ON gp.parent_id = ge.id
            WHERE a.level < 100  -- Prevent infinite loops, max depth = 100
        )
        SELECT group_path, MIN(level) as level
        FROM ancestors
        GROUP BY group_path
        ORDER BY level, group_path
        """, nativeQuery = true)
    List<Object[]> findAllAncestorsWithDistance(@Param("groupPath") String groupPath);
}
