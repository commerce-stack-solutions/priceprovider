package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueDefinition;

public interface PriceTypeDefinition extends EnumTypeValueDefinition<PriceType> {
    PriceType getPriceType();

    @Override
    default PriceType getValue() {
        return getPriceType();
    }
}
