package io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.definition;

import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationType;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class CompanyOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("COMPANY");
    }
}
