import { test as base } from '@playwright/test';

export const test = base.extend({
  authenticatedPage: async ({ page }, use) => {
    // Mock the OIDC discovery document
    await page.route('**/realms/priceprovider/.well-known/openid-configuration', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          issuer: 'http://localhost:8081/realms/priceprovider',
          authorization_endpoint: 'http://localhost:8081/realms/priceprovider/protocol/openid-connect/auth',
          token_endpoint: 'http://localhost:8081/realms/priceprovider/protocol/openid-connect/token',
          userinfo_endpoint: 'http://localhost:8081/realms/priceprovider/protocol/openid-connect/userinfo',
          jwks_uri: 'http://localhost:8081/realms/priceprovider/protocol/openid-connect/certs',
        }),
      });
    });

    // Mock the session storage to simulate an authenticated state
    // We need to set this before the application initializes the AuthService
    await page.addInitScript(() => {
      const mockToken = 'mock-access-token';
      const mockIdToken = 'mock-id-token';
      const mockClaims = {
        sub: '1234567890',
        preferred_username: 'admin',
        name: 'Admin User',
        email: 'admin@priceprovider.com',
        realm_access: {
          roles: ['admin', 'offline_access', 'uma_authorization']
        },
        resource_access: {
          'priceprovider-app': {
            roles: ['admin']
          }
        }
      };

      // These keys might depend on angular-oauth2-oidc implementation details
      // but usually it uses sessionStorage
      sessionStorage.setItem('access_token', mockToken);
      sessionStorage.setItem('id_token', mockIdToken);
      sessionStorage.setItem('id_token_claims_obj', JSON.stringify(mockClaims));
      sessionStorage.setItem('access_token_stored_at', Date.now().toString());
      sessionStorage.setItem('id_token_stored_at', Date.now().toString());
      sessionStorage.setItem('expires_at', (Date.now() + 3600000).toString()); // 1 hour later
    });

    await use(page);
  },
});

export { expect } from '@playwright/test';
