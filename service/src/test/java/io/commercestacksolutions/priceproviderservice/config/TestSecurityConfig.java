package io.commercestacksolutions.priceproviderservice.config;

import jakarta.servlet.Filter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Test-only security configuration that disables all security constraints.
 *
 * <p>Import this configuration in test classes that use {@link MockMvc} and should
 * not be blocked by Spring Security:</p>
 * <pre>
 *   {@code @Import(TestSecurityConfig.class)}
 * </pre>
 *
 * <p>Sets up a fully-authenticated test user with all admin permissions so that
 * both HTTP-level and method-level {@code @PreAuthorize} checks pass in integration tests.</p>
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(testAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Filter that sets up a fully-authenticated test admin in the SecurityContextHolder
     * for every request, so that {@code @PreAuthorize} method-level checks pass.
     */
    private Filter testAuthenticationFilter() {
        return (request, response, chain) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                var authorities = AuthorityUtils.createAuthorityList(
                    "ROLE_ADMIN",
                    "priceprovider.admin:AppPermission:read",
                    "priceprovider.admin:AppPermission:write",
                    "priceprovider.admin:AppPermission:delete",
                    "priceprovider.admin:AppRole:read",
                    "priceprovider.admin:AppRole:write",
                    "priceprovider.admin:AppRole:delete",
                    "priceprovider.admin:Channel:read",
                    "priceprovider.admin:Channel:write",
                    "priceprovider.admin:Channel:delete",
                    "priceprovider.admin:Country:read",
                    "priceprovider.admin:Country:write",
                    "priceprovider.admin:Country:delete",
                    "priceprovider.admin:Currency:read",
                    "priceprovider.admin:Currency:write",
                    "priceprovider.admin:Currency:delete",
                    "priceprovider.admin:Group:read",
                    "priceprovider.admin:Group:write",
                    "priceprovider.admin:Group:delete",
                    "priceprovider.admin:Language:read",
                    "priceprovider.admin:Language:write",
                    "priceprovider.admin:Language:delete",
                    "priceprovider.admin:Organization:read",
                    "priceprovider.admin:Organization:write",
                    "priceprovider.admin:Organization:delete",
                    "priceprovider.admin:PriceRow:read",
                    "priceprovider.admin:PriceRow:write",
                    "priceprovider.admin:PriceRow:delete",
                    "priceprovider.admin:TaxClass:read",
                    "priceprovider.admin:TaxClass:write",
                    "priceprovider.admin:TaxClass:delete",
                    "priceprovider.admin:Unit:read",
                    "priceprovider.admin:Unit:write",
                    "priceprovider.admin:Unit:delete",
                    "priceprovider.public:PriceRow:read"
                );
                var auth = new UsernamePasswordAuthenticationToken("test-admin", "test", authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(request, response);
        };
    }
}



