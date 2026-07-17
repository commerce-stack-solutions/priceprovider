package io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.definition;

import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationType;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class NonProfitOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("NON_PROFIT_ORGANIZATION");
    }
}
