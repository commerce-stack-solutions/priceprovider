package io.commercestacksolutions.priceproviderservice.service.publicprice;

import io.commercestacksolutions.commons.permissionselector.PermissionMatcher;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.group.GroupHierarchyService;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.PriceDeterminationStrategy;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.PriceCandidatesQueryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of PublicPriceService.
 *
 * This service finds the best matching prices based on various criteria,
 * delegating the ranking logic to a configurable PriceDeterminationStrategy
 * and using a PriceCandidatesQueryStrategy for efficient database queries.
 *
 * The service uses a single SQL query to build group hierarchies with distance
 * levels, avoiding N+1 query problems and enabling distance-based group priority.
 *
 * Permission filtering is applied to candidates based on the current user's permissions,
 * ensuring anonymous users and authenticated users only see prices they're authorized to access.
 */
@Service
public class PublicPriceServiceImpl implements PublicPriceService {

    private static final Logger logger = LoggerFactory.getLogger(PublicPriceServiceImpl.class);

    private final PriceCandidatesQueryStrategy queryStrategy;
    private final PriceDeterminationStrategy priceDeterminationStrategy;
    private final GroupHierarchyService groupHierarchyService;
    private final PermissionMatcher permissionMatcher;
    private final AuthorizationContext authorizationContext;

    @Autowired
    public PublicPriceServiceImpl(
            PriceCandidatesQueryStrategy queryStrategy,
            PriceDeterminationStrategy priceDeterminationStrategy,
            GroupHierarchyService groupHierarchyService,
            PermissionMatcher permissionMatcher,
            AuthorizationContext authorizationContext) {
        this.queryStrategy = queryStrategy;
        this.priceDeterminationStrategy = priceDeterminationStrategy;
        this.groupHierarchyService = groupHierarchyService;
        this.permissionMatcher = permissionMatcher;
        this.authorizationContext = authorizationContext;
    }
    
    @Override
    @Transactional(readOnly = true)
    public PriceRowEntity findBestPrice(PriceMatchingCriteria criteria) {
        // Get candidate prices for the priced resource
        List<PriceRowEntity> candidates = getCandidatePrices(criteria);

        // Apply permission-based filtering
        List<PriceRowEntity> permittedCandidates = filterByPermissions(candidates);

        if (permittedCandidates.isEmpty()) {
            logger.debug("No permitted price candidates found for {}", criteria.getPricedResourceId());
            return null;
        }

        // Build group hierarchy for sorting
        List<GroupWithDistance> groupHierarchy =
            groupHierarchyService.findAllAncestorsWithDistance(criteria.getGroupId());

        // Use strategy to determine best match
        return priceDeterminationStrategy.determineBestPrice(criteria, permittedCandidates, groupHierarchy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRowEntity> findAllPrices(PriceMatchingCriteria criteria) {
        // Get candidate prices for the priced resource
        List<PriceRowEntity> candidates = getCandidatePrices(criteria);

        // Apply permission-based filtering
        List<PriceRowEntity> permittedCandidates = filterByPermissions(candidates);

        if (permittedCandidates.isEmpty()) {
            logger.debug("No permitted price candidates found for {}", criteria.getPricedResourceId());
            return List.of();
        }

        // Build group hierarchy for sorting
        List<GroupWithDistance> groupHierarchy =
            groupHierarchyService.findAllAncestorsWithDistance(criteria.getGroupId());

        // Use strategy to rank all matches by priority
        return priceDeterminationStrategy.rankPrices(criteria, permittedCandidates, groupHierarchy);
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

        // Apply permission-based filtering
        List<PriceRowEntity> permittedCandidates = filterByPermissions(allCandidates);

        if (permittedCandidates.isEmpty()) {
            logger.debug("No permitted price candidates found for {}", criteria.getPricedResourceId());
            return List.of();
        }

        // Find unique minQuantity values
        java.util.Set<java.math.BigDecimal> quantities = permittedCandidates.stream()
            .map(PriceRowEntity::getMinQuantity)
            .collect(java.util.stream.Collectors.toSet());

        // For each quantity, find the best price
        return quantities.stream()
            .sorted()
            .map(qty -> {

                final java.math.BigDecimal currentQty = qty;
                List<PriceRowEntity> applicableCandidates = permittedCandidates.stream()
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

    /**
     * Filters price row candidates based on the current user's permissions.
     *
     * This method applies permission selector evaluation to each candidate price row,
     * ensuring that only prices matching the user's permission selectors are returned.
     *
     * For example, an anonymous user with permission:
     * priceprovider.public:PriceRow[groupRefs isEmpty AND (priceType=='SALES_PRICE' OR ...)]:read
     * will only see prices without group assignment and of the specified price types.
     *
     * @param candidates the list of candidate price rows from the database query
     * @return filtered list containing only prices the user has permission to access
     */
    private List<PriceRowEntity> filterByPermissions(List<PriceRowEntity> candidates) {
        // Skip authorization checks during bootstrap/data import
        if (AuthorizationContext.isBootstrapMode()) {
            logger.debug("Bootstrap mode active - skipping permission filtering");
            return candidates;
        }

        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

        // If user has global PriceRow:read permission (no selector), return all candidates
        if (permissionMatcher.hasGlobalPermission(permissions, "PriceRow", "read")) {
            logger.debug("User has global PriceRow:read permission - no filtering applied");
            return candidates;
        }

        // Filter candidates by evaluating permission selectors against each entity
        List<PriceRowEntity> permittedCandidates = candidates.stream()
            .filter(priceRow -> permissionMatcher.hasAccess(permissions, "PriceRow", "read", priceRow))
            .collect(Collectors.toList());

        logger.debug("Permission filtering: {} candidates -> {} permitted",
            candidates.size(), permittedCandidates.size());

        return permittedCandidates;
    }
}
