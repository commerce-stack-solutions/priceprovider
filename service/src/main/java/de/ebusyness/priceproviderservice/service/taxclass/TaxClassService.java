package de.ebusyness.priceproviderservice.service.taxclass;

import de.ebusyness.commons.exception.InvalidParameterException;

import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for TaxClass entity operations.
 * This interface defines the contract for tax class management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface TaxClassService extends EntityService<TaxClassEntity> {
    
    /**
     * Deletes a tax class by its ID.
     * 
     * @param taxClassId the tax class ID
     */
    void deleteTaxClass(String taxClassId);
    
    /**
     * Retrieves a paginated list of tax classes with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of tax class entities
     */
    Page<TaxClassEntity> getTaxClasses(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Retrieves a tax class by its ID.
     * 
     * @param taxClassId the tax class ID
     * @return the tax class entity, or null if not found
     */
    TaxClassEntity getTaxClass(String taxClassId);
}
