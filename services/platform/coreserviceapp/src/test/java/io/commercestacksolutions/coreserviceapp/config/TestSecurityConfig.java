package io.commercestacksolutions.coreserviceapp.config;

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
                "priceprovider.admin:AppPermission:read",
                "priceprovider.admin:AppPermission:write",
                "priceprovider.admin:AppPermission:delete",
                "priceprovider.admin:AppRole:read",
                "priceprovider.admin:AppRole:write",
                "priceprovider.admin:AppRole:delete"
            );
            var auth = new UsernamePasswordAuthenticationToken("test-admin", "test", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        };
    }
}
