package io.commercestacksolutions.commons.service.entity.authorization;

import io.commercestacksolutions.commons.permissionselector.PermissionMatcher;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for entity-level authorization checks using permission selectors.
 * This service provides centralized authorization logic that can be used across all entity services.
 */
@Service
public class EntityAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(EntityAuthorizationService.class);

    private final PermissionMatcher permissionMatcher;
    private final AuthorizationContext authorizationContext;

    public EntityAuthorizationService(PermissionMatcher permissionMatcher, AuthorizationContext authorizationContext) {
        this.permissionMatcher = permissionMatcher;
        this.authorizationContext = authorizationContext;
    }

    /**
     * Checks if the current user has permission to access the given entity.
     *
     * @param entity     the entity to check access for
     * @param entityType the entity type name (e.g., "PriceRow", "Channel")
     * @param action     the action to perform (read, write, delete)
     * @param entityId   the entity identifier for logging purposes
     * @param <T>        the entity type
     * @throws AccessDeniedException if the user doesn't have permission
     */
    public <T> void checkAccess(T entity, String entityType, String action, Object entityId) {
        // Skip authorization checks during bootstrap/data import
        if (AuthorizationContext.isBootstrapMode()) {
            logger.debug("Bootstrap mode active - skipping authorization check for {} on {} with id '{}'",
                action, entityType, entityId);
            return;
        }

        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        boolean hasAccess = permissionMatcher.hasAccess(permissions, entityType, action, entity);

        if (!hasAccess) {
            logger.warn("Access denied for action '{}' on {} with id '{}'", action, entityType, entityId);
            throw new AccessDeniedException("Access denied to " + entityType + " with id " + entityId);
        }
    }

    /**
     * Checks if the current user has permission to perform write or delete operations
     * on both the before and after states of an entity.
     *
     * For write operations, this ensures the user has permission to:
     * 1. Modify the existing entity (before state) - if it exists
     * 2. Create/update to the new entity (after state)
     *
     * For delete operations, this ensures the user has permission to:
     * 1. Access the existing entity (before state)
     *
     * @param entityBefore the entity state before changes (null for new entities)
     * @param entityAfter  the entity state after changes (null for delete operations)
     * @param entityType   the entity type name (e.g., "PriceRow", "Channel")
     * @param action       the action to perform (write, delete)
     * @param entityId     the entity identifier for logging purposes
     * @param <T>          the entity type
     * @throws AccessDeniedException if the user doesn't have permission for either state
     */
    public <T> void checkAccessBeforeAndAfter(T entityBefore, T entityAfter, String entityType, String action, Object entityId) {
        // Skip authorization checks during bootstrap/data import
        if (AuthorizationContext.isBootstrapMode()) {
            logger.debug("Bootstrap mode active - skipping before/after authorization check for {} on {} with id '{}'",
                action, entityType, entityId);
            return;
        }

        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

        // Check permission on the "before" state (existing object in database)
        if (entityBefore != null) {
            boolean hasAccessBefore = permissionMatcher.hasAccess(permissions, entityType, action, entityBefore);
            if (!hasAccessBefore) {
                logger.warn("Access denied for action '{}' on {} with id '{}' (before state check failed)",
                    action, entityType, entityId);
                throw new AccessDeniedException("Access denied to " + entityType + " with id " + entityId +
                    " (no permission for existing state)");
            }
            logger.debug("Permission check passed for {} on {} with id '{}' (before state)",
                action, entityType, entityId);
        }

        // Check permission on the "after" state (changed object)
        if (entityAfter != null) {
            boolean hasAccessAfter = permissionMatcher.hasAccess(permissions, entityType, action, entityAfter);
            if (!hasAccessAfter) {
                logger.warn("Access denied for action '{}' on {} with id '{}' (after state check failed)",
                    action, entityType, entityId);
                throw new AccessDeniedException("Access denied to " + entityType + " with id " + entityId +
                    " (no permission for new state)");
            }
            logger.debug("Permission check passed for {} on {} with id '{}' (after state)",
                action, entityType, entityId);
        }
    }
}
