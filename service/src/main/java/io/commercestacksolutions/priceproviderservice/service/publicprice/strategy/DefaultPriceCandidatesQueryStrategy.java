package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Default implementation of PriceCandidatesQueryStrategy using JPA Criteria API.
 *
 * This implementation extends {@link AbstractPermissionAwarePriceCandidatesQueryStrategy}
 * to ensure permission-based filtering is automatically applied at the database level.
 *
 * The query filters price rows at the database level for optimal performance:
 * - Exact matches: pricedResourceId, currencyRef, priceType
 * - Optional exact match: unitRef (null = any unit matches)
 * - Range filters: minQuantity, validFrom/validTo date range
 * - Group filtering: either no groups or matching group hierarchy with distance-based priority
 * - Permission filtering: applies user's permission selectors automatically via abstract base class
 */
@Component
public class DefaultPriceCandidatesQueryStrategy
        extends AbstractPermissionAwarePriceCandidatesQueryStrategy
        implements PriceCandidatesQueryStrategy {

    @Autowired
    public DefaultPriceCandidatesQueryStrategy(
            EntityManager entityManager,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext) {
        super(entityManager, permissionFilterBuilder, authorizationContext);
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

        // Build parameter object
        PriceCandidatesQueryParams params = PriceCandidatesQueryParams.builder()
            .pricedResourceId(pricedResourceId)
            .currencyRef(currencyRef)
            .priceType(priceType)
            .unitRef(unitRef)
            .quantity(quantity)
            .referenceDate(referenceDate)
            .hasGroups(hasGroups)
            .groupHierarchy(groupHierarchy)
            .channelId(channelId)
            .countryKey(countryKey)
            .taxIncludedFilter(taxIncludedFilter)
            .build();

        // Execute query with automatic permission filtering via abstract base class
        return executeQuery(params);
    }

    @Override
    protected void buildBusinessLogicPredicates(
            CriteriaBuilder cb,
            Root<PriceRowEntity> root,
            CriteriaQuery<?> query,
            List<Predicate> predicates,
            PriceCandidatesQueryParams params) {

        // 1. pricedResourceId must match
        predicates.add(cb.equal(root.get("pricedResourceId"), params.getPricedResourceId()));

        // 2. currencyRef must match (navigate to currencyRef.currencyKey)
        predicates.add(cb.equal(root.get("currencyRef").get("currencyKey"), params.getCurrencyRef()));

        // 3. priceType must match
        predicates.add(cb.equal(root.get("priceType"), params.getPriceType()));

        // 4. unitRef must match if specified (navigate to unitRef.symbol)
        if (params.getUnitRef() != null) {
            predicates.add(cb.equal(root.get("unitRef").get("symbol"), params.getUnitRef()));
        }

        // 5. minQuantity must be <= requested quantity (if provided)
        if (params.getQuantity() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("minQuantity"), params.getQuantity()));
        }

        // 6. Date range filtering
        // validFrom must be null OR <= referenceDate
        predicates.add(cb.or(
            cb.isNull(root.get("validFrom")),
            cb.lessThanOrEqualTo(root.get("validFrom"), params.getReferenceDate())
        ));

        // validTo must be null OR >= referenceDate
        predicates.add(cb.or(
            cb.isNull(root.get("validTo")),
            cb.greaterThanOrEqualTo(root.get("validTo"), params.getReferenceDate())
        ));

        // 7. Group filtering
        if (!params.isHasGroups()) {
            // No group context: only prices with no group assignments
            predicates.add(cb.isEmpty(root.get("groupRefs")));
        } else {
            // Group context: prices with no groups OR prices matching group hierarchy
            // Extract group paths (@ReferenceKey) for the IN clause
            List<String> groupPaths = params.getGroupHierarchy().stream()
                .map(GroupWithDistance::getGroupId)
                .collect(Collectors.toList());

            Join<Object, Object> groupJoin = root.join("groupRefs", JoinType.LEFT);
            predicates.add(cb.or(
                cb.isEmpty(root.get("groupRefs")),
                groupJoin.get("path").in(groupPaths)
            ));
        }

        // 8. Channel filtering (optional)
        // If channelId is provided: prices with no channel assignment OR prices assigned to this channel
        if (params.getChannelId() != null && !params.getChannelId().isEmpty()) {
            Join<Object, Object> channelJoin = root.join("channelRefs", JoinType.LEFT);
            predicates.add(cb.or(
                cb.isEmpty(root.get("channelRefs")),
                channelJoin.get("id").in(params.getChannelId())
            ));
        }

        // 9. Country filtering via taxClass (optional)
        // If countryKey is provided: filter prices where taxClass.countryRef.isoKey matches countryKey
        if (params.getCountryKey() != null && !params.getCountryKey().isEmpty()) {
            Join<Object, Object> taxClassJoin = root.join("taxClassRef", JoinType.LEFT);
            Join<Object, Object> countryJoin = taxClassJoin.join("countryRef", JoinType.LEFT);
            predicates.add(cb.equal(countryJoin.get("isoKey"), params.getCountryKey()));
        }

        // 10. taxIncluded filtering (optional)
        // If taxIncludedFilter is provided: filter prices where taxIncluded matches the filter value
        if (params.getTaxIncludedFilter() != null) {
            predicates.add(cb.equal(root.get("taxIncluded"), params.getTaxIncludedFilter()));
        }

        // Note: Permission filtering is automatically added by the abstract base class
        // No sorting here - all sorting is done in DefaultPriceDeterminationStrategy
        // This query strategy focuses solely on filtering candidates from the database
    }
}
