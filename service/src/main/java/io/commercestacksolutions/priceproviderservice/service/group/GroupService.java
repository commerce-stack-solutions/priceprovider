package io.commercestacksolutions.priceproviderservice.service.group;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Group entity operations.
 * This interface defines the contract for group management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface GroupService extends EntityService<GroupEntity> {
    
    /**
     * Retrieves all groups.
     * 
     * @return list of all group entities
     */
    List<GroupEntity> getAllGroups();
    
    /**
     * Retrieves a paginated list of groups with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of group entities
     */
    Page<GroupEntity> getGroups(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Retrieves a group by its String ID.
     * 
     * @param id the group ID
     * @return optional containing the group entity if found
     */
    Optional<GroupEntity> getGroupById(String id);
    
    /**
     * Retrieves a group by its String ID.
     * 
     * @param id the group ID
     * @return the group entity, or null if not found
     */
    GroupEntity getGroup(String id);

    /**
     * Retrieves a group by its path (unique human-readable identifier).
     *
     * @param path the group path
     * @return the group entity, or null if not found
     */
    GroupEntity getGroupByPath(String path);
    
    /**
     * Updates a group entity.
     * 
     * @param updatedGroup the group entity to update
     * @return the updated group entity
     */
    GroupEntity updateGroup(GroupEntity updatedGroup) throws EntityValidationException;
    
    /**
     * Deletes a group by its String ID.
     * 
     * @param id the group ID
     */
    void deleteGroup(String id);
}
