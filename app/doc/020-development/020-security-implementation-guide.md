# Security Implementation Guide (App)

This document provides a technical overview of the OIDC and permission-based security implementation in the Price Provider App.

## Authentication (OIDC)

The application uses **OpenID Connect (OIDC)** with **Authorization Code Flow + PKCE** for authentication.

### AuthService

The `AuthService` handles all authentication-related tasks using the `angular-oauth2-oidc` library.

-   **Login/Logout**: Redirects the user to the IDP (Keycloak) for authentication.
-   **User Profile**: Extracts user information from the JWT ID token.
-   **Role Extraction**: Parses the JWT access token to extract user roles.
-   **Organization Context**: Derives the current organization from the `groups` claim.

### Configuration

OIDC settings are configured in the `environment.ts` files:

```typescript
export const environment = {
  oidc: {
    issuerUri: 'http://localhost:8080/realms/priceprovider',
    clientId: 'priceprovider-app',
    scope: 'openid profile email offline_access',
    requireHttps: false,
  },
  ...
};
```

## Authorization (Permissions)

Authorization is managed via a dedicated `PermissionService` that maps roles to their corresponding permissions.

### PermissionService

The `PermissionService` provides methods to check if a user has a specific permission.

-   **loadPermissions()**: Fetches the permissions for each of the user's roles from the backend.
-   **hasPermission(permission)**: Returns `true` if the user has the specified permission string.
-   **Helper Methods**: Provides `hasReadPermission`, `hasWritePermission`, and `hasDeletePermission` for common data types.

### Usage in Components

Components can use the `PermissionService` to conditionally show/hide UI elements.

```html
@if (permissionService.hasReadPermission('Channel')) {
  <a [routerLink]="['/channels']">Channels</a>
}
```

## HTTP Interceptor

The `AuthInterceptor` automatically adds the OIDC access token to all outgoing HTTP requests to the backend API.

-   **Logic**: If a valid access token is available, it adds the `Authorization: Bearer <token>` header to the request.
-   **Handling Exclusions**: Public requests (e.g., to the Public Price API) are sent without a token when the user is not authenticated.

## Organization Filter

The app extracts the user's organization from the OIDC `groups` claim and uses it as a global context. This context is used to filter data in the Public Price API, ensuring the user only sees prices for their specific organization.

The effective organization is derived from the deepest matching path under `/organizations/`.
