package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype;

import org.springframework.stereotype.Component;

@Component
public class PublicInstitutionOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("PUBLIC_INSTITUTION");
    }

    @Override
    public String getDisplayName() {
        return "Public Institution";
    }
}
