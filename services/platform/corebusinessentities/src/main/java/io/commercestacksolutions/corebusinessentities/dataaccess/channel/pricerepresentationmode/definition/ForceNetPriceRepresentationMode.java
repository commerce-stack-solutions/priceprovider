package io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.definition;

import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeDefinition;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeType;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.TaxationMode;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationModeDefinition} that publishes all prices as net.
 *
 * <p>Prices declared as gross are converted to net before being returned.
 * Prices declared as net are returned as-is.</p>
 */
@Component("FORCE_NET")
public class ForceNetPriceRepresentationMode implements PriceRepresentationModeDefinition {

    @Override
    public PriceRepresentationModeType getModeType() {
        return new PriceRepresentationModeType("FORCE_NET");
    }

    @Override
    public TaxationMode getTaxationMode() {
        return TaxationMode.NET;
    }

    @Override
    public Boolean getTaxIncludedFilter() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Convert all prices to net (tax excluded) if necessary. Gross prices are recalculated by stripping tax.";
    }
}
