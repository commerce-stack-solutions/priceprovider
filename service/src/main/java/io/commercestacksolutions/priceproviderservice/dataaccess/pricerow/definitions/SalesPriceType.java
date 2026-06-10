package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.definitions;

import org.springframework.stereotype.Component;

@Component
public class SalesPriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("SALES_PRICE");
    }

    @Override
    public String getDisplayName() {
        return "Sales Price";
    }
}
