package io.commercestacksolutions.priceproviderservice.service.publicprice;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;

import java.util.List;

/**
 * Service interface for public price query operations.
 * This interface defines the contract for finding the best matching prices
 * for third-party consumers (Shop, PIM, etc.).
 * 
 * Following Interface Driven Design (IDD) principles.
 */
public interface PublicPriceService {
    
    /**
     * Finds the best matching price for the given criteria.
     * 
     * The matching algorithm considers:
     * - Date range (validFrom/validTo)
     * - Quantity matching (nearest minQuantity)
     * - Currency and unit (exact match)
     * - Price type priority (order in criteria determines ranking)
     * - Group hierarchy (group-specific prices beat generic prices)
     * 
     * @param criteria the search criteria
     * @return the best matching price entity, or null if no match found
     */
    PriceRowEntity findBestPrice(PriceMatchingCriteria criteria);
    
    /**
     * Finds all matching prices for the given criteria, ranked by priority.
     * 
     * The matching algorithm is the same as findBestPrice, but returns all
     * matches ordered by rank (best match first).
     * 
     * @param criteria the search criteria
     * @return list of matching price entities, ordered by rank (empty if no matches)
     */
    List<PriceRowEntity> findAllPrices(PriceMatchingCriteria criteria);

    /**
     * Finds the best matching prices for all applicable quantity break points.
     *
     * This method identifies all unique minQuantity values that exist for the
     * given priced resource and other criteria, and for each unique quantity,
     * determines the best matching price.
     *
     * @param criteria the search criteria (quantity field is ignored)
     * @return list of best matching prices for each unique quantity tier
     */
    List<PriceRowEntity> findAllQuantityBestPrices(PriceMatchingCriteria criteria);
}
