package io.commercestacksolutions.priceproviderservice.dataaccess.organization.organizationtype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrganizationTypeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationTypeRegistry.class);

    private final Map<String, OrganizationTypeDefinition> definitions;

    public OrganizationTypeRegistry(List<OrganizationTypeDefinition> definitions) {
        this.definitions = new HashMap<>();
        if (definitions == null) {
            logger.warn("OrganizationTypeRegistry initialized with no definitions (null/ no definitions provided).");
            return;
        }
        for (OrganizationTypeDefinition d : definitions) {
            if (d == null || d.getOrganizationType() == null || d.getOrganizationType().code() == null) {
                logger.error("OrganizationTypeDefinition or its code is null, skipping: {}", d);
                continue;
            }
            String code = d.getOrganizationType().code().toUpperCase();
            if (this.definitions.containsKey(code)) {
                logger.error("Duplicate OrganizationType code found: {}. Skipping bean: {}", code, d);
            } else {
                this.definitions.put(code, d);
            }
        }

        if (this.definitions.isEmpty()) {
            logger.warn("OrganizationTypeRegistry initialized with no definitions (no definitions added).");
        }
    }

    public boolean exists(String code) {
        return code != null && definitions.containsKey(code.toUpperCase());
    }

    public OrganizationTypeDefinition get(String code) {
        return definitions.get(code);
    }
}
