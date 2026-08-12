package io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.definition;

import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationType;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class HostOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("HOST_ORGANIZATION");
    }
}
