package io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.definition;

import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeDefinition;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeType;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.TaxationMode;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationModeDefinition} that publishes only gross prices.
 *
 * <p>Prices declared as net are excluded from the Public Price API response.
 * Prices declared as gross are returned as-is (no conversion applied).</p>
 */
@Component("GROSS_ONLY")
public class GrossOnlyPriceRepresentationMode implements PriceRepresentationModeDefinition {

    @Override
    public PriceRepresentationModeType getModeType() {
        return new PriceRepresentationModeType("GROSS_ONLY");
    }

    @Override
    public TaxationMode getTaxationMode() {
        return TaxationMode.AS_DECLARED;
    }

    @Override
    public Boolean getTaxIncludedFilter() {
        return Boolean.TRUE;
    }

    @Override
    public String getDescription() {
        return "Publish only prices that are already defined as gross. Prices defined as net are excluded.";
    }
}
