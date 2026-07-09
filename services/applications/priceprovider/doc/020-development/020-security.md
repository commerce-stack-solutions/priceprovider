# Security Implementation

This document describes the implemented security concepts of the Price Provider Service, focusing on OIDC and RBAC.

## Architecture Overview

The Price Provider Service uses **OpenID Connect (OIDC)** for authentication and a fine-grained **Role-Based Access Control (RBAC)** system for authorization. Keycloak is used as the default Identity Provider (IDP).

### Admin API (RBAC-Protected)

All admin endpoints under `/admin/api/**` are protected using Spring Security's `@PreAuthorize` annotations. Access is granted based on **AppPermissions**, which are grouped into **AppRoles**.

### Public Price API (Organization-Scoped)

Endpoints under `/public/api/**` are open but can be filtered by organization. The organization context is derived from the OIDC `groups` claim.

## Keycloak Integration

The service acts as an OAuth2 Resource Server. It validates JWT access tokens issued by Keycloak.

### Connection Configuration

The OIDC connection is configured in `application.yaml` (and can be overridden via environment variables):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/priceprovider}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8080/realms/priceprovider/protocol/openid-connect/certs}

priceprovider:
  oidc:
    clientId: priceprovider-service
    groupsClaim: groups
    organizationPathPrefix: /organizations/
```

## Role-Based Access Control (RBAC)

The RBAC system maps IDP roles to application-specific permissions.

### AppRole and AppPermission

- **AppPermission**: Represents a specific action on a data type (e.g., `priceprovider.admin:Channel:read`).
- **AppRole**: A collection of permissions (e.g., `priceprovider.admin:Admin`).

These are initialized from JSON files in `service/src/main/resources/initialize/essential/`.

### Claim Mapping

The `JwtClaimsExtractor` component is responsible for:
1.  **Role Extraction**: Reading roles from the JWT (`resource_access.<clientId>.roles` or `realm_access.roles`).
2.  **Permission Resolution**: Looking up the corresponding `AppRole` in the database to get its permissions.
3.  **Authority Registration**: Registering both roles and permissions as Spring Security `GrantedAuthority` objects.

### Usage in Controllers

Controllers use `@PreAuthorize` to enforce access control:

```java
@RestController
@RequestMapping("/admin/api/channels")
public class ChannelController {

    @GetMapping
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:read')")
    public RestResponse<ChannelRestEntity> list(...) { ... }
}
```

## Organization Context

The user's organization is extracted from the `groups` claim in the JWT. The `JwtClaimsExtractor` looks for groups starting with `/organizations/` and determines the "deepest" matching group as the effective organization.

This context is used to filter data in the Public Price API, ensuring users only see prices relevant to their organization.

## CORS Configuration

CORS is configured in `SecurityConfig` and can be adjusted via `priceprovider.cors.allowed-origins`.

```java
@Value("${priceprovider.cors.allowed-origins:*}")
private String[] allowedOrigins;
```

By default, it allows all origins in development, but should be restricted in production.
