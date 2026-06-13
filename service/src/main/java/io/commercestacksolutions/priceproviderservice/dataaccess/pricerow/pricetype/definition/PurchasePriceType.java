package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.definition;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class PurchasePriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("PURCHASE_PRICE");
    }
}
