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
}
