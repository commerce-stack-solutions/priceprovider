package io.commercestacksolutions.priceproviderservice.service.publicprice;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.group.GroupHierarchyService;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.PriceDeterminationStrategy;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.PriceCandidatesQueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of PublicPriceService.
 * 
 * This service finds the best matching prices based on various criteria,
 * delegating the ranking logic to a configurable PriceDeterminationStrategy
 * and using a PriceCandidatesQueryStrategy for efficient database queries.
 * 
 * The service uses a single SQL query to build group hierarchies with distance
 * levels, avoiding N+1 query problems and enabling distance-based group priority.
 */
@Service
public class PublicPriceServiceImpl implements PublicPriceService {
    
    private final PriceCandidatesQueryStrategy queryStrategy;
    private final PriceDeterminationStrategy priceDeterminationStrategy;
    private final GroupHierarchyService groupHierarchyService;
    
    @Autowired
    public PublicPriceServiceImpl(
            PriceCandidatesQueryStrategy queryStrategy,
            PriceDeterminationStrategy priceDeterminationStrategy,
            GroupHierarchyService groupHierarchyService) {
        this.queryStrategy = queryStrategy;
        this.priceDeterminationStrategy = priceDeterminationStrategy;
        this.groupHierarchyService = groupHierarchyService;
    }
    
    @Override
    @Transactional(readOnly = true)
    public PriceRowEntity findBestPrice(PriceMatchingCriteria criteria) {
        // Get candidate prices for the priced resource
        List<PriceRowEntity> candidates = getCandidatePrices(criteria);
        
        // Build group hierarchy for sorting
        List<GroupWithDistance> groupHierarchy = 
            groupHierarchyService.findAllAncestorsWithDistance(criteria.getGroupId());
        
        // Use strategy to determine best match
        return priceDeterminationStrategy.determineBestPrice(criteria, candidates, groupHierarchy);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PriceRowEntity> findAllPrices(PriceMatchingCriteria criteria) {
        // Get candidate prices for the priced resource
        List<PriceRowEntity> candidates = getCandidatePrices(criteria);
        
        // Build group hierarchy for sorting
        List<GroupWithDistance> groupHierarchy = 
            groupHierarchyService.findAllAncestorsWithDistance(criteria.getGroupId());
        
        // Use strategy to rank all matches by priority
        return priceDeterminationStrategy.rankPrices(criteria, candidates, groupHierarchy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRowEntity> findAllQuantityBestPrices(PriceMatchingCriteria criteria) {
        if (criteria.getPricedResourceId() == null) {
            return List.of();
        }

        // Build group hierarchy
        List<GroupWithDistance> groupHierarchy =
            groupHierarchyService.findAllAncestorsWithDistance(criteria.getGroupId());
        boolean hasGroups = groupHierarchy != null && !groupHierarchy.isEmpty();

        // Find ALL prices matching basic criteria (ignore quantity for now)
        List<PriceRowEntity> allCandidates = queryStrategy.findCandidatePrices(
            criteria.getPricedResourceId(),
            criteria.getCurrencyRef(),
            criteria.getPriceType(),
            criteria.getUnitRef(),
            null, // No quantity filter
            criteria.getReferenceDate(),
            hasGroups,
            hasGroups ? groupHierarchy : List.of(),
            criteria.getChannelId(),
            criteria.getCountryKey(),
            criteria.getTaxIncludedFilter()
        );

        if (allCandidates.isEmpty()) {
            return List.of();
        }

        // Find unique minQuantity values
        java.util.Set<java.math.BigDecimal> quantities = allCandidates.stream()
            .map(PriceRowEntity::getMinQuantity)
            .collect(java.util.stream.Collectors.toSet());

        // For each quantity, find the best price
        return quantities.stream()
            .sorted()
            .map(qty -> {

                final java.math.BigDecimal currentQty = qty;
                List<PriceRowEntity> applicableCandidates = allCandidates.stream()
                    .filter(p -> p.getMinQuantity() == null || p.getMinQuantity().compareTo(currentQty) <= 0)
                    .collect(java.util.stream.Collectors.toList());

                PriceMatchingCriteria qtyCriteria = new PriceMatchingCriteria();
                qtyCriteria.setPricedResourceId(criteria.getPricedResourceId());
                qtyCriteria.setQuantity(qty);
                qtyCriteria.setUnitRef(criteria.getUnitRef());
                qtyCriteria.setCurrencyRef(criteria.getCurrencyRef());
                qtyCriteria.setPriceType(criteria.getPriceType());
                qtyCriteria.setGroupId(criteria.getGroupId());
                qtyCriteria.setChannelId(criteria.getChannelId());
                qtyCriteria.setCountryKey(criteria.getCountryKey());
                qtyCriteria.setTaxationMode(criteria.getTaxationMode());
                qtyCriteria.setTaxIncludedFilter(criteria.getTaxIncludedFilter());
                qtyCriteria.setReferenceDate(criteria.getReferenceDate());

                return priceDeterminationStrategy.determineBestPrice(qtyCriteria, applicableCandidates, groupHierarchy);

            })
            .filter(java.util.Objects::nonNull)

            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets all candidate prices for the priced resource.
     * 
     * Uses the query strategy to filter at database level for optimal performance.
     * The query strategy is responsible for building and executing the appropriate
     * database query based on the criteria.
     * 
     * Group hierarchy is built using a single SQL query with recursive CTE,
     * providing both group IDs and their distance levels for sorting.
     */
    private List<PriceRowEntity> getCandidatePrices(PriceMatchingCriteria criteria) {
        if (criteria.getPricedResourceId() == null) {
            return List.of();
        }
        
        // Build group hierarchy with distance levels using single SQL query
        List<GroupWithDistance> groupHierarchy = 
            groupHierarchyService.findAllAncestorsWithDistance(criteria.getGroupId());
        
        // Use database-level filtering for performance via strategy
        boolean hasGroups = groupHierarchy != null && !groupHierarchy.isEmpty();
        
        return queryStrategy.findCandidatePrices(
            criteria.getPricedResourceId(),
            criteria.getCurrencyRef(),
            criteria.getPriceType(),
            criteria.getUnitRef(),
            criteria.getQuantity(),
            criteria.getReferenceDate(),
            hasGroups,
            hasGroups ? groupHierarchy : List.of(),
            criteria.getChannelId(),
            criteria.getCountryKey(),
            criteria.getTaxIncludedFilter()
        );
    }
}
