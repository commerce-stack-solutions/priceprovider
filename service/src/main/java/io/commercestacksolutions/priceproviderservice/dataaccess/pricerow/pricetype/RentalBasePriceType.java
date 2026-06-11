package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype;

import org.springframework.stereotype.Component;

@Component
public class RentalBasePriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("RENTAL_BASE_PRICE");
    }

    @Override
    public String getDisplayName() {
        return "Rental Base Price";
    }
}
