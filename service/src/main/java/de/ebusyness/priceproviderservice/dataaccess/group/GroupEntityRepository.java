package de.ebusyness.priceproviderservice.dataaccess.group;

import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupEntityRepository extends JpaRepository<GroupEntity, String>, JpaSpecificationExecutor<GroupEntity> {
    
    /**
     * Finds all ancestor groups using a recursive SQL query.
     * 
     * Returns a list of Object arrays where:
     * - Object[0] = group_id (String)
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
     * @param groupId the starting group ID
     * @return list of [groupId, level] pairs for the group and all its ancestors
     */
    @Query(value = """
        WITH RECURSIVE ancestors(group_id, level) AS (
            -- Base case: the group itself (level 0)
            SELECT id, 0
            FROM group_entity
            WHERE id = :groupId
            
            UNION ALL
            
            -- Recursive case: find parents of groups already in the result set
            SELECT gp.parent_id, a.level + 1
            FROM ancestors a
            INNER JOIN group_parents gp ON a.group_id = gp.group_id
            WHERE a.level < 100  -- Prevent infinite loops, max depth = 100
        )
        SELECT group_id, MIN(level) as level
        FROM ancestors
        GROUP BY group_id
        ORDER BY level, group_id
        """, nativeQuery = true)
    List<Object[]> findAllAncestorsWithDistance(@Param("groupId") String groupId);
}
