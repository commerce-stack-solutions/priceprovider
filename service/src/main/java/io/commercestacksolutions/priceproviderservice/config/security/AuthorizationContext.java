package io.commercestacksolutions.priceproviderservice.config.security;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.service.approle.AppRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides access to the current security context for authorization decisions.
 *
 * <p>This component extracts:
 * <ul>
 *   <li>Current user's permissions (from JWT claims via JwtClaimsExtractor)</li>
 *   <li>Current user's organization filter (for org-scoped data access)</li>
 *   <li>Authentication status</li>
 *   <li>Anonymous user permissions (for unauthenticated requests)</li>
 * </ul>
 */
@Component
public class AuthorizationContext {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationContext.class);
    private static final String ANONYMOUS_USER_ROLE_NAME = "priceprovider.public:AnonymousUser";

    private final JwtClaimsExtractor jwtClaimsExtractor;
    private final AppRoleService appRoleService;

    public AuthorizationContext(JwtClaimsExtractor jwtClaimsExtractor, @Lazy AppRoleService appRoleService) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
        this.appRoleService = appRoleService;
    }

    /**
     * Returns the current user's permissions extracted from the JWT token, Spring Security authorities,
     * or the AnonymousUser role for unauthenticated requests.
     *
     * <p>Fallback mechanism:
     * <ol>
     *   <li>JWT claims (production) - extracts permissions from JWT token</li>
     *   <li>Spring Security authorities (test environments) - allows tests to work without JWT infrastructure</li>
     *   <li>AnonymousUser role (unauthenticated) - returns permissions from the AnonymousUser role</li>
     * </ol>
     *
     * @return set of permissions
     */
    public Set<AppPermissionEntity> getCurrentPermissions() {
        // First try JWT-based permissions (production)
        Jwt jwt = getCurrentJwt();
        if (jwt != null) {
            return jwtClaimsExtractor.extractPermissions(jwt);
        }

        // Fallback to Spring Security authorities (test environments only)
        // This allows integration tests to work without a full JWT/OIDC infrastructure.
        // In production, this code path is only reached for anonymous requests (see below).
        Authentication auth = getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Set<AppPermissionEntity> permissions = new HashSet<>();
            for (org.springframework.security.core.GrantedAuthority authority : auth.getAuthorities()) {
                String authorityString = authority.getAuthority();
                // Filter out ROLE_ authorities, only keep permission strings
                if (!authorityString.startsWith("ROLE_")) {
                    AppPermissionEntity permission = new AppPermissionEntity();
                    permission.setName(authorityString);
                    permissions.add(permission);
                }
            }
            return permissions;
        }

        // For anonymous/unauthenticated requests, return AnonymousUser permissions
        return getAnonymousUserPermissions();
    }

    /**
     * Returns the permissions granted to anonymous users.
     *
     * @return set of permissions from the AnonymousUser role
     */
    private Set<AppPermissionEntity> getAnonymousUserPermissions() {
        try {
            AppRoleEntity anonymousRole = appRoleService.getAppRoleWithPermissionsByName(ANONYMOUS_USER_ROLE_NAME);
            if (anonymousRole != null && anonymousRole.getPermissionRefs() != null) {
                logger.debug("Assigning AnonymousUser role permissions to unauthenticated request");
                return new HashSet<>(anonymousRole.getPermissionRefs());
            }
        } catch (Exception e) {
            logger.debug("AnonymousUser role not found or could not be loaded: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    /**
     * Returns the current user's effective organization path for filtering.
     *
     * @return organization path, or null if not set
     */
    public String getCurrentOrganization() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }
        return jwtClaimsExtractor.extractEffectiveOrganization(jwt);
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    /**
     * Returns the current JWT token, or null if not authenticated or not a JWT-based authentication.
     *
     * @return JWT token or null
     */
    public Jwt getCurrentJwt() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt) {
            return (Jwt) principal;
        }

        return null;
    }

    private Authentication getAuthentication() {
        try {
            return SecurityContextHolder.getContext().getAuthentication();
        } catch (Exception e) {
            return null;
        }
    }
}
