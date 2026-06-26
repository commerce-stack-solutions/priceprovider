import { test as base } from '@playwright/test';

export const test = base.extend({
  authenticatedPage: async ({ page }, use) => {
    const mockState = 'VE5xOC0wTHJzZVJUWnExcG5GREtkdjhaYmdiSUktRlNWWk4zWWVOckZNQkE1';
    const mockCode = 'mock-code';
    const mockClaims = {
      sub: '1234567890',
      preferred_username: 'admin-user',
      name: 'Admin User',
      email: 'admin@priceprovider.local',
      realm_access: {
        roles: ['priceprovider.admin:Admin', 'offline_access', 'uma_authorization']
      },
      resource_access: {
        'priceprovider-app': {
          roles: ['priceprovider.admin:Admin']
        }
      },
      groups: ['/organizations/ORG-TECHCORP-EU']
    };

    const toBase64 = (obj: any) => btoa(JSON.stringify(obj))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');

    const header = toBase64({ alg: 'RS256', typ: 'JWT' });
    const payload = toBase64(mockClaims);
    const mockAccessToken = `${header}.${payload}.signature`;
    const mockIdToken = `${header}.${payload}.signature`;

    // 1. Mock OIDC Discovery Document
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

    // 2. Mock Token Endpoint (used in Code Flow)
    await page.route('**/protocol/openid-connect/token', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          access_token: mockAccessToken,
          id_token: mockIdToken,
          token_type: 'Bearer',
          expires_in: 3600,
          scope: 'openid profile email'
        }),
      });
    });

    // 3. Mock UserInfo Endpoint
    await page.route('**/protocol/openid-connect/userinfo', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockClaims),
      });
    });

    // 4. Mock Permissions API
    await page.route('**/admin/api/app-roles/by-name/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          name: 'priceprovider.admin:Admin',
          permissionRefs: [
            'priceprovider.admin:PriceRow:read',
            'priceprovider.admin:PriceRow:write',
            'priceprovider.admin:PriceRow:delete',
            'priceprovider.admin:Channel:read',
            'priceprovider.admin:Unit:read',
            'priceprovider.admin:Currency:read',
            'priceprovider.admin:TaxClass:read'
          ]
        }),
      });
    });

    // 5. Pre-set sessionStorage for the Code Flow callback
    // angular-oauth2-oidc expects state and PKCE verifier to be present
    await page.addInitScript(({ state }) => {
      sessionStorage.setItem('state', state);
      sessionStorage.setItem('nonce', 'mock-nonce');
      sessionStorage.setItem('PKCE_verifier', 'mock-pkce-verifier');
    }, { state: mockState });

    // 6. Navigate to the callback URL
    // This will trigger angular-oauth2-oidc's loadDiscoveryDocumentAndTryLogin()
    await page.goto(`/?code=${mockCode}&state=${mockState}&iss=http%3A%2F%2Flocalhost%3A8081%2Frealms%2Fpriceprovider`);

    await use(page);
  },
});

export { expect } from '@playwright/test';
