package io.commercestacksolutions.priceproviderservice.config.security;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Provides access to the current security context for authorization decisions.
 *
 * <p>This component extracts:
 * <ul>
 *   <li>Current user's permissions (from JWT claims via JwtClaimsExtractor)</li>
 *   <li>Current user's organization filter (for org-scoped data access)</li>
 *   <li>Authentication status</li>
 * </ul>
 */
@Component
public class AuthorizationContext {

    private final JwtClaimsExtractor jwtClaimsExtractor;

    public AuthorizationContext(JwtClaimsExtractor jwtClaimsExtractor) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    /**
     * Returns the current user's permissions extracted from the JWT token.
     *
     * @return set of permissions, or empty set if not authenticated
     */
    public Set<AppPermissionEntity> getCurrentPermissions() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return Collections.emptySet();
        }
        return jwtClaimsExtractor.extractPermissions(jwt);
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
