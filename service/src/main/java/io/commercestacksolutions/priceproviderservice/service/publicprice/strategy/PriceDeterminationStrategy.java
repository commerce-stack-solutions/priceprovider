package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;

import java.util.List;

/**
 * Strategy interface for ranking and selecting the best matching prices.
 * 
 * Implementations define the algorithm for:
 * - Ranking matching prices by priority (including group distance)
 * - Selecting the best match
 * 
 * Note: Filtering is done at DB level by PriceCandidatesQueryStrategy.
 * This strategy focuses solely on ranking pre-filtered candidates.
 * 
 * Following Open-Closed Principle - implementations can be exchanged without modifying existing code.
 */
public interface PriceDeterminationStrategy {
    
    /**
     * Determines the best matching price for the given criteria.
     * Returns null if no matching price is found.
     * 
     * @param criteria the search criteria
     * @param availablePrices all available price rows to search through (pre-filtered by query strategy)
     * @param groupHierarchy group hierarchy with distance levels for group-based sorting (empty for non-group queries)
     * @return the best matching price row, or null if no match found
     */
    PriceRowEntity determineBestPrice(PriceMatchingCriteria criteria, List<PriceRowEntity> availablePrices, List<GroupWithDistance> groupHierarchy);
    
    /**
     * Ranks all matching prices for the given criteria, ordered by priority.
     * Returns empty list if no matching prices are found.
     * 
     * @param criteria the search criteria
     * @param availablePrices all available price rows to search through (pre-filtered by query strategy)
     * @param groupHierarchy group hierarchy with distance levels for group-based sorting (empty for non-group queries)
     * @return list of matching price rows, ordered by rank (best match first)
     */
    List<PriceRowEntity> rankPrices(PriceMatchingCriteria criteria, List<PriceRowEntity> availablePrices, List<GroupWithDistance> groupHierarchy);
}
