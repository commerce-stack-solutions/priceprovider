package de.ebusyness.priceproviderservice.service.publicprice.strategy;

import de.ebusyness.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;

/**
 * Strategy interface for the price representation mode of a channel.
 *
 * <p>A channel's price representation mode determines how prices are published
 * via the Public Price API:
 * <ul>
 *   <li><b>NET_ONLY</b> – publish only prices declared as net; gross prices are excluded.</li>
 *   <li><b>GROSS_ONLY</b> – publish only prices declared as gross; net prices are excluded.</li>
 *   <li><b>FORCE_NET</b> – publish all prices as net; gross prices are converted before publishing.</li>
 *   <li><b>FORCE_GROSS</b> – publish all prices as gross; net prices are converted before publishing.</li>
 * </ul>
 *
 * <p>Implementations are registered as Spring beans.  The bean name is stored in
 * {@code ChannelEntity.priceRepresentationMode} so that the correct strategy can be
 * resolved at runtime via the application context.
 *
 * <p>Following Open-Closed Principle — additional modes can be added without modifying
 * existing code.
 */
public interface PriceRepresentationMode {

    /**
     * Returns the taxation conversion mode to apply when publishing prices.
     *
     * @return {@link PriceMatchingCriteria.TaxationMode#NET} to convert all prices to net,
     *         {@link PriceMatchingCriteria.TaxationMode#GROSS} to convert all prices to gross,
     *         or {@link PriceMatchingCriteria.TaxationMode#AS_DECLARED} to publish as stored
     */
    PriceMatchingCriteria.TaxationMode getTaxationMode();

    /**
     * Returns the required {@code taxIncluded} value used to filter candidate prices.
     *
     * @return {@code Boolean.FALSE} to include only net prices (taxIncluded=false),
     *         {@code Boolean.TRUE} to include only gross prices (taxIncluded=true),
     *         or {@code null} to include all prices regardless of tax declaration
     */
    Boolean getTaxIncludedFilter();

    /**
     * Returns a short human-readable description of this price representation mode.
     */
    String getDescription();
}
