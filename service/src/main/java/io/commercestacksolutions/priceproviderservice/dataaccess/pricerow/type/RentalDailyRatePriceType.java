package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.type;

import org.springframework.stereotype.Component;

@Component
public class RentalDailyRatePriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("RENTAL_DAILY_RATE");
    }

    @Override
    public String getDisplayName() {
        return "Rental Daily Rate";
    }
}
