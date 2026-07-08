package io.commercestacksolutions.priceproviderservice.config.security;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Spring Security expression methods for permission checking with selector support.
 *
 * <p>This service provides custom security expressions that can be used in @PreAuthorize
 * annotations to check permissions including those with selectors.
 *
 * <p>Usage in controllers:
 * <pre>
 * &#64;PreAuthorize("@permissionSecurityService.hasPermissionForAction('PriceRow', 'read')")
 * </pre>
 */
@Service
public class PermissionSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionSecurityService.class);

    private final AuthorizationContext authorizationContext;
    private final ApiContextResolver apiContextResolver;

    public PermissionSecurityService(AuthorizationContext authorizationContext, ApiContextResolver apiContextResolver) {
        this.authorizationContext = authorizationContext;
        this.apiContextResolver = apiContextResolver;
    }

    /**
     * Checks if the current user has permission for the given entity type and action.
     * This method supports both global permissions and selector-based permissions.
     *
     * <p>The permission prefix (priceprovider.admin or priceprovider.public) is automatically
     * determined from the current API context based on the request path.
     *
     * <p>Examples of matching permissions:
     * <ul>
     *   <li>priceprovider.admin:PriceRow:read (global permission for admin API)</li>
     *   <li>priceprovider.admin:PriceRow[currencyRef=='EUR']:read (selector-based permission for admin API)</li>
     *   <li>priceprovider.public:PriceRow:read (global permission for public API)</li>
     *   <li>priceprovider.public:PriceRow[groupRefs isEmpty]:read (selector-based permission for public API)</li>
     * </ul>
     *
     * @param entityType the entity type name (e.g., "PriceRow", "Channel")
     * @param action     the action (read, write, delete)
     * @return true if the user has ANY permission for this entity type and action in the current API context
     */
    public boolean hasPermissionForAction(String entityType, String action) {
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

        // Determine the correct permission prefix based on API context
        String permissionPrefix = apiContextResolver.getCurrentPermissionPrefix();
        String prefix = permissionPrefix + ":" + entityType;
        String suffix = ":" + action;

        for (AppPermissionEntity permission : permissions) {
            String permissionName = permission.getName();
            if (permissionName != null && permissionName.startsWith(prefix) && permissionName.endsWith(suffix)) {
                // Check if it's either a direct match or has a selector in between
                String middle = permissionName.substring(prefix.length(), permissionName.length() - suffix.length());
                if (middle.isEmpty() || middle.startsWith("[")) {
                    logger.debug("User has permission '{}' matching pattern '{}:*:{}' for API context '{}'",
                        permissionName, entityType, action, permissionPrefix);
                    return true;
                }
            }
        }

        logger.debug("User does not have any permission matching pattern '{}:*:{}' for API context '{}'",
            entityType, action, permissionPrefix);
        return false;
    }

    /**
     * Checks if the current user has the exact permission string.
     * This is a fallback for non-entity permissions like ServiceInitialization.
     *
     * @param permission the exact permission string
     * @return true if the user has this exact permission
     */
    public boolean hasExactPermission(String permission) {
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        for (AppPermissionEntity perm : permissions) {
            if (permission.equals(perm.getName())) {
                return true;
            }
        }
        return false;
    }
}
