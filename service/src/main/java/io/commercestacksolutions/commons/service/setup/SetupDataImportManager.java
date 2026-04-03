package io.commercestacksolutions.commons.service.setup;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.service.approle.AppPermissionService;
import io.commercestacksolutions.priceproviderservice.service.approle.AppRoleService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of DataImportManager interface.
 * Manager that discovers and executes all SetupDataImporter implementations.
 */
@Component
public class SetupDataImportManager implements SelectiveDataImportManager {

    private static final Logger logger = LoggerFactory.getLogger(SetupDataImportManager.class);

    private final List<SetupDataImporter> dataImporters;
    private final AppPermissionService appPermissionService;
    private final AppRoleService appRoleService;

    @Value("${service-config.initialize.data-folder}")
    private String dataFolder;

    @Value("${service-config.initialize.essential-data-on}")
    private Boolean essentialDataOn;

    @Value("${service-config.initialize.sample-data-on}")
    private Boolean sampleDataOn;

    @Autowired
    public SetupDataImportManager(List<SetupDataImporter> dataImporters,
                                  AppPermissionService appPermissionService,
                                  AppRoleService appRoleService) {
        this.dataImporters = dataImporters;
        this.appPermissionService = appPermissionService;
        this.appRoleService = appRoleService;
    }

    @PostConstruct
    @Override
    public void loadData() {
        // Bootstrap: Create minimal permission and role if database is empty
        bootstrapMinimalAccess();

        // Only auto-load if configured to do so
        if (Boolean.TRUE.equals(essentialDataOn)) {
            loadEssentialData();
        }

        if (Boolean.TRUE.equals(sampleDataOn)) {
            loadSampleData();
        }
    }

    /**
     * Bootstrap minimal access control when database is empty.
     * Creates the ServiceInitialization permission and Admin role to allow initial setup.
     */
    private void bootstrapMinimalAccess() {
        try {
            // Check if database is empty (no permissions and no roles)
            boolean hasPermissions = !appPermissionService.getAllAppPermissions().isEmpty();
            boolean hasRoles = !appRoleService.getAllAppRoles().isEmpty();

            if (!hasPermissions && !hasRoles) {
                logger.info("Database is empty. Creating bootstrap permission and role for service initialization.");

                // Create the ServiceInitialization permission
                AppPermissionEntity initPermission = new AppPermissionEntity();
                initPermission.setName("priceprovider.admin:ServiceInitialization:write");
                initPermission.setDescription("Initialize service data");
                appPermissionService.save(initPermission);
                logger.info("Created permission: {}", initPermission.getName());

                // Create the AppRole:read permission (needed to load roles)
                AppPermissionEntity roleReadPermission = new AppPermissionEntity();
                roleReadPermission.setName("priceprovider.admin:AppRole:read");
                roleReadPermission.setDescription("Read app roles");
                appPermissionService.save(roleReadPermission);
                logger.info("Created permission: {}", roleReadPermission.getName());

                // Create the Admin role with both permissions
                AppRoleEntity adminRole = new AppRoleEntity();
                adminRole.setPath("priceprovider.admin:Admin");
                adminRole.setDescription("Full admin access");
                Set<AppPermissionEntity> permissions = new HashSet<>();
                permissions.add(initPermission);
                permissions.add(roleReadPermission);
                adminRole.setPermissionRefs(permissions);
                appRoleService.save(adminRole);
                logger.info("Created role: {} with bootstrap permissions", adminRole.getPath());

                logger.info("Bootstrap complete. Admin users can now access the service initialization page.");
            }
        } catch (Exception e) {
            logger.error("Error during bootstrap: {}", e.getMessage(), e);
        }
    }

    @Override
    public void loadEssentialData() {
        loadEssentialDataInternal(false);
    }

    @Override
    public void loadSampleData() {
        loadSampleDataInternal(false);
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadDataAsync(boolean loadEssential, boolean loadSample) {
        try {
            logger.info("Starting asynchronous data loading: essential={}, sample={}", loadEssential, loadSample);

            // Set force load flag to override configuration
            AbstractSetupDataImporter.setForceLoad(true);

            try {
                if (loadEssential) {
                    loadEssentialDataInternal(true);
                }

                if (loadSample) {
                    loadSampleDataInternal(true);
                }

                logger.info("Asynchronous data loading completed successfully");
            } finally {
                // Always clear the force load flag
                AbstractSetupDataImporter.clearForceLoad();
            }
        } catch (Exception e) {
            logger.error("Error during asynchronous data loading", e);
            throw e;
        }
    }

    private void loadEssentialDataInternal(boolean forceEagerLoad) {
        // Sort data loaders by priority
        List<SetupDataImporter> sortedDataLoaders = dataImporters.stream()
                .sorted((dl1, dl2) -> Integer.compare(dl1.getPriority(), dl2.getPriority()))
                .collect(Collectors.toList());

        // Load essential data
        for (SetupDataImporter dataLoader : sortedDataLoaders) {
            dataLoader.loadEssentialData();
        }
    }

    private void loadSampleDataInternal(boolean forceEagerLoad) {
        // Sort data loaders by priority
        List<SetupDataImporter> sortedDataLoaders = dataImporters.stream()
                .sorted((dl1, dl2) -> Integer.compare(dl1.getPriority(), dl2.getPriority()))
                .collect(Collectors.toList());

        // Load sample data
        for (SetupDataImporter dataLoader : sortedDataLoaders) {
            dataLoader.loadSampleData();
        }
    }

    @Override
    public List<String> getEssentialDataFiles() {
        return getDataFilesFromDirectory(dataFolder + "essential/");
    }

    @Override
    public List<String> getSampleDataFiles() {
        return getDataFilesFromDirectory(dataFolder + "sample/");
    }

    @Override
    public String getEssentialDataDirectory() {
        return dataFolder + "essential/";
    }

    @Override
    public String getSampleDataDirectory() {
        return dataFolder + "sample/";
    }

    private List<String> getDataFilesFromDirectory(String directory) {
        Path dirPath = Path.of(directory);
        List<String> files = new ArrayList<>();

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return files;
        }

        try (Stream<Path> paths = Files.list(dirPath)) {
            files = paths
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            // Return empty list on error
        }

        return files;
    }
}