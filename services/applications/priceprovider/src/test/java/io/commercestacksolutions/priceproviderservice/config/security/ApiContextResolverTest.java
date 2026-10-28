package io.commercestacksolutions.priceproviderservice.config.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiContextResolverTest {

    private final ApiContextResolver resolver = new ApiContextResolver();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentPermissionPrefix_returnsAdminForAdminRequestPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/api/entities");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("priceprovider.admin", resolver.getCurrentPermissionPrefix());
    }

    @Test
    void getCurrentPermissionPrefix_returnsPublicForPublicRequestPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/public/api/entities");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("priceprovider.public", resolver.getCurrentPermissionPrefix());
    }

    @Test
    void getCurrentPermissionPrefix_fallsBackToAdminAuthorityWhenNoRequestContext() {
        var auth = new UsernamePasswordAuthenticationToken(
                "test-admin",
                "test",
                AuthorityUtils.createAuthorityList("priceprovider.admin:Language:write")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("priceprovider.admin", resolver.getCurrentPermissionPrefix());
    }

    @Test
    void getCurrentPermissionPrefix_fallsBackToPublicAuthorityWhenNoRequestContext() {
        var auth = new UsernamePasswordAuthenticationToken(
                "test-public",
                "test",
                AuthorityUtils.createAuthorityList("priceprovider.public:PriceRow:read")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("priceprovider.public", resolver.getCurrentPermissionPrefix());
    }

    @Test
    void getCurrentPermissionPrefix_throwsWhenNeitherRequestNorAuthorityProvidesContext() {
        assertThrows(IllegalStateException.class, resolver::getCurrentPermissionPrefix);
    }
}

