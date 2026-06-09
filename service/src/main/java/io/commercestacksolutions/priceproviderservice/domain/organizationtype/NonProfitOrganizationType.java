package io.commercestacksolutions.priceproviderservice.domain.organizationtype;

import org.springframework.stereotype.Component;

@Component
public class NonProfitOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("NON_PROFIT_ORGANIZATION");
    }

    @Override
    public String getDisplayName() {
        return "Non Profit Organization";
    }
}
