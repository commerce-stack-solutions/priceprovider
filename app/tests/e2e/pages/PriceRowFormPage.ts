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
    this.priceTypeSelector = page.locator('app-enum-selector');
    this.pricedResourceIdInput = page.locator('#pricedResourceId');
    this.priceValueInput = page.locator('#priceValue');
    this.minQuantityInput = page.locator('#minQuantity');
    this.unitRefInput = page.locator('app-reference-edit').first().locator('input');
    this.currencyInput = page.locator('app-reference-edit').nth(1).locator('input');
    this.taxClassRefInput = page.locator('app-reference-edit').nth(2).locator('input');
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
    // Handling app-enum-selector might be tricky, let's assume it's clickable
    await this.priceTypeSelector.click();
    await this.page.locator(`text=${data.priceType}`).click();

    await this.pricedResourceIdInput.fill(data.pricedResourceId);
    await this.priceValueInput.fill(data.priceValue);
    await this.minQuantityInput.fill(data.minQuantity);

    await this.unitRefInput.fill(data.unitRef);
    await this.page.keyboard.press('Enter');

    await this.currencyInput.fill(data.currency);
    await this.page.keyboard.press('Enter');

    await this.taxClassRefInput.fill(data.taxClassRef);
    await this.page.keyboard.press('Enter');

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
