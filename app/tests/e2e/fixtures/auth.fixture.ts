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
    await page.addInitScript(() => {
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
          },
          'priceproviderservice': {
            roles: ['admin']
          }
        },
        groups: ['/organizations/main-dept']
      };

      // Helper to generate a b64 token part
      const toBase64 = (obj: any) => btoa(JSON.stringify(obj)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
      const mockToken = `header.${toBase64(mockClaims)}.signature`;

      // These keys must match what angular-oauth2-oidc uses (default is sessionStorage)
      sessionStorage.setItem('access_token', mockToken);
      sessionStorage.setItem('id_token', mockToken);
      sessionStorage.setItem('id_token_claims_obj', JSON.stringify(mockClaims));
      sessionStorage.setItem('access_token_stored_at', Date.now().toString());
      sessionStorage.setItem('id_token_stored_at', Date.now().toString());
      sessionStorage.setItem('expires_at', (Date.now() + 3600000).toString()); // 1 hour later
      sessionStorage.setItem('granted_scopes', JSON.stringify(['openid', 'profile', 'email']));
    });

    // Navigate to a blank page first to ensure sessionStorage is set for the domain
    await page.goto('/');

    await use(page);
  },
});

export { expect } from '@playwright/test';
