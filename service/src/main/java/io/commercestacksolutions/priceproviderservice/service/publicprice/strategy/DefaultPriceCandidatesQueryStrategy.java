package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Default implementation of PriceCandidatesQueryStrategy using JPA Criteria API.
 *
 * This implementation uses a query builder approach for flexibility and maintainability.
 * The Criteria API allows for dynamic query construction while maintaining type safety.
 *
 * The query filters price rows at the database level for optimal performance:
 * - Exact matches: pricedResourceId, currencyRef, priceType
 * - Optional exact match: unitRef (null = any unit matches)
 * - Range filters: minQuantity, validFrom/validTo date range
 * - Group filtering: either no groups or matching group hierarchy with distance-based priority
 * - Permission filtering: applies user's permission selectors at the database level
 */
@Component
public class DefaultPriceCandidatesQueryStrategy implements PriceCandidatesQueryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPriceCandidatesQueryStrategy.class);

    private final EntityManager entityManager;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;

    @Autowired
    public DefaultPriceCandidatesQueryStrategy(
            EntityManager entityManager,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext) {
        this.entityManager = entityManager;
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
    }
    
    @Override
    public List<PriceRowEntity> findCandidatePrices(
            String pricedResourceId,
            String currencyRef,
            PriceType priceType,
            String unitRef,
            BigDecimal quantity,
            OffsetDateTime referenceDate,
            boolean hasGroups,
            List<GroupWithDistance> groupHierarchy,
            String channelId,
            String countryKey,
            Boolean taxIncludedFilter) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> priceRow = query.from(PriceRowEntity.class);
        
        // Build predicates list
        List<Predicate> predicates = new ArrayList<>();
        
        // 1. pricedResourceId must match
        predicates.add(cb.equal(priceRow.get("pricedResourceId"), pricedResourceId));
        
        // 2. currencyRef must match (navigate to currencyRef.currencyKey)
        predicates.add(cb.equal(priceRow.get("currencyRef").get("currencyKey"), currencyRef));
        
        // 3. priceType must match
        predicates.add(cb.equal(priceRow.get("priceType"), priceType));
        
        // 4. unitRef must match if specified (navigate to unitRef.symbol)
        if (unitRef != null) {
            predicates.add(cb.equal(priceRow.get("unitRef").get("symbol"), unitRef));
        }
        
        // 5. minQuantity must be <= requested quantity (if provided)
        if (quantity != null) {
            predicates.add(cb.lessThanOrEqualTo(priceRow.get("minQuantity"), quantity));
        }
        
        // 6. Date range filtering
        // validFrom must be null OR <= referenceDate
        predicates.add(cb.or(
            cb.isNull(priceRow.get("validFrom")),
            cb.lessThanOrEqualTo(priceRow.get("validFrom"), referenceDate)
        ));
        
        // validTo must be null OR >= referenceDate
        predicates.add(cb.or(
            cb.isNull(priceRow.get("validTo")),
            cb.greaterThanOrEqualTo(priceRow.get("validTo"), referenceDate)
        ));
        
        // 7. Group filtering
        if (!hasGroups) {
            // No group context: only prices with no group assignments
            predicates.add(cb.isEmpty(priceRow.get("groupRefs")));
        } else {
            // Group context: prices with no groups OR prices matching group hierarchy
            // Extract group paths (@ReferenceKey) for the IN clause
            List<String> groupPaths = groupHierarchy.stream()
                .map(GroupWithDistance::getGroupId)
                .collect(Collectors.toList());
            
            Join<Object, Object> groupJoin = priceRow.join("groupRefs", JoinType.LEFT);
            predicates.add(cb.or(
                cb.isEmpty(priceRow.get("groupRefs")),
                groupJoin.get("path").in(groupPaths)
            ));
        }

        // 8. Channel filtering (optional)
        // If channelId is provided: prices with no channel assignment OR prices assigned to this channel
        if (channelId != null && !channelId.isEmpty()) {
            Join<Object, Object> channelJoin = priceRow.join("channelRefs", JoinType.LEFT);
            predicates.add(cb.or(
                cb.isEmpty(priceRow.get("channelRefs")),
                channelJoin.get("id").in(channelId)
            ));
        }

        // 9. Country filtering via taxClass (optional)
        // If countryKey is provided: filter prices where taxClass.countryRef.isoKey matches countryKey
        if (countryKey != null && !countryKey.isEmpty()) {
            Join<Object, Object> taxClassJoin = priceRow.join("taxClassRef", JoinType.LEFT);
            Join<Object, Object> countryJoin = taxClassJoin.join("countryRef", JoinType.LEFT);
            predicates.add(cb.equal(countryJoin.get("isoKey"), countryKey));
        }

        // 10. taxIncluded filtering (optional)
        // If taxIncludedFilter is provided: filter prices where taxIncluded matches the filter value
        if (taxIncludedFilter != null) {
            predicates.add(cb.equal(priceRow.get("taxIncluded"), taxIncludedFilter));
        }

        // 11. Permission filtering - apply user's permission selectors at database level
        // Skip authorization checks during bootstrap/data import
        if (!AuthorizationContext.isBootstrapMode()) {
            Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

            try {
                Specification<PriceRowEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

                if (permissionSpec != null) {
                    // Permission spec returned non-null means we need to apply filtering
                    // Convert the Specification to a Predicate and add it to our predicates list
                    Predicate permissionPredicate = permissionSpec.toPredicate(priceRow, query, cb);
                    if (permissionPredicate != null) {
                        predicates.add(permissionPredicate);
                        logger.debug("Applied permission-based filtering at database query level");
                    }
                } else {
                    // null spec means global permission without selector - no filtering needed
                    logger.debug("Global permission found - no permission filtering applied at query level");
                }
            } catch (Exception e) {
                logger.error("Failed to build permission filter, denying all access: {}", e.getMessage());
                // On error, add a predicate that denies all access as a safety measure
                predicates.add(cb.disjunction()); // Always false
            }
        } else {
            logger.debug("Bootstrap mode active - skipping permission filtering at query level");
        }

        // Apply all predicates
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        
        // Use distinct to avoid duplicates from group joins
        query.distinct(true);
        
        // Execute query
        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();
        
        // No sorting here - all sorting is done in DefaultPriceDeterminationStrategy
        // This query strategy focuses solely on filtering candidates from the database
        
        return results;
    }
}
