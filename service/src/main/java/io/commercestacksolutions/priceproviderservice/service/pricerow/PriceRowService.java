package io.commercestacksolutions.priceproviderservice.service.pricerow;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.PriceRowMatchingContext;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for PriceRow entity operations.
 * This interface defines the contract for price row management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface PriceRowService extends EntityService<PriceRowEntity> {

    /**
     * Retrieves a paginated list of price rows.
     *
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @return page of price row entities
     */
    Page<PriceRowEntity> findAll(int page, int pageSize);
    
    /**
     * Retrieves a paginated list of price rows with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of price row entities
     */
    Page<PriceRowEntity> findAll(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Finds a price row by its ID.
     * 
     * @param id the price row ID
     * @return optional containing the price row entity if found
     */
    Optional<PriceRowEntity> findById(String id);
    
    /**
     * Deletes a price row by its ID.
     * 
     * @param id the price row ID
     */
    void deleteById(String id);
    
    /**
     * Finds a price row matching the given criteria using the configured
     * {@link io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.SmartMatchingStrategy}.
     * This is used by bulk create-or-update operations to determine whether to update an
     * existing row or create a new one.
     *
     * @param context the matching criteria
     * @return optional containing the matching price row if found
     */
    Optional<PriceRowEntity> findByMatchingFields(PriceRowMatchingContext context);
}
