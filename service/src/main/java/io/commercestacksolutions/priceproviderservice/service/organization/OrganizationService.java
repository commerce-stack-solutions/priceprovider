package io.commercestacksolutions.priceproviderservice.service.organization;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Organization entity operations.
 * This interface defines the contract for organization management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface OrganizationService extends EntityService<OrganizationEntity> {
    
    /**
     * Retrieves all organizations.
     * 
     * @return list of all organization entities
     */
    List<OrganizationEntity> getAllOrganizations();
    
    /**
     * Retrieves a paginated list of organizations with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of organization entities
     */
    Page<OrganizationEntity> getOrganizations(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Retrieves an organization by its String ID.
     * 
     * @param id the organization ID
     * @return optional containing the organization entity if found
     */
    Optional<OrganizationEntity> getOrganizationById(String id);
    
    /**
     * Retrieves an organization by its String ID.
     * 
     * @param id the organization ID
     * @return the organization entity, or null if not found
     */
    OrganizationEntity getOrganization(String id);

    /**
     * Retrieves an organization by its path (unique human-readable identifier).
     *
     * @param path the organization path
     * @return the organization entity, or null if not found
     */
    OrganizationEntity getOrganizationByPath(String path);
    
    /**
     * Updates an organization entity.
     * 
     * @param updatedOrganization the organization entity to update
     * @return the updated organization entity
     */
    OrganizationEntity updateOrganization(OrganizationEntity updatedOrganization) throws EntityValidationException;
    
    /**
     * Deletes an organization by its String ID.
     * 
     * @param id the organization ID
     */
    void deleteOrganization(String id);
}
