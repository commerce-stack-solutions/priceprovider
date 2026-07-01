import { test as base } from '@playwright/test';

export const test = base.extend({
  authenticatedPage: async ({ page }, use) => {
    const mockNonce = '67d81a95-7e8e-4a8a-9a9a-67d81a957e8e';
    const issuer = 'http://localhost:8081/realms/priceprovider';

    const mockClaims = {
      exp: Math.floor(Date.now() / 1000) + 3600,
      iat: Math.floor(Date.now() / 1000),
      auth_time: Math.floor(Date.now() / 1000) - 1,
      jti: '80c79fd1-6bd8-4eae-9143-1a377ec3c02f',
      iss: issuer,
      aud: 'priceprovider-app',
      sub: 'af9ae619-a751-4c4b-9350-1f1e4919353f',
      typ: 'Bearer',
      azp: 'priceprovider-app',
      nonce: mockNonce,
      session_state: '0d942570-23ac-4b95-99fd-9fe332205538',
      realm_access: {
        roles: ['priceprovider.admin:Admin']
      },
      resource_access: {
        'priceprovider-app': {
          roles: ['priceprovider.admin:Admin']
        }
      },
      scope: 'openid email profile',
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
    const mockToken = `${header}.${payload}.signature`;

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
          id_token_signing_alg_values_supported: ['RS256']
        }),
      });
    });

    // 2. Mock JWKS
    await page.route('**/protocol/openid-connect/certs', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          keys: [{
            kid: 'XqmVt5FiakF6FFffKuZBQGBynLc7DMyYIVXtrQbKhAI',
            kty: 'RSA',
            alg: 'RS256',
            use: 'sig',
            n: 'mock-n',
            e: 'AQAB'
          }]
        }),
      });
    });

    // 3. Mock Permissions API (common for all authenticated tests)
    await page.route('**/admin/api/app-roles/by-name/**', async (route) => {
      const url = route.request().url();
      const roleName = decodeURIComponent(url.substring(url.lastIndexOf('/') + 1)).split('?')[0];
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          name: roleName,
          permissionRefs: [
            'priceprovider.admin:PriceRow:read',
            'priceprovider.admin:PriceRow:write',
            'priceprovider.admin:PriceRow:delete',
            'priceprovider.admin:Channel:read',
            'priceprovider.admin:Unit:read',
            'priceprovider.admin:Currency:read',
            'priceprovider.admin:TaxClass:read',
            'priceprovider.admin:AppRole:read'
          ]
        }),
      });
    });

    // 4. Mock Transloco (Languages)
    await page.route('**/assets/i18n/**/*.json', async (route) => {
      const url = route.request().url();
      const part = url.split('/').slice(-1)[0];
      let body = {};
      if (part === 'common.json') {
        body = {
          auth: { logout: 'Logout', login: 'Login', noAdminAccess: 'No Admin Access', loginRequired: 'Login Required' },
          actions: { edit: 'Edit', delete: 'Delete', save: 'Save', cancel: 'Cancel', yes: 'Yes', no: 'No', addReference: 'Add Reference', createNew: 'Create New' },
          fields: { id: 'ID', actions: 'Actions', pricedResourceId: 'Priced Resource ID', priceValue: 'Price', minQuantity: 'Min Quantity', unit: 'Unit', currency: 'Currency', taxClass: 'Tax Class', taxIncluded: 'Tax Included', validFrom: 'Valid From', validTo: 'Valid To' },
          pagination: { previous: 'Previous', next: 'Next' },
          statuses: { loading: 'Loading...' }
        };
      } else if (part === 'pages.json') {
        body = {
          home: { title: 'Home' },
          pricerows: {
            title: 'Price Rows',
            addPriceRow: 'Add Price Row',
            editPriceRow: 'Edit Price Row'
          }
        };
      } else if (part === 'components.json') {
        body = {
          sidebar: {
            home: 'Home',
            pricerows: 'Price Rows',
            categories: { commerceManagement: 'Commerce' }
          }
        };
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
    });

    // 5. Mock Available Languages
    await page.route('**/admin/api/languages?q=active:true', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [{ isoKey: 'en', active: true, mandatory: true }]
        }),
      });
    });

    // 6. Mock Price Rows API
    await page.route('**/admin/api/pricerows**', async (route) => {
      const url = route.request().url();
      const method = route.request().method();

      if (url.endsWith('/$meta')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            entityType: 'PriceRow',
            mandatoryFields: ['priceType', 'pricedResourceId', 'priceValue', 'minQuantity', 'unitRef', 'currencyRef'],
            enumValues: {
              priceType: ['DEFAULT', 'PROMO', 'CLEARANCE']
            }
          }),
        });
      } else if (method === 'GET') {
        // List or Single
        if (url.match(/\/pricerows\/[^\/\?\$]+(\?|$)/)) {
          // Single
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'PR-1',
              priceType: 'DEFAULT',
              pricedResourceId: 'product-1',
              priceValue: 10.00,
              minQuantity: 1,
              unitRef: 'PCE',
              currencyRef: 'EUR',
              taxClassRef: 'STANDARD',
              taxIncluded: true
            }),
          });
        } else {
          // List
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              items: [
                {
                  id: 'PR-1',
                  priceType: 'DEFAULT',
                  pricedResourceId: 'product-1',
                  priceValue: 10.00,
                  minQuantity: 1,
                  unitRef: 'PCE',
                  currencyRef: 'EUR',
                  taxClassRef: 'STANDARD',
                  taxIncluded: true
                }
              ],
              $info: {
                paging: { page: 0, 'page-size': 20, 'total-items': 1, 'total-pages': 1 }
              }
            }),
          });
        }
      } else if (method === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ id: 'PR-NEW', ...route.request().postDataJSON() }),
        });
      } else if (method === 'PATCH' || method === 'PUT') {
         await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ id: 'PR-UPDATED' }),
        });
      }
    });

    // 7. Mock Lookups (Units, Currencies, TaxClasses)
    await page.route('**/admin/api/units**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [{ symbol: 'PCE', name: 'Piece' }],
          $info: { paging: { page: 0, 'page-size': 20, 'total-items': 1, 'total-pages': 1 } }
        }),
      });
    });

    await page.route('**/admin/api/currencies**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [{ currencyKey: 'EUR', symbol: '€' }],
          $info: { paging: { page: 0, 'page-size': 20, 'total-items': 1, 'total-pages': 1 } }
        }),
      });
    });

    await page.route('**/admin/api/taxclasses**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [{ taxClassId: 'STANDARD', taxRate: 19.0 }],
          $info: { paging: { page: 0, 'page-size': 20, 'total-items': 1, 'total-pages': 1 } }
        }),
      });
    });

    // 8. Inject Tokens directly into sessionStorage to bypass redirect flow
    await page.addInitScript(({ token, idToken, claims }) => {
      sessionStorage.setItem('access_token', token);
      sessionStorage.setItem('id_token', idToken);
      sessionStorage.setItem('id_token_claims_obj', JSON.stringify(claims));
      sessionStorage.setItem('access_token_stored_at', Date.now().toString());
      sessionStorage.setItem('id_token_stored_at', Date.now().toString());
      sessionStorage.setItem('id_token_expires_at', (Date.now() + 3600000).toString());
    }, { token: mockToken, idToken: mockToken, claims: mockClaims });

    await page.goto('/en/home');
    await page.waitForLoadState('domcontentloaded');
    await use(page);
  },
});

export { expect } from '@playwright/test';
