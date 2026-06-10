package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.definitions;

import org.springframework.stereotype.Component;

@Component
public class PurchasePriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("PURCHASE_PRICE");
    }

    @Override
    public String getDisplayName() {
        return "Purchase Price";
    }
}
