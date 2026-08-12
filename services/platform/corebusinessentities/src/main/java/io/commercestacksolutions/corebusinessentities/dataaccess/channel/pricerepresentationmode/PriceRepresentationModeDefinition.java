package io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueDefinition;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.TaxationMode;

/**
 * Strategy interface for the price representation mode of a channel.
 */
public interface PriceRepresentationModeDefinition extends EnumTypeValueDefinition<PriceRepresentationModeType> {

    PriceRepresentationModeType getModeType();

    @Override
    default PriceRepresentationModeType getValue() {
        return getModeType();
    }

    /**
     * Returns the taxation conversion mode to apply when publishing prices.
     */
    TaxationMode getTaxationMode();

    /**
     * Returns the required {@code taxIncluded} value used to filter candidate prices.
     */
    Boolean getTaxIncludedFilter();

    /**
     * Returns a short human-readable description of this price representation mode.
     */
    String getDescription();
}
