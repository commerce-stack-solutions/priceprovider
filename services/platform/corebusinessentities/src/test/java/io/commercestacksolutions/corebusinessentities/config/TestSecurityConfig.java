package io.commercestacksolutions.corebusinessentities.config;

import io.commercestacksolutions.commons.config.security.AuthorizationContext;
import io.commercestacksolutions.commons.dataaccess.approle.entity.CommonAppPermission;
import jakarta.servlet.Filter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Set;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public AuthorizationContext testAuthorizationContext() {
        return new AuthorizationContext() {
            @Override
            public Set<? extends CommonAppPermission> getCurrentPermissions() {
                return Set.of();
            }

            @Override
            public boolean isBootstrapModeEnabled() {
                return false;
            }

            @Override
            public void activateBootstrapMode() {
            }

            @Override
            public void deactivateBootstrapMode() {
            }
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(testAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private Filter testAuthenticationFilter() {
        return (request, response, chain) -> {
            var authorities = AuthorityUtils.createAuthorityList(
                "ROLE_ADMIN",
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
                "priceprovider.admin:Organization:read",
                "priceprovider.admin:Organization:write",
                "priceprovider.admin:Organization:delete",
                "priceprovider.admin:TaxClass:read",
                "priceprovider.admin:TaxClass:write",
                "priceprovider.admin:TaxClass:delete",
                "priceprovider.admin:Unit:read",
                "priceprovider.admin:Unit:write",
                "priceprovider.admin:Unit:delete"
            );
            var auth = new UsernamePasswordAuthenticationToken("test-admin", "test", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        };
    }
}
