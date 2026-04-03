package io.commercestacksolutions.priceproviderservice.config.security;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security configuration for JWT-based resource server with RBAC.
 *
 * <p>Access control strategy:
 * <ul>
 *   <li>Public API ({@code /public/api/**}) – open to all (anonymous + authenticated)</li>
 *   <li>Admin API ({@code /admin/api/**}) – requires a valid JWT; individual operations
 *       are further protected by {@code @PreAuthorize} annotations on controllers.</li>
 *   <li>Infrastructure ({@code /actuator/**, /swagger-ui/**, /v3/api-docs/**,
 *       /h2-console/**}) – open without authentication for development tooling.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtClaimsExtractor jwtClaimsExtractor;

    @Value("${priceprovider.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Autowired
    public SecurityConfig(JwtClaimsExtractor jwtClaimsExtractor) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(Collections.singletonList("*"));
                configuration.setExposedHeaders(Arrays.asList("Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                return configuration;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow preflight requests without authentication
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public price API – open to everyone (anonymous + authenticated)
                .requestMatchers("/public/api/**").permitAll()
                // Infrastructure / tooling
                .requestMatchers("/public/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Admin API – require authentication; fine-grained access via @PreAuthorize
                .requestMatchers("/admin/api/**").authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            // Allow H2 console in dev (uses frames)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    /**
     * Converts JWT roles (extracted via {@link JwtClaimsExtractor}) into Spring Security
     * {@link SimpleGrantedAuthority} objects so that {@code @PreAuthorize} expressions
     * like {@code hasAuthority('priceprovider.admin:Channel:read')} work out of the box.
     *
     * <p>Both role names and permission values are registered as authorities so that
     * you can check either level in {@code @PreAuthorize}.</p>
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::convertJwtToAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> convertJwtToAuthorities(Jwt jwt) {
        Set<AppRoleEntity> roles = jwtClaimsExtractor.extractRoles(jwt);

        // Register both roles AND their permissions as Spring Security authorities
        return roles.stream()
                .flatMap(role -> {
                    // The role name (e.g., "priceprovider.admin:Admin") is used as authority
                    String roleName = role.getName();
                    String normalizedRoleName = normalizeId(roleName);
                    java.util.stream.Stream<SimpleGrantedAuthority> roleAuthorities;
                    if (roleName != null && !normalizedRoleName.equals(roleName)) {
                        // register both original and normalized authority for compatibility
                        roleAuthorities = java.util.stream.Stream.of(
                                new SimpleGrantedAuthority(roleName),
                                new SimpleGrantedAuthority(normalizedRoleName)
                        );
                    } else if (roleName != null) {
                        roleAuthorities = java.util.stream.Stream.of(new SimpleGrantedAuthority(roleName));
                    } else {
                        roleAuthorities = java.util.stream.Stream.empty();
                    }

                    // All permissions granted by this role
                    java.util.stream.Stream<SimpleGrantedAuthority> permissionAuthorities =
                            role.getPermissionRefs().stream()
                                    .flatMap(p -> {
                                        String pid = p.getName();
                                        if (pid == null) return java.util.stream.Stream.empty();
                                        String npid = normalizeId(pid);
                                        if (!npid.equals(pid)) {
                                            return java.util.stream.Stream.of(
                                                    new SimpleGrantedAuthority(pid),
                                                    new SimpleGrantedAuthority(npid)
                                            );
                                        }
                                        return java.util.stream.Stream.of(new SimpleGrantedAuthority(pid));
                                    });

                    return java.util.stream.Stream.concat(roleAuthorities, permissionAuthorities);
                })
                .collect(Collectors.toSet());
    }

    private String normalizeId(String id) {
        if (id == null) return null;
        return id.replace('/', ':');
    }
}


