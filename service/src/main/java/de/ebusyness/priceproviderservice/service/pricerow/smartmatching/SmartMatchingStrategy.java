package de.ebusyness.priceproviderservice.service.pricerow.smartmatching;

import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;

import java.util.Optional;

/**
 * Strategy interface for smart matching of price rows in bulk create-or-update operations.
 *
 * <p>When a price row is submitted without an ID, this strategy is used to locate an existing
 * price row that matches the given business fields. If a match is found the row is updated;
 * otherwise a new row is created.
 *
 * <p>Each implementation receives a {@link PriceRowMatchingContext} and may query the
 * database using any criteria it needs. Implementations are free to build their own
 * JPA specifications, native queries, or any other data-access mechanism.
 *
 * <p>The default implementation is {@link DefaultSmartMatchingStrategy}. To provide custom
 * matching logic, declare a Spring bean that implements this interface and annotate it with
 * {@code @Primary} to ensure it takes precedence over the default.
 */
public interface SmartMatchingStrategy {

    /**
     * Finds an existing price row that matches the criteria in the supplied context.
     *
     * @param context the matching criteria; each strategy decides which fields it uses
     * @return an {@link Optional} containing the matching price row, or empty if none found
     */
    Optional<PriceRowEntity> findMatch(PriceRowMatchingContext context);
}
