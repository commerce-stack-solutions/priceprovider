package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.type.PriceType;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Strategy interface for querying candidate prices.
 * 
 * This strategy is responsible for building and executing database queries
 * to retrieve price rows that match the given criteria. The query builder
 * approach allows for different query strategies (e.g., different databases,
 * different optimization strategies).
 * 
 * Implementations should filter at the database level for:
 * - pricedResourceId (exact match)
 * - currencyRef (exact match)
 * - priceType (exact match)
 * - unitRef (exact match or null for any)
 * - minQuantity (price rows where minQuantity <= requested quantity)
 * - validFrom/validTo date range (referenceDate within range)
 * - group assignments (either no groups, or matching group hierarchy with distance-based priority)
 */
public interface PriceCandidatesQueryStrategy {
    
    /**
     * Finds candidate price rows matching the given criteria.
     * 
     * The results are pre-sorted by group distance when group hierarchy is provided,
     * with nearer groups taking priority over farther groups.
     * 
     * @param pricedResourceId the priced resource ID (required)
     * @param currencyRef the currency reference key (required)
     * @param priceType the price type (required)
     * @param unitRef the unit reference symbol (optional, null = any unit)
     * @param quantity the quantity for min quantity filtering (required)
     * @param referenceDate the reference date for validity filtering (required)
     * @param hasGroups true if group filtering should be applied, false for no-group-only filtering
     * @param groupHierarchy list of groups with their distance levels (used only when hasGroups is true)
     * @param channelId the channel ID for channel filtering (optional, null = no channel filter)
     * @param countryKey the country ISO key for country filtering via taxClass (optional, null = no country filter)
     * @param taxIncludedFilter filter by taxIncluded flag: null = include all, false = only net prices, true = only gross prices
     * @return list of matching price rows with preliminary sorting by group distance
     */
    List<PriceRowEntity> findCandidatePrices(
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
        Boolean taxIncludedFilter
    );
}
