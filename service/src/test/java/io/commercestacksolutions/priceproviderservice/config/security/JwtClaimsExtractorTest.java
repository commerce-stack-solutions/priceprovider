package io.commercestacksolutions.priceproviderservice.config.security;

import io.commercestacksolutions.priceproviderservice.service.approle.AppRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtClaimsExtractorTest {

    @Mock
    private OidcProperties oidcProperties;

    @Mock
    private AppRoleService appRoleService;

    @Mock
    private Jwt jwt;

    private JwtClaimsExtractor jwtClaimsExtractor;

    @BeforeEach
    public void setup() {
        jwtClaimsExtractor = new JwtClaimsExtractor(oidcProperties, appRoleService);
    }

    @Test
    public void testExtractEffectiveOrganization_ReturnFullPath() {
        when(oidcProperties.getGroupsClaim()).thenReturn("groups");
        when(oidcProperties.getOrganizationPathPrefix()).thenReturn("/organizations/");

        String groupPath = "/organizations/ORG-CITY-COUNCIL/ORG-CITY-HEALTH";
        when(jwt.getClaim("groups")).thenReturn(List.of(groupPath));

        String result = jwtClaimsExtractor.extractEffectiveOrganization(jwt);

        assertEquals("ORG-CITY-COUNCIL/ORG-CITY-HEALTH", result);
    }

    @Test
    public void testExtractEffectiveOrganization_DeepestPathWins() {
        when(oidcProperties.getGroupsClaim()).thenReturn("groups");
        when(oidcProperties.getOrganizationPathPrefix()).thenReturn("/organizations/");

        String path1 = "/organizations/ORG-MY-COMPANY";
        String path2 = "/organizations/ORG-MY-COMPANY/ORG-TECHCORP-US";
        when(jwt.getClaim("groups")).thenReturn(List.of(path1, path2));

        String result = jwtClaimsExtractor.extractEffectiveOrganization(jwt);

        assertEquals("ORG-MY-COMPANY/ORG-TECHCORP-US", result);
    }

    @Test
    public void testExtractEffectiveOrganization_NoMatchingPrefix_ReturnsNull() {
        when(oidcProperties.getGroupsClaim()).thenReturn("groups");
        when(oidcProperties.getOrganizationPathPrefix()).thenReturn("/organizations/");

        when(jwt.getClaim("groups")).thenReturn(List.of("/other/prefix/GROUP"));

        String result = jwtClaimsExtractor.extractEffectiveOrganization(jwt);

        assertNull(result);
    }

    @Test
    public void testExtractEffectiveOrganization_NullJwt_ReturnsNull() {
        assertNull(jwtClaimsExtractor.extractEffectiveOrganization(null));
    }
}
