import { test, expect } from '../fixtures/auth.fixture';

test.describe('Generic Form Component', () => {

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

    // Verify it navigated back to currencies list (or list route)
    await expect(authenticatedPage).toHaveURL(/.*\/currencies/);

    // Verify payload is structured correctly
    expect(postBody).not.toBeNull();
    expect(postBody.currencyKey).toBe('CHF');
    expect(postBody.symbol).toBe('CHF');
    expect(postBody.name.en).toBe('Swiss Franc');
  });
});
