package io.commercestacksolutions.priceproviderservice.config.security;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;

/**
 * Test-only AuthorizationContext that extracts permissions from Spring Security authorities.
 *
 * <p>This component is used in integration tests to allow authorization checks to work
 * without a full JWT/OIDC infrastructure. It extracts permissions from the
 * {@link org.springframework.security.core.GrantedAuthority} authorities set by
 * {@link io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig}.
 *
 * <p>Import this configuration in test classes alongside {@link io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig}:</p>
 * <pre>
 *   {@code @Import({TestSecurityConfig.class, TestAuthorizationContext.class})}
 * </pre>
 *
 * <p>This test component takes precedence over the production {@link AuthorizationContext}
 * when both are in the application context due to Spring's component scanning ordering.</p>
 */
@TestComponent
public class TestAuthorizationContext extends AuthorizationContext {

    public TestAuthorizationContext() {
        // No dependencies needed for test context
        super(null, null);
    }

    /**
     * Returns the current user's permissions extracted from Spring Security authorities.
     *
     * <p>This method extracts permission strings from the authorities set up by
     * {@link io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig}
     * and converts them to {@link AppPermissionEntity} objects.
     *
     * @return set of permissions from Spring Security authorities
     */
    @Override
    public Set<AppPermissionEntity> getCurrentPermissions() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Set.of();
        }

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

    /**
     * Returns null in test environment (organization filtering not needed for tests).
     *
     * @return null
     */
    @Override
    public String getCurrentOrganization() {
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
