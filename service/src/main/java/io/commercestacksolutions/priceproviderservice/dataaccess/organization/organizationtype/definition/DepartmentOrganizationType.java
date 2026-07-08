package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.definition;

import io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.OrganizationType;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.OrganizationTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class DepartmentOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("DEPARTMENT");
    }
}
