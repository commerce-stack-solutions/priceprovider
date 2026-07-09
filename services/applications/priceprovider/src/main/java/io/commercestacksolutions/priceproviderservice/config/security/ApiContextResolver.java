package io.commercestacksolutions.priceproviderservice.config.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * Resolves the current API context (admin vs public) based on the request path.
 *
 * <p>This component examines the incoming HTTP request to determine whether it's targeting
 * the admin API (/admin/api/*) or the public API (/public/api/*), and returns the appropriate
 * permission prefix to use.
 *
 * <p>Used by permission checking services to ensure that:
 * <ul>
 *   <li>Admin API endpoints only accept priceprovider.admin:* permissions</li>
 *   <li>Public API endpoints only accept priceprovider.public:* permissions</li>
 * </ul>
 */
@Component
public class ApiContextResolver {

    private static final String ADMIN_PREFIX = "priceprovider.admin";
    private static final String PUBLIC_PREFIX = "priceprovider.public";
    private static final String ADMIN_PATH_PREFIX = "/admin/api";
    private static final String PUBLIC_PATH_PREFIX = "/public/api";

    /**
     * Gets the appropriate permission prefix for the current request context.
     *
     * @return "priceprovider.admin" for admin API requests, "priceprovider.public" for public API requests
     * @throws IllegalStateException if the API context cannot be determined from the request
     */
    public String getCurrentPermissionPrefix() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String requestPath = request.getRequestURI();

            if (requestPath.startsWith(ADMIN_PATH_PREFIX)) {
                return ADMIN_PREFIX;
            } else if (requestPath.startsWith(PUBLIC_PATH_PREFIX)) {
                return PUBLIC_PREFIX;
            }
        }

        String permissionPrefix = resolvePrefixFromAuthentication();
        if (permissionPrefix != null) {
            return permissionPrefix;
        }

        // If we can't determine the context, throw an exception
        // This shouldn't happen in normal operation, but it's better to fail explicitly
        throw new IllegalStateException("Unable to determine API context - request path does not match admin or public API patterns");
    }

    private String resolvePrefixFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = authentication.getAuthorities();
        boolean hasAdminAuthority = false;
        boolean hasPublicAuthority = false;

        for (org.springframework.security.core.GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (name == null) {
                continue;
            }
            if (name.startsWith(ADMIN_PREFIX + ":")) {
                hasAdminAuthority = true;
            } else if (name.startsWith(PUBLIC_PREFIX + ":")) {
                hasPublicAuthority = true;
            }
        }

        if (hasAdminAuthority) {
            return ADMIN_PREFIX;
        }
        if (hasPublicAuthority) {
            return PUBLIC_PREFIX;
        }
        return null;
    }

}
