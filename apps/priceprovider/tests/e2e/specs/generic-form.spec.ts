import { test, expect } from '../fixtures/auth.fixture';

test.describe('Generic Meta UI Components', () => {

  test('should render dynamic form inputs based on $meta API', async ({ authenticatedPage }) => {
    // 1. Mock the currencies $meta call specifically
    await authenticatedPage.route('**/admin/api/currencies/$meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          identityFields: ['currencyKey'],
          mandatoryFields: ['currencyKey', 'symbol'],
          referenceKeyFields: ['currencyKey'],
          fields: [
            { name: 'currencyKey', type: 'String', readOnly: false },
            { name: 'symbol', type: 'String', readOnly: false },
            { name: 'name', type: 'LocalizedString', readOnly: false }
          ]
        })
      });
    });

    // 2. Navigate to the generic form add currencies path
    await authenticatedPage.goto('/en/generic/currencies/add');
    await authenticatedPage.waitForLoadState('domcontentloaded');

    // 3. Verify heading is displayed
    const heading = authenticatedPage.locator('h1');
    await expect(heading).toContainText('Add Currencies');

    // 4. Verify the specific inputs are rendered
    await expect(authenticatedPage.locator('input#currencyKey')).toBeVisible();
    await expect(authenticatedPage.locator('input#symbol')).toBeVisible();

    // Localized string edit field for EN should be visible based on mock languages
    await expect(authenticatedPage.locator('input[placeholder="Name in EN"]')).toBeVisible();

    // Take screenshot of the successfully rendered form!
    await authenticatedPage.screenshot({ path: 'app-generic-form-screenshot.png' });
  });

  test('should submit the generic form and issue POST create', async ({ authenticatedPage }) => {
    let postBody: any = null;

    // Mock $meta
    await authenticatedPage.route('**/admin/api/currencies/$meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          identityFields: ['currencyKey'],
          mandatoryFields: ['currencyKey', 'symbol'],
          referenceKeyFields: ['currencyKey'],
          fields: [
            { name: 'currencyKey', type: 'String', readOnly: false },
            { name: 'symbol', type: 'String', readOnly: false },
            { name: 'name', type: 'LocalizedString', readOnly: false }
          ]
        })
      });
    });

    // Mock POST create and capture payload
    await authenticatedPage.route('**/admin/api/currencies/create', async (route) => {
      postBody = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          currencyKey: 'CHF',
          symbol: 'CHF',
          name: { en: 'Swiss Franc' }
        })
      });
    });

    // Navigate
    await authenticatedPage.goto('/en/generic/currencies/add');
    await authenticatedPage.waitForLoadState('domcontentloaded');

    // Fill form fields
    await authenticatedPage.locator('input#currencyKey').fill('CHF');
    await authenticatedPage.locator('input#symbol').fill('CHF');
    await authenticatedPage.locator('input[placeholder="Name in EN"]').fill('Swiss Franc');

    // Submit form
    await authenticatedPage.locator('button[type="submit"]').click();

    // Verify it navigated back to the currencies list route
    await expect(authenticatedPage).toHaveURL(/.*\/currencies$/);

    // Verify payload is structured correctly
    expect(postBody).not.toBeNull();
    expect(postBody.currencyKey).toBe('CHF');
    expect(postBody.symbol).toBe('CHF');
    expect(postBody.name.en).toBe('Swiss Franc');
  });

  test('should correctly query group reference endpoint for parentRefs field using referencedEntity metadata', async ({ authenticatedPage }) => {
    let queriedApiUrl: string | null = null;

    // 1. Mock groups $meta call returning parentRefs with referencedEntity set to Group
    await authenticatedPage.route('**/admin/api/groups/$meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          identityFields: ['id'],
          mandatoryFields: ['path', 'name'],
          referenceKeyFields: ['path'],
          fields: [
            { name: 'id', type: 'String', readOnly: true },
            { name: 'path', type: 'String', readOnly: false },
            { name: 'name', type: 'String', readOnly: false },
            { name: 'parentRefs', type: 'Set<Reference>', referencedEntity: 'Group', readOnly: false }
          ]
        })
      });
    });

    // 2. Intercept general API searches to verify it calls admin/api/groups instead of parentrefs
    await authenticatedPage.route('**/admin/api/groups?**', async (route) => {
      const url = route.request().url();
      queriedApiUrl = url;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [{ id: 'GRP-1', path: 'ORG-ROOT', name: 'Root Organization' }],
          $info: { paging: { page: 0, 'page-size': 30, 'total-items': 1, 'total-pages': 1 } }
        })
      });
    });

    // 3. Navigate to add groups generic form
    await authenticatedPage.goto('/en/generic/groups/add');
    await authenticatedPage.waitForLoadState('domcontentloaded');

    // 4. Focus on parentRefs input to trigger drop down lookup
    await authenticatedPage.locator('input[placeholder="Search ParentRefs..."]').click();

    // 5. Verify the backend call target was indeed groups (not parentrefs!)
    await expect.poll(() => queriedApiUrl).toContain('admin/api/groups');
  });

  test('should render dynamic list view based on $meta API', async ({ authenticatedPage }) => {
    // 1. Mock the currencies $meta call
    await authenticatedPage.route('**/admin/api/currencies/$meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          identityFields: ['currencyKey'],
          mandatoryFields: ['currencyKey', 'symbol'],
          referenceKeyFields: ['currencyKey'],
          fields: [
            { name: 'currencyKey', type: 'String', readOnly: false },
            { name: 'symbol', type: 'String', readOnly: false },
            { name: 'name', type: 'LocalizedString', readOnly: false }
          ]
        })
      });
    });

    // 2. Mock currencies data list call
    await authenticatedPage.route('**/admin/api/currencies?**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [
            { currencyKey: 'EUR', symbol: '€', name: { en: 'Euro' } },
            { currencyKey: 'USD', symbol: '$', name: { en: 'US Dollar' } }
          ],
          $info: { paging: { page: 0, 'page-size': 50, 'total-items': 2, 'total-pages': 1 } }
        })
      });
    });

    // 3. Navigate to generic list
    await authenticatedPage.goto('/en/generic/currencies');
    await authenticatedPage.waitForLoadState('domcontentloaded');

    // 4. Verify table headers & records
    await expect(authenticatedPage.locator('th:has-text("CurrencyKey")')).toBeVisible();
    await expect(authenticatedPage.locator('th:has-text("Symbol")')).toBeVisible();
    await expect(authenticatedPage.locator('th:has-text("Name")')).toBeVisible();

    await expect(authenticatedPage.getByRole('cell', { name: 'EUR', exact: true })).toBeVisible();
    await expect(authenticatedPage.getByRole('cell', { name: 'USD', exact: true })).toBeVisible();
  });

  test('should render dynamic detail view based on $meta API', async ({ authenticatedPage }) => {
    // 1. Mock the currencies $meta call
    await authenticatedPage.route('**/admin/api/currencies/$meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          identityFields: ['currencyKey'],
          mandatoryFields: ['currencyKey', 'symbol'],
          referenceKeyFields: ['currencyKey'],
          fields: [
            { name: 'currencyKey', type: 'String', readOnly: false },
            { name: 'symbol', type: 'String', readOnly: false },
            { name: 'name', type: 'LocalizedString', readOnly: false }
          ]
        })
      });
    });

    // 2. Mock currencies single data call
    await authenticatedPage.route('**/admin/api/currencies/EUR**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          currencyKey: 'EUR',
          symbol: '€',
          name: { en: 'Euro' },
          $info: { createdAt: '2026-07-22T00:00:00.000Z', lastModifiedAt: '2026-07-22T12:00:00.000Z' }
        })
      });
    });

    // 3. Navigate to generic detail view
    await authenticatedPage.goto('/en/generic/currencies/EUR');
    await authenticatedPage.waitForLoadState('domcontentloaded');

    // 4. Verify detail elements are displayed
    await expect(authenticatedPage.locator('h1')).toContainText('EUR');
    await expect(authenticatedPage.locator('div.col-sm-4:has-text("CurrencyKey") + div.col-sm-8')).toContainText('EUR');
    await expect(authenticatedPage.locator('div.col-sm-4:has-text("Symbol") + div.col-sm-8')).toContainText('€');
  });
});
