package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.definition;

import io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.OrganizationType;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.OrganizationTypeDefinition;
import org.springframework.stereotype.Component;

@Component
public class PublicInstitutionOrganizationType implements OrganizationTypeDefinition {
    @Override
    public OrganizationType getOrganizationType() {
        return new OrganizationType("PUBLIC_INSTITUTION");
    }
}
