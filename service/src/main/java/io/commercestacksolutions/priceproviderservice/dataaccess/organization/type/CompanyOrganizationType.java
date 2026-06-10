package io.commercestacksolutions.priceproviderservice.dataaccess.organization.type;

import org.springframework.stereotype.Component;

@Component
public class CompanyOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("COMPANY");
    }

    @Override
    public String getDisplayName() {
        return "Company";
    }
}
