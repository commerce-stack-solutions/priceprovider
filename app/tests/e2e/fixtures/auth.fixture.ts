import { test as base } from '@playwright/test';

export const test = base.extend({
  authenticatedPage: async ({ page }, use) => {
    const mockState = 'VE5xOC0wTHJzZVJUWnExcG5GREtkdjhaYmdiSUktRlNWWk4zWWVOckZNQkE1';
    const mockSessionState = '0d942570-23ac-4b95-99fd-9fe332205538';
    const mockCode = 'dd742aea-ef61-4792-8296-28f42c812972.0d942570-23ac-4b95-99fd-9fe332205538.6db69f12-ec7d-4fda-a7a8-aedef955996f';
    const issuer = 'http://localhost:8081/realms/priceprovider';

    // Create base claims for the user
    const mockClaims = {
      exp: Math.floor(Date.now() / 1000) + 3600,
      iat: Math.floor(Date.now() / 1000),
      auth_time: Math.floor(Date.now() / 1000) - 1,
      jti: '80c79fd1-6bd8-4eae-9143-1a377ec3c02f',
      iss: issuer,
      sub: 'af9ae619-a751-4c4b-9350-1f1e4919353f',
      typ: 'Bearer',
      azp: 'priceprovider-app',
      nonce: 'mock-nonce',
      session_state: mockSessionState,
      acr: '1',
      'allowed-origins': ['http://localhost:80', 'http://localhost', 'http://localhost:4200'],
      realm_access: {
        roles: ['priceprovider.admin:Admin']
      },
      resource_access: {
        'priceprovider-app': {
          roles: ['priceprovider.admin:Admin']
        }
      },
      scope: 'openid email profile',
      sid: mockSessionState,
      email_verified: true,
      name: 'Admin User',
      preferred_username: 'admin-user',
      given_name: 'Admin',
      family_name: 'User',
      email: 'admin@priceprovider.local',
      groups: ['/organizations/ORG-TECHCORP-EU']
    };

    const toBase64 = (obj: any) => btoa(JSON.stringify(obj))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');

    const header = toBase64({ alg: 'RS256', typ: 'JWT', kid: 'XqmVt5FiakF6FFffKuZBQGBynLc7DMyYIVXtrQbKhAI' });
    const payload = toBase64(mockClaims);
    const mockAccessToken = `${header}.${payload}.signature`;
    const mockIdToken = `${header}.${payload}.signature`;

    // 1. Mock OIDC Discovery
    await page.route('**/realms/priceprovider/.well-known/openid-configuration', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          issuer: issuer,
          authorization_endpoint: `${issuer}/protocol/openid-connect/auth`,
          token_endpoint: `${issuer}/protocol/openid-connect/token`,
          userinfo_endpoint: `${issuer}/protocol/openid-connect/userinfo`,
          jwks_uri: `${issuer}/protocol/openid-connect/certs`,
          response_types_supported: ['code', 'id_token', 'token id_token'],
          subject_types_supported: ['public'],
          id_token_signing_alg_values_supported: ['RS256']
        }),
      });
    });

    // 2. Mock Token Endpoint (MATCHING USER EXAMPLE)
    await page.route('**/protocol/openid-connect/token', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          access_token: mockAccessToken,
          expires_in: 300,
          refresh_expires_in: 1800,
          refresh_token: 'mock-refresh-token',
          token_type: 'Bearer',
          id_token: mockIdToken,
          'not-before-policy': 0,
          session_state: mockSessionState,
          scope: 'openid email profile'
        }),
      });
    });

    // 3. Mock UserInfo
    await page.route('**/protocol/openid-connect/userinfo', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockClaims),
      });
    });

    // 4. Mock Permissions
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

    // 5. Pre-set sessionStorage for Code Flow
    await page.addInitScript(({ state }) => {
      // Keys used by angular-oauth2-oidc
      sessionStorage.setItem('state', state);
      sessionStorage.setItem('nonce', 'mock-nonce');
      sessionStorage.setItem('PKCE_verifier', 'mock-pkce-verifier');
    }, { state: mockState });

    // 6. Navigate to callback URL structure provided by user
    await page.goto(`/?state=${mockState}&session_state=${mockSessionState}&iss=${encodeURIComponent(issuer)}&code=${mockCode}`);

    // Wait for the URL to be cleaned and app to stabilize
    await page.waitForURL(url => !url.searchParams.has('code'), { timeout: 10000 });

    await use(page);
  },
});

export { expect } from '@playwright/test';
