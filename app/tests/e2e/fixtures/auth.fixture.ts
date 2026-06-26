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

    // Mock the permissions API call from PermissionService
    await page.route('**/admin/api/app-roles/by-name/**', async (route) => {
      const url = route.request().url();
      const roleName = decodeURIComponent(url.split('/').pop() || '');

      let permissions: string[] = [];
      if (roleName === 'priceprovider.admin:Admin' || roleName === 'priceprovider.admin:Superuser') {
        permissions = [
          'priceprovider.admin:PriceRow:read',
          'priceprovider.admin:PriceRow:write',
          'priceprovider.admin:PriceRow:delete',
          'priceprovider.admin:Channel:read',
          'priceprovider.admin:Unit:read',
          'priceprovider.admin:Currency:read',
          'priceprovider.admin:TaxClass:read',
          'priceprovider.admin:Group:read',
          'priceprovider.admin:Organization:read',
          'priceprovider.admin:Country:read',
          'priceprovider.admin:Language:read',
          'priceprovider.admin:AppRole:read',
          'priceprovider.admin:AppPermission:read'
        ];
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          name: roleName,
          permissionRefs: permissions
        }),
      });
    });

    // Mock the session storage to simulate an authenticated state
    await page.addInitScript(() => {
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
      const mockToken = `${header}.${payload}.signature`;

      // keys used by angular-oauth2-oidc
      const storage = sessionStorage; // can also try localStorage if needed
      storage.setItem('access_token', mockToken);
      storage.setItem('id_token', mockToken);
      storage.setItem('id_token_claims_obj', JSON.stringify(mockClaims));
      storage.setItem('access_token_stored_at', Date.now().toString());
      storage.setItem('id_token_stored_at', Date.now().toString());
      storage.setItem('expires_at', (Date.now() + 3600000).toString());
      storage.setItem('granted_scopes', JSON.stringify(['openid', 'profile', 'email']));

      // Also set in localStorage just in case
      localStorage.setItem('access_token', mockToken);
      localStorage.setItem('id_token', mockToken);
      localStorage.setItem('id_token_claims_obj', JSON.stringify(mockClaims));
      localStorage.setItem('expires_at', (Date.now() + 3600000).toString());
    });

    // Navigate to establish domain
    await page.goto('/');

    await use(page);
  },
});

export { expect } from '@playwright/test';
