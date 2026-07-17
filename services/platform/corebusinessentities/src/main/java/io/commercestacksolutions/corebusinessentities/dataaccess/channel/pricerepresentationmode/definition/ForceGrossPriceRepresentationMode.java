package io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.definition;

import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeDefinition;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeType;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.TaxationMode;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationModeDefinition} that publishes all prices as gross.
 *
 * <p>Prices declared as net are converted to gross before being returned.
 * Prices declared as gross are returned as-is.</p>
 */
@Component("FORCE_GROSS")
public class ForceGrossPriceRepresentationMode implements PriceRepresentationModeDefinition {

    @Override
    public PriceRepresentationModeType getModeType() {
        return new PriceRepresentationModeType("FORCE_GROSS");
    }

    @Override
    public TaxationMode getTaxationMode() {
        return TaxationMode.GROSS;
    }

    @Override
    public Boolean getTaxIncludedFilter() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Convert all prices to gross (tax included) if necessary. Net prices are recalculated using the applicable tax rate.";
    }
}
