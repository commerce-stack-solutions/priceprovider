package io.commercestacksolutions.priceproviderservice.dataaccess.organization.converter;

import io.commercestacksolutions.priceproviderservice.dataaccess.organization.definitions.OrganizationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrganizationTypeConverter implements AttributeConverter<OrganizationType, String> {

    @Override
    public String convertToDatabaseColumn(OrganizationType attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public OrganizationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new OrganizationType(dbData);
    }
}
