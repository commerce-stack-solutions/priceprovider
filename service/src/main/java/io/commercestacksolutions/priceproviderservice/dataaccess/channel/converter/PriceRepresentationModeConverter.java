package io.commercestacksolutions.priceproviderservice.dataaccess.channel.converter;

import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.PriceRepresentationModeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PriceRepresentationModeConverter implements AttributeConverter<PriceRepresentationModeType, String> {

    @Override
    public String convertToDatabaseColumn(PriceRepresentationModeType attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public PriceRepresentationModeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new PriceRepresentationModeType(dbData);
    }
}
