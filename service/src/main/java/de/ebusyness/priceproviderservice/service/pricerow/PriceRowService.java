package de.ebusyness.priceproviderservice.service.pricerow;

import de.ebusyness.commons.exception.InvalidParameterException;

import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for PriceRow entity operations.
 * This interface defines the contract for price row management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface PriceRowService extends EntityService<PriceRowEntity> {
    
    /**
     * Retrieves all price rows.
     * 
     * @return list of all price row entities
     */
    List<PriceRowEntity> findAll();
    
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
    Optional<PriceRowEntity> findById(Long id);
    
    /**
     * Deletes a price row by its ID.
     * 
     * @param id the price row ID
     */
    void deleteById(Long id);
    
    /**
     * Finds a price row by matching key fields (excluding price value).
     * This is used for bulk create-or-update operations to determine if a price row should be updated.
     * 
     * @param pricedResourceId the priced resource identifier
     * @param minQuantity the minimum quantity
     * @param unitRef the unit reference (symbol)
     * @param currencyRef the currency reference (key)
     * @param taxClassRef the tax class reference (id)
     * @param taxIncluded whether tax is included
     * @param priceType the price type
     * @param validFrom the valid from date
     * @param validTo the valid to date
     * @param groupRefs the group references
     * @return optional containing the matching price row if found
     */
    Optional<PriceRowEntity> findByMatchingFields(
        String pricedResourceId,
        BigDecimal minQuantity,
        String unitRef,
        String currencyRef,
        String taxClassRef,
        boolean taxIncluded,
        PriceType priceType,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        Set<String> groupRefs
    );
}
