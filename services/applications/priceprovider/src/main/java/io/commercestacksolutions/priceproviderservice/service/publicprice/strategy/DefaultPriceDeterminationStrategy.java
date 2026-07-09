package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of PriceDeterminationStrategy.
 * 
 * This strategy ranks and selects the best matching prices from a pre-filtered set of candidates.
 * It does NOT perform any filtering - all filtering is done at the database level by
 * PriceCandidatesQueryStrategy for optimal performance.
 * 
 * The strategy applies the following ranking logic:
 * 
 * 1. Group Distance (lowest level wins)
 *    - Prices assigned to groups closer in the hierarchy are ranked higher
 *    - Group-specific prices always rank higher than generic prices (no group)
 *    - Implemented by comparing distance levels from GroupHierarchyService
 * 
 * 2. ValidFrom Date (more recent wins)
 *    - Among prices with same group distance, more recent validFrom dates rank higher
 *    - Prices with validFrom dates rank higher than those without
 * 
 * 3. MinQuantity (higher wins)
 *    - Among prices with same group distance and validFrom, higher minQuantity ranks higher
 *    - This ensures quantity-based tiering works correctly
 *    - Prices with minQuantity rank higher than those without
 * 
 * Pre-filtering at DB level ensures all candidates already match:
 * - pricedResourceId, currency, priceType, unit, minQuantity threshold, date range, group hierarchy
 */
@Component
public class DefaultPriceDeterminationStrategy implements PriceDeterminationStrategy {
    
    public DefaultPriceDeterminationStrategy() {
        // No dependencies needed - candidates are fully pre-processed
    }
    
    @Override
    public PriceRowEntity determineBestPrice(PriceMatchingCriteria criteria, List<PriceRowEntity> availablePrices, List<GroupWithDistance> groupHierarchy) {
        List<PriceRowEntity> matches = rankPrices(criteria, availablePrices, groupHierarchy);
        return matches.isEmpty() ? null : matches.get(0);
    }
    
    @Override
    public List<PriceRowEntity> rankPrices(PriceMatchingCriteria criteria, List<PriceRowEntity> availablePrices, List<GroupWithDistance> groupHierarchy) {
        // Candidates are pre-filtered at DB level
        // We apply all ranking logic including group distance
        List<PriceRowEntity> result = new ArrayList<>(availablePrices);
        
        // Rank by priority (group distance, validFrom, minQuantity)
        result.sort(new PriceRowComparator(criteria, groupHierarchy));
        
        return result;
    }
    
    
    /**
     * Comparator for ranking price rows by priority.
     * 
     * Since candidates are pre-filtered at DB level, all prices in the list already match:
     * - pricedResourceId, currency, priceType, unit, minQuantity, date range, group hierarchy
     * 
     * Priority order for sorting:
     * 1. Currency match (already filtered at DB level, all match - check omitted)
     * 2. PriceType match (already filtered at DB level, all match - check omitted)
     * 3. Unit match (already filtered at DB level, all match - check omitted)
     * 4. Group distance (lowest level wins - IMPLEMENTED HERE)
     *    - Prices with lowest distance level (nearest group) win
     *    - Generic prices (no group) have max distance, so group-specific prices win
     * 5. Nearest validFrom (more recent validFrom wins)
     * 6. Nearest minQuantity (higher minQuantity wins)
     */
    private static class PriceRowComparator implements Comparator<PriceRowEntity> {
        private final PriceMatchingCriteria criteria;
        private final Map<String, Integer> groupDistanceMap;
        
        public PriceRowComparator(PriceMatchingCriteria criteria, List<GroupWithDistance> groupHierarchy) {
            this.criteria = criteria;
            // Build map of groupId -> distance level for quick lookup
            this.groupDistanceMap = (groupHierarchy != null && !groupHierarchy.isEmpty())
                ? groupHierarchy.stream()
                    .collect(Collectors.toMap(
                        GroupWithDistance::getGroupId,
                        GroupWithDistance::getLevel,
                        Math::min  // If duplicate, keep minimum distance
                    ))
                : Map.of();
        }
        
        @Override
        public int compare(PriceRowEntity p1, PriceRowEntity p2) {
            // Priority 1-3: Currency, PriceType, Unit
            // Already filtered at DB level, so all candidates match - no comparison needed
            
            // Priority 4: Group Distance (lowest level wins)
            int dist1 = getMinGroupDistance(p1);
            int dist2 = getMinGroupDistance(p2);
            if (dist1 != dist2) {
                return Integer.compare(dist1, dist2);
            }
            
            // Priority 5: Nearest validFrom (more recent wins)
            if (p1.getValidFrom() != null && p2.getValidFrom() != null) {
                int dateCompare = p2.getValidFrom().compareTo(p1.getValidFrom());
                if (dateCompare != 0) {
                    return dateCompare;
                }
            } else if (p1.getValidFrom() != null) {
                return -1;  // p1 has validFrom, p2 doesn't - p1 wins
            } else if (p2.getValidFrom() != null) {
                return 1;   // p2 has validFrom, p1 doesn't - p2 wins
            }
            
            // Priority 6: Nearest minQuantity (higher wins)
            if (p1.getMinQuantity() != null && p2.getMinQuantity() != null) {
                return p2.getMinQuantity().compareTo(p1.getMinQuantity());
            } else if (p1.getMinQuantity() != null) {
                return -1;  // p1 has minQuantity, p2 doesn't - p1 wins
            } else if (p2.getMinQuantity() != null) {
                return 1;   // p2 has minQuantity, p1 doesn't - p2 wins
            }
            
            return 0;
        }
        
        /**
         * Gets the minimum group distance for a price row.
         * 
         * If the price has no groups, it's considered to have maximum distance (generic price).
         * If the price has groups, returns the minimum distance among all assigned groups.
         * 
         * @param priceRow the price row to evaluate
         * @return minimum distance level (Integer.MAX_VALUE if no groups)
         */
        private int getMinGroupDistance(PriceRowEntity priceRow) {
            Set<String> groupRefs = priceRow.getGroupRefs();
            if (groupRefs == null || groupRefs.isEmpty()) {
                // No groups = generic price with maximum distance
                return Integer.MAX_VALUE;
            }
            
            // Get minimum distance among all groups assigned to this price
            int minDistance = Integer.MAX_VALUE;
            for (String groupId : groupRefs) {
                Integer distance = groupDistanceMap.get(groupId);
                if (distance != null && distance < minDistance) {
                    minDistance = distance;
                }
            }
            
            return minDistance;
        }
    }
}
