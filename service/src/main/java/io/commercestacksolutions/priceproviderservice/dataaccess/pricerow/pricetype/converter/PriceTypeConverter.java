package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.converter;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PriceTypeConverter implements AttributeConverter<PriceType, String> {

    @Override
    public String convertToDatabaseColumn(PriceType attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public PriceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new PriceType(dbData);
    }
}
