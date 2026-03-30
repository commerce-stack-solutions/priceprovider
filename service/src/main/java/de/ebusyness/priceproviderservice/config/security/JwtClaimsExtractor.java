package de.ebusyness.priceproviderservice.config.security;

import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import de.ebusyness.priceproviderservice.service.approle.AppRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts application roles and organization context from a JWT token.
 *
 * <p>This component acts as the "claim mapping layer": it is the single place
 * that knows how to read roles and groups out of a JWT, making the rest of the
 * authorization logic IDP-agnostic. Claim paths are configurable via
 * {@link OidcProperties}.</p>
 *
 * <p>Role resolution order:
 * <ol>
 *   <li>{@code resource_access.<clientId>.roles} (preferred, client-specific)</li>
 *   <li>{@code realm_access.roles} (fallback, realm-level)</li>
 * </ol>
 * </p>
 *
 * <p>Organization filter: derived from the deepest matching path in the
 * {@code groups} claim that starts with the configured {@code organizationPathPrefix}.
 * "Deepest" means the path with the most {@code /} separators.</p>
 */
@Component
public class JwtClaimsExtractor {

    private final OidcProperties oidcProperties;
    private final AppRoleService appRoleService;

    private final static Logger logger = LoggerFactory.getLogger(JwtClaimsExtractor.class);

    @Autowired
    public JwtClaimsExtractor(OidcProperties oidcProperties, @Lazy AppRoleService appRoleService) {
        this.oidcProperties = oidcProperties;
        this.appRoleService = appRoleService;
    }

    /**
     * Extracts all {@link AppRoleEntity}s from the JWT token by looking them up in the DB.
     * Returns an empty set for anonymous/unauthenticated requests or when roles are not found in DB.
     */
    public Set<AppRoleEntity> extractRoles(Jwt jwt) {
        if (jwt == null) return Collections.emptySet();
        List<String> roleNames = extractRoleNames(jwt);
        Set<AppRoleEntity> roles = new HashSet<>();
        for (String roleName : roleNames) {
            try {
                AppRoleEntity role = appRoleService.getAppRole(roleName);
                if (role != null) {
                    roles.add(role);
                }
            } catch (Exception e) {
                // Skip roles that cannot be loaded (e.g., DB not yet initialized)
                logger.trace("Failed to load role '{}' from DB: {}", roleName, e.getMessage());
            }
        }
        return roles;
    }

    /**
     * Extracts all {@link AppPermissionEntity}s granted to the user by their roles.
     */
    public Set<AppPermissionEntity> extractPermissions(Jwt jwt) {
        return extractRoles(jwt).stream()
                .flatMap(role -> role.getPermissionRefs().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Derives the effective organization filter from the token's {@code groups} claim.
     *
     * <p>Selects the deepest path that starts with the configured
     * {@code organizationPathPrefix}. Deepest = most path segments (most {@code /} chars).
     * Returns the full organization path (without the prefix).
     * Returns {@code null} if no matching group is found.</p>
     */
    public String extractEffectiveOrganization(Jwt jwt) {
        return extractEffectiveOrganizationPath(jwt);
    }

    /**
     * Returns the full organization path (without the prefix) for use in filtering.
     * Unlike {@link #extractEffectiveOrganization}, this returns the full path.
     */
    public String extractEffectiveOrganizationPath(Jwt jwt) {
        if (jwt == null) return null;
        List<String> groups = extractGroups(jwt);
        String prefix = oidcProperties.getOrganizationPathPrefix();
        return groups.stream()
                .filter(g -> g != null && g.startsWith(prefix))
                .max(Comparator.comparingInt(g -> countOccurrences(g, '/')))
                .map(path -> path.substring(prefix.length()))
                .orElse(null);
    }

    // --- Private helpers ---

    @SuppressWarnings("unchecked")
    private List<String> extractRoleNames(Jwt jwt) {
        // 1) Try resource_access.<clientId>.roles
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Object clientEntry = resourceAccess.get(oidcProperties.getClientId());
                if (clientEntry instanceof Map) {
                    Object roles = ((Map<?, ?>) clientEntry).get("roles");
                    if (roles instanceof List) {
                        return (List<String>) roles;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to realm_access
            logger.trace("Failed to extract roles from resource_access claim: {}", ignored.getMessage());
        }
        // 2) Fallback: realm_access.roles
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object roles = realmAccess.get("roles");
                if (roles instanceof List) {
                    return (List<String>) roles;
                }
            }
        } catch (Exception ignored) {
            // No roles found
            logger.trace("Failed to extract roles from realm_access claim: {}", ignored.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractGroups(Jwt jwt) {
        try {
            Object groups = jwt.getClaim(oidcProperties.getGroupsClaim());
            if (groups instanceof List) {
                return (List<String>) groups;
            }
        } catch (Exception ignored) {
            // No groups found
            logger.trace("Failed to extract groups from claim '{}': {}", oidcProperties.getGroupsClaim(), ignored.getMessage());
        }
        return Collections.emptyList();
    }

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) count++;
        }
        return count;
    }
}
