package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.definition;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class RentalDailyRatePriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("RENTAL_DAILY_RATE");
    }
}
