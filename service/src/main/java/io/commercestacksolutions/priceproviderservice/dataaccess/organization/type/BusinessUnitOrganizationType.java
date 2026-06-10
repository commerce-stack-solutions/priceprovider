package io.commercestacksolutions.priceproviderservice.dataaccess.organization.type;

import org.springframework.stereotype.Component;

@Component
public class BusinessUnitOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("BUSINESS_UNIT");
    }

    @Override
    public String getDisplayName() {
        return "Business Unit";
    }
}
