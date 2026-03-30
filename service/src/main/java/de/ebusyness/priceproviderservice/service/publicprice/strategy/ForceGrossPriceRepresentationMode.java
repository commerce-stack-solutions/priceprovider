package de.ebusyness.priceproviderservice.service.publicprice.strategy;

import de.ebusyness.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationMode} that converts all prices to gross before publishing.
 *
 * <p>All prices — regardless of how they are declared — are returned as gross prices
 * (tax included).  Net prices are converted using the channel's tax rounding strategy.</p>
 */
@Component("FORCE_GROSS")
public class ForceGrossPriceRepresentationMode implements PriceRepresentationMode {

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
        return "Publish all prices as gross. Any net prices are converted to gross before publishing.";
    }
}
