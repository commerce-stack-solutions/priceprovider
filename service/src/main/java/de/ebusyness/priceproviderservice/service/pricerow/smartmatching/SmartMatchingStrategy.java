package de.ebusyness.priceproviderservice.service.pricerow.smartmatching;

import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy interface for smart matching of price rows in bulk create-or-update operations.
 *
 * <p>When a price row is submitted without an ID, this strategy is used to locate an existing
 * price row that matches the given business fields. If a match is found the row is updated;
 * otherwise a new row is created.
 *
 * <p>The default implementation is {@link DefaultSmartMatchingStrategy}. To provide custom
 * matching logic, declare a Spring bean that implements this interface and annotate it with
 * {@code @Primary} to ensure it takes precedence over the default.
 */
public interface SmartMatchingStrategy {

    /**
     * Finds an existing price row that matches the supplied business key fields.
     *
     * @param pricedResourceId the product / resource identifier
     * @param minQuantity      minimum quantity for the price
     * @param unitRef          unit of measurement reference (e.g. "piece", "kg")
     * @param currencyRef      currency code reference (e.g. "EUR", "USD")
     * @param taxClassRef      tax class identifier reference
     * @param taxIncluded      whether tax is included in the price
     * @param priceType        type of price (e.g. "SALES_PRICE", "PURCHASE_PRICE")
     * @param validFrom        price validity start date (nullable)
     * @param validTo          price validity end date (nullable)
     * @param groupRefs        set of group IDs this price applies to (nullable treated as empty)
     * @return an {@link Optional} containing the matching price row, or empty if none found
     */
    Optional<PriceRowEntity> findMatch(
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
