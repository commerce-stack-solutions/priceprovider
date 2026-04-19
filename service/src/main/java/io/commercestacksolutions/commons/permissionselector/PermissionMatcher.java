package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.permissionselector.PermissionNameParser.ParsedPermission;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for matching objects against permission selectors.
 *
 * <p>This service evaluates whether a user's permissions grant access to a specific object
 * or set of objects based on selector expressions in permission names.
 *
 * <p>Permission matching rules:
 * <ul>
 *   <li>If user has a permission without selector (global), access is granted</li>
 *   <li>If user has a permission with selector that evaluates to true, access is granted</li>
 *   <li>Multiple permissions are combined with OR (union) - any match grants access</li>
 * </ul>
 */
@Service
public class PermissionMatcher {

    private static final Logger logger = LoggerFactory.getLogger(PermissionMatcher.class);

    private final PermissionNameParser permissionNameParser = new PermissionNameParser();
    private final SelectorEvaluator selectorEvaluator = new SelectorEvaluator();

    /**
     * Checks if the given permissions allow access to a specific object.
     *
     * @param permissions  the user's permissions
     * @param dataType     the data type to check (e.g., "PriceRow")
     * @param action       the action to check (e.g., "read", "write", "delete")
     * @param target       the object to check access for
     * @return true if access is granted, false otherwise
     */
    public boolean hasAccess(Collection<AppPermissionEntity> permissions, String dataType, String action, Object target) {
        if (permissions == null || permissions.isEmpty()) {
            logAccessDecision(dataType, action, target, false, "No permissions");
            return false;
        }

        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }

        // Find all permissions matching dataType:action
        List<ParsedPermission> matchingPermissions = permissions.stream()
                .map(p -> {
                    try {
                        return permissionNameParser.parse(p.getName());
                    } catch (Exception e) {
                        logger.warn("Failed to parse permission '{}': {}", p.getName(), e.getMessage());
                        return null;
                    }
                })
                .filter(p -> p != null && p.matchesTypeAndAction(dataType, action))
                .collect(Collectors.toList());

        if (matchingPermissions.isEmpty()) {
            logAccessDecision(dataType, action, target, false, "No matching permissions");
            return false;
        }

        // Check each permission
        for (ParsedPermission permission : matchingPermissions) {
            // Global permission (no selector) grants unconditional access
            if (!permission.hasSelector()) {
                logAccessDecision(dataType, action, target, true, "Global permission: " + permission.getFullName());
                return true;
            }

            // Evaluate selector against target object
            try {
                boolean matches = selectorEvaluator.evaluate(permission.getSelector(), target);
                if (matches) {
                    logAccessDecision(dataType, action, target, true, "Matched permission: " + permission.getFullName());
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Failed to evaluate selector for permission '{}' against {}: {}",
                        permission.getFullName(), target.getClass().getSimpleName(), e.getMessage());
            }
        }

        logAccessDecision(dataType, action, target, false, "No permission selector matched");
        return false;
    }

    /**
     * Checks if the given permissions allow access without evaluating against a specific object.
     * This is useful for checking if a user has ANY permission for a dataType:action combination.
     *
     * @param permissions the user's permissions
     * @param dataType    the data type to check
     * @param action      the action to check
     * @return true if user has at least one permission (global or selector-based) for this dataType:action
     */
    public boolean hasAnyPermission(Collection<AppPermissionEntity> permissions, String dataType, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return permissions.stream()
                .map(p -> {
                    try {
                        return permissionNameParser.parse(p.getName());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .anyMatch(p -> p != null && p.matchesTypeAndAction(dataType, action));
    }

    /**
     * Checks if the user has a global (unfiltered) permission for the given dataType:action.
     *
     * @param permissions the user's permissions
     * @param dataType    the data type to check
     * @param action      the action to check
     * @return true if user has a global permission (without selector)
     */
    public boolean hasGlobalPermission(Collection<AppPermissionEntity> permissions, String dataType, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return permissions.stream()
                .map(p -> {
                    try {
                        return permissionNameParser.parse(p.getName());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .anyMatch(p -> p != null && p.matchesTypeAndAction(dataType, action) && !p.hasSelector());
    }

    /**
     * Extracts all parsed permissions for a specific dataType:action combination.
     *
     * @param permissions the user's permissions
     * @param dataType    the data type
     * @param action      the action
     * @return list of parsed permissions matching the criteria
     */
    public List<ParsedPermission> getPermissionsFor(Collection<AppPermissionEntity> permissions, String dataType, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        return permissions.stream()
                .map(p -> {
                    try {
                        return permissionNameParser.parse(p.getName());
                    } catch (Exception e) {
                        logger.debug("Failed to parse permission '{}': {}", p.getName(), e.getMessage());
                        return null;
                    }
                })
                .filter(p -> p != null && p.matchesTypeAndAction(dataType, action))
                .collect(Collectors.toList());
    }

    private void logAccessDecision(String dataType, String action, Object target, boolean granted, String reason) {
        if (logger.isDebugEnabled()) {
            logger.debug("Permission check: {}:{} on {} -> {} ({})",
                    dataType, action, target.getClass().getSimpleName(), granted ? "GRANTED" : "DENIED", reason);
        }
    }
}
