package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationMode} that publishes all prices as gross.
 *
 * <p>Prices declared as net are converted to gross before being returned.
 * Prices declared as gross are returned as-is.</p>
 */
@Component("FORCE_GROSS")
public class ForceGrossPriceRepresentationMode implements PriceRepresentationMode {

    @Override
    public PriceRepresentationModeType getModeType() {
        return new PriceRepresentationModeType("FORCE_GROSS");
    }

    @Override
    public PriceMatchingCriteria.TaxationMode getTaxationMode() {
        return PriceMatchingCriteria.TaxationMode.GROSS;
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
