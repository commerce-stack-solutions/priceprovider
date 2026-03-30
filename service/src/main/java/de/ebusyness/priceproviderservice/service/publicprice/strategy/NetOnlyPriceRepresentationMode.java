package de.ebusyness.priceproviderservice.service.publicprice.strategy;

import de.ebusyness.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationMode} that publishes only net prices.
 *
 * <p>Prices already declared as gross are excluded from the Public Price API response.
 * Prices declared as net are returned as-is (no conversion applied).</p>
 */
@Component("NET_ONLY")
public class NetOnlyPriceRepresentationMode implements PriceRepresentationMode {

    @Override
    public PriceMatchingCriteria.TaxationMode getTaxationMode() {
        return PriceMatchingCriteria.TaxationMode.AS_DECLARED;
    }

    @Override
    public Boolean getTaxIncludedFilter() {
        return Boolean.FALSE;
    }

    @Override
    public String getDescription() {
        return "Publish only prices that are already defined as net. Prices defined as gross are excluded.";
    }
}
