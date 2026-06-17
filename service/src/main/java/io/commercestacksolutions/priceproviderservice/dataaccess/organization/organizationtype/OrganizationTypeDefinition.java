package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueDefinition;

public interface OrganizationTypeDefinition extends EnumTypeValueDefinition<OrganizationType> {
    OrganizationType getOrganizationType();

    @Override
    default OrganizationType getValue() {
        return getOrganizationType();
    }
}
