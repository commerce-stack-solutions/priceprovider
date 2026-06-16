package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationTypeRegistry extends EnumTypeValueRegistry<OrganizationType, OrganizationTypeDefinition> {

    public OrganizationTypeRegistry(List<OrganizationTypeDefinition> definitions) {
        super(definitions, OrganizationType::code);
    }

}
