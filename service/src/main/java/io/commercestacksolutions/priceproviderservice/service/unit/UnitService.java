package io.commercestacksolutions.priceproviderservice.service.unit;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for Unit entity operations.
 * This interface defines the contract for unit management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface UnitService extends EntityService<UnitEntity> {
    
    /**
     * Deletes a unit by its symbol.
     * 
     * @param symbol the unit symbol
     */
    void deleteUnit(String symbol);
    
    /**
     * Finds a unit by its symbol.
     * 
     * @param symbol the unit symbol
     * @return the unit entity, or null if not found
     */
    UnitEntity findBySymbol(String symbol);
    
    /**
     * Retrieves a paginated list of units with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of unit entities
     */
    Page<UnitEntity> getUnits(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Retrieves a unit by its symbol.
     * 
     * @param symbol the unit symbol
     * @return the unit entity, or null if not found
     */
    UnitEntity getUnit(String symbol);
}
