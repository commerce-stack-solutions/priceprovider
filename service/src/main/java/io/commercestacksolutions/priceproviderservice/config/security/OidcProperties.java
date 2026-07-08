package io.commercestacksolutions.priceproviderservice.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable claim-path mapping so that switching IDPs does not require
 * rewriting authorization logic.
 *
 * <p>Configured via application.yaml under the {@code priceprovider.oidc} prefix.</p>
 */
@Component
@ConfigurationProperties(prefix = "priceprovider.oidc")
public class OidcProperties {

    /**
     * Path to the client-specific roles array in the JWT.
     * Default is the standard Keycloak resource_access pattern.
     * Use {@code {clientId}} as a placeholder for the OAuth2 client ID.
     */
    private String resourceAccessRolesPath = "resource_access.{clientId}.roles";

    /**
     * Path to the realm-level roles array in the JWT (fallback when client roles not found).
     */
    private String realmRolesPath = "realm_access.roles";

    /**
     * Claim name that holds the user's group paths (e.g. Keycloak {@code groups}).
     * Used to derive the effective organization filter.
     */
    private String groupsClaim = "groups";

    /**
     * Organization path prefix used to identify organization groups.
     * Groups starting with this prefix are considered organization assignments.
     */
    private String organizationPathPrefix = "/organizations/";

    /**
     * The OAuth2 client ID registered in the IDP for this service.
     * Used to look up client-specific roles in {@code resource_access}.
     */
    private String clientId = "priceprovider-service";

    public String getResourceAccessRolesPath() {
        return resourceAccessRolesPath;
    }

    public void setResourceAccessRolesPath(String resourceAccessRolesPath) {
        this.resourceAccessRolesPath = resourceAccessRolesPath;
    }

    public String getRealmRolesPath() {
        return realmRolesPath;
    }

    public void setRealmRolesPath(String realmRolesPath) {
        this.realmRolesPath = realmRolesPath;
    }

    public String getGroupsClaim() {
        return groupsClaim;
    }

    public void setGroupsClaim(String groupsClaim) {
        this.groupsClaim = groupsClaim;
    }

    public String getOrganizationPathPrefix() {
        return organizationPathPrefix;
    }

    public void setOrganizationPathPrefix(String organizationPathPrefix) {
        this.organizationPathPrefix = organizationPathPrefix;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
