package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueDefinition;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;

/**
 * Strategy interface for the price representation mode of a channel.
 */
public interface PriceRepresentationMode extends EnumTypeValueDefinition<PriceRepresentationModeType> {

    PriceRepresentationModeType getModeType();

    @Override
    default PriceRepresentationModeType getValue() {
        return getModeType();
    }

    /**
     * Returns the taxation conversion mode to apply when publishing prices.
     */
    PriceMatchingCriteria.TaxationMode getTaxationMode();

    /**
     * Returns the required {@code taxIncluded} value used to filter candidate prices.
     */
    Boolean getTaxIncludedFilter();

    /**
     * Returns a short human-readable description of this price representation mode.
     */
    String getDescription();
}
