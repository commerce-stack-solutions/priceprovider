import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class PriceRowFormPage extends BasePage {
  readonly priceTypeSelector: Locator;
  readonly pricedResourceIdInput: Locator;
  readonly priceValueInput: Locator;
  readonly minQuantityInput: Locator;
  readonly unitRefInput: Locator;
  readonly currencyInput: Locator;
  readonly taxClassRefInput: Locator;
  readonly taxIncludedTrue: Locator;
  readonly taxIncludedFalse: Locator;
  readonly saveButton: Locator;
  readonly successMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.priceTypeSelector = page.locator('app-enum-selector select');
    this.pricedResourceIdInput = page.locator('#pricedResourceId');
    this.priceValueInput = page.locator('#priceValue');
    this.minQuantityInput = page.locator('#minQuantity');

    // Improved locators using ARIA or specific hierarchy
    this.unitRefInput = page.locator('div.row:has(label:has-text("Unit")) app-reference-edit input');
    this.currencyInput = page.locator('div.row:has(label:has-text("Currency")) app-reference-edit input');
    this.taxClassRefInput = page.locator('div.row:has(label:has-text("Tax Class")) app-reference-edit input');

    this.taxIncludedTrue = page.locator('#taxIncludedTrue');
    this.taxIncludedFalse = page.locator('#taxIncludedFalse');
    this.saveButton = page.locator('button[type="submit"]');
    this.successMessage = page.locator('.alert-success');
  }

  async fillForm(data: {
    priceType: string;
    pricedResourceId: string;
    priceValue: string;
    minQuantity: string;
    unitRef: string;
    currency: string;
    taxClassRef: string;
    taxIncluded: boolean;
  }): Promise<void> {
    await this.priceTypeSelector.selectOption({ value: data.priceType });

    await this.pricedResourceIdInput.fill(data.pricedResourceId);
    await this.priceValueInput.fill(data.priceValue);
    await this.minQuantityInput.fill(data.minQuantity);

    // Fill Unit
    await this.unitRefInput.fill(data.unitRef);
    await this.page.locator(`.dropdown-item:has-text("${data.unitRef}")`).first().click();

    // Fill Currency
    await this.currencyInput.fill(data.currency);
    await this.page.locator(`.dropdown-item:has-text("${data.currency}")`).first().click();

    // Fill Tax Class
    await this.taxClassRefInput.fill(data.taxClassRef);
    await this.page.locator(`.dropdown-item:has-text("${data.taxClassRef}")`).first().click();

    if (data.taxIncluded) {
      await this.taxIncludedTrue.check();
    } else {
      await this.taxIncludedFalse.check();
    }
  }

  async save(): Promise<void> {
    await this.saveButton.click();
  }
}
