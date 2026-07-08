package io.commercestacksolutions.priceproviderservice.service.approle.setup;

import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.service.approle.AppRoleService;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppRoleDataImporter extends AbstractSetupDataImporter<AppRoleEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppRoleDataImporter.class);

    private final AppRoleService appRoleService;

    @Autowired
    public AppRoleDataImporter(AppRoleService entityService) {
        super(entityService);
        this.appRoleService = entityService;
    }

    @Override
    public int getPriority() {
        return 46; // Load after AppPermission (45)
    }

    @Override
    public String getEntityTypeName() {
        return "AppRole";
    }

    /**
     * Custom import that performs name-based upsert and resolves permission refs by name.
     * Reads JSON array and constructs AppRoleEntity instances manually.
     */
    @Override
    protected void importFile(String filePath) {
        Path path = Path.of(filePath);
        LOGGER.debug("Lookup data file {}", filePath);
        if (!Files.exists(path)) {
            LOGGER.debug("Data file {} not found. Skipped.", filePath);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            String content = Files.readString(path);
            JsonNode root = objectMapper.readTree(content);
            if (!root.isArray()) {
                LOGGER.error("Unexpected data format for {} - expected JSON array.", filePath);
                return;
            }

            for (JsonNode node : root) {
                String name = node.hasNonNull("name") ? node.get("name").asText() : null;
                if (name == null) {
                    LOGGER.error("Skipping entity without name in file {}", filePath);
                    continue;
                }

                AppRoleEntity role = appRoleService.getAppRoleByName(name)
                        .orElseGet(AppRoleEntity::new);

                role.setName(name);
                if (node.hasNonNull("description")) {
                    role.setDescription(node.get("description").asText());
                }

                if (node.has("permissionRefs") && node.get("permissionRefs").isArray()) {
                    Set<AppPermissionEntity> perms = new HashSet<>();
                    for (JsonNode pref : node.get("permissionRefs")) {
                        if (pref.isTextual()) {
                            AppPermissionEntity perm = new AppPermissionEntity();
                            perm.setName(pref.asText());
                            perms.add(perm);
                        }
                    }
                    role.setPermissionRefs(perms);
                }

                try {
                    appRoleService.save(role);
                } catch (RuntimeException | EntityValidationException e) {
                    LOGGER.error("Unexpected error while saving entity from {}: {}", filePath, role, e);
                }
            }

            LOGGER.info("Data file {} successfully imported.", filePath);
        } catch (IOException e) {
            LOGGER.error("Error occurred while loading data: {}", e.getMessage(), e);
        }
    }
}