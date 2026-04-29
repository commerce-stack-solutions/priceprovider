package io.commercestacksolutions.priceproviderservice.config.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

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

        // If we can't determine the context, throw an exception
        // This shouldn't happen in normal operation, but it's better to fail explicitly
        throw new IllegalStateException("Unable to determine API context - request path does not match admin or public API patterns");
    }

    /**
     * Checks if the current context is the admin API.
     *
     * @return true if the current request is targeting the admin API
     */
    public boolean isAdminContext() {
        return ADMIN_PREFIX.equals(getCurrentPermissionPrefix());
    }

    /**
     * Checks if the current context is the public API.
     *
     * @return true if the current request is targeting the public API
     */
    public boolean isPublicContext() {
        return PUBLIC_PREFIX.equals(getCurrentPermissionPrefix());
    }
}
