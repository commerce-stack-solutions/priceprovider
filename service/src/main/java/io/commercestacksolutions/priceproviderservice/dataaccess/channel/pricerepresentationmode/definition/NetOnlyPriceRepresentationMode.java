package io.commercestacksolutions.priceproviderservice.dataaccess.channel.pricerepresentationmode.definition;

import io.commercestacksolutions.priceproviderservice.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeDefinition;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeType;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.springframework.stereotype.Component;

/**
 * {@link PriceRepresentationModeDefinition} that publishes only net prices.
 *
 * <p>Prices already declared as gross are excluded from the Public Price API response.
 * Prices declared as net are returned as-is (no conversion applied).</p>
 */
@Component("NET_ONLY")
public class NetOnlyPriceRepresentationMode implements PriceRepresentationModeDefinition {

    @Override
    public PriceRepresentationModeType getModeType() {
        return new PriceRepresentationModeType("NET_ONLY");
    }

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
