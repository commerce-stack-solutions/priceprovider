package io.commercestacksolutions.priceproviderservice.dataaccess.organization.type;

import org.springframework.stereotype.Component;

@Component
public class DepartmentOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("DEPARTMENT");
    }

    @Override
    public String getDisplayName() {
        return "Department";
    }
}
