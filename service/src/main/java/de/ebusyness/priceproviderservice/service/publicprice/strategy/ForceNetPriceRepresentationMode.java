package de.ebusyness.priceproviderservice.service.publicprice.strategy;

import de.ebusyness.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationMode} that converts all prices to net before publishing.
 *
 * <p>All prices — regardless of how they are declared — are returned as net prices
 * (tax excluded).  Gross prices are converted using the channel's tax rounding strategy.</p>
 */
@Component("FORCE_NET")
public class ForceNetPriceRepresentationMode implements PriceRepresentationMode {

    @Override
    public PriceMatchingCriteria.TaxationMode getTaxationMode() {
        return PriceMatchingCriteria.TaxationMode.NET;
    }

    @Override
    public Boolean getTaxIncludedFilter() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Publish all prices as net. Any gross prices are converted to net before publishing.";
    }
}
