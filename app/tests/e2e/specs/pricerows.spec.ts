import { test, expect } from '../fixtures/auth.fixture';
import { PriceRowsPage } from '../pages/PriceRowsPage';
import { PriceRowFormPage } from '../pages/PriceRowFormPage';

test.describe('Price Row Management', () => {
  let priceRowsPage: PriceRowsPage;
  let priceRowFormPage: PriceRowFormPage;

  test.beforeEach(async ({ authenticatedPage }) => {
    priceRowsPage = new PriceRowsPage(authenticatedPage);
    priceRowFormPage = new PriceRowFormPage(authenticatedPage);
    await priceRowsPage.navigateTo();
    // Wait for the heading to be visible as a minimum baseline
    await expect(priceRowsPage.page.locator('h1')).toBeVisible();
  });

  test('should display price rows list', async () => {
    await expect(priceRowsPage.page.locator('h1')).toContainText('Price Rows');
    const rowCount = await priceRowsPage.getRowCount();
    expect(rowCount).toBeGreaterThan(0); // Our mock returns 1 item
  });

  test('should navigate to add price row form', async () => {
    await priceRowsPage.clickAddPriceRow();
    await expect(priceRowFormPage.page.locator('h1')).toContainText('Add Price Row');
  });

  test('should fill and submit add price row form', async () => {
    await priceRowsPage.clickAddPriceRow();

    // Ensure form is loaded
    await expect(priceRowFormPage.priceTypeSelector).toBeVisible();

    await priceRowFormPage.fillForm({
      priceType: 'DEFAULT',
      pricedResourceId: 'test-product-' + Date.now(),
      priceValue: '99.99',
      minQuantity: '1',
      unitRef: 'PCE',
      currency: 'EUR',
      taxClassRef: 'STANDARD',
      taxIncluded: true
    });

    await priceRowFormPage.save();

    // In our mock, create returns a PriceRow with id 'PR-NEW'.
    // The component navigates to /pricerows/PR-NEW
    await expect(priceRowsPage.page).toHaveURL(/.*\/pricerows\/PR-NEW/);
  });

  test('should navigate to edit price row form', async () => {
    await expect(priceRowsPage.tableRows.first()).toBeVisible();
    await priceRowsPage.clickEditRow(0);
    await expect(priceRowFormPage.page.locator('h1')).toContainText('Edit Price Row');
  });
});
