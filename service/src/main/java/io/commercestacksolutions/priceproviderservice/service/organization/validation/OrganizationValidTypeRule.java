package io.commercestacksolutions.priceproviderservice.service.organization.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype.OrganizationTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrganizationValidTypeRule implements ValidationRule<OrganizationEntity> {

    private final OrganizationTypeRegistry registry;

    public OrganizationValidTypeRule(OrganizationTypeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<Message> validate(OrganizationEntity entity) {
        if (entity == null || entity.getOrganizationType() == null) {
            return Collections.emptyList();
        }

        if (!registry.exists(entity.getOrganizationType().code())) {
            return Collections.singletonList(new Message(
                Message.MessageType.ERROR,
                "Unknown organization type: " + entity.getOrganizationType().code(),
                List.of("organizationType")
            ));
        }

        return Collections.emptyList();
    }
}
