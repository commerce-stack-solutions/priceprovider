package io.commercestacksolutions.priceproviderservice.service.approle.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.service.approle.AppPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AppPermissionDataImporter extends AbstractSetupDataImporter<AppPermissionEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppPermissionDataImporter.class);

    private final AppPermissionService appPermissionService;

    @Autowired
    public AppPermissionDataImporter(AppPermissionService entityService) {
        super(entityService);
        this.appPermissionService = entityService;
    }

    @Override
    public int getPriority() {
        return 45; // Load before AppRole (46)
    }

    @Override
    public String getEntityTypeName() {
        return "AppPermission";
    }

    /**
     * Custom import that performs name-based upsert to avoid duplicate inserts
     * on application restart (since id is now auto-generated).
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

                AppPermissionEntity entity = appPermissionService.getAppPermissionByName(name)
                        .orElseGet(AppPermissionEntity::new);

                entity.setName(name);
                if (node.hasNonNull("description")) {
                    entity.setDescription(node.get("description").asText());
                }

                try {
                    appPermissionService.save(entity);
                } catch (Exception e) {
                    LOGGER.error("Unexpected error while saving AppPermissionEntity with name '{}' from {}: {}", name, filePath, e.getMessage(), e);
                }
            }

            LOGGER.info("Data file {} successfully imported.", filePath);
        } catch (IOException e) {
            LOGGER.error("Error occurred while loading data: {}", e.getMessage(), e);
        }
    }
}
