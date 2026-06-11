package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype;

import org.springframework.stereotype.Component;

@Component
public class HostOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("HOST_ORGANIZATION");
    }

    @Override
    public String getDisplayName() {
        return "Host Organization";
    }
}
