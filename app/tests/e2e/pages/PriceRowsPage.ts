import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class PriceRowsPage extends BasePage {
  readonly addPriceRowButton: Locator;
  readonly tableRows: Locator;

  constructor(page: Page) {
    super(page);
    this.addPriceRowButton = page.locator('a:has-text("Add Price Row")');
    this.tableRows = page.locator('tbody tr');
  }

  async navigateTo(lang: string = 'en'): Promise<void> {
    await this.navigate(`/${lang}/pricerows`);
  }

  async clickAddPriceRow(): Promise<void> {
    await this.addPriceRowButton.click();
  }

  async getRowCount(): Promise<number> {
    return this.tableRows.count();
  }

  async clickEditRow(index: number): Promise<void> {
    await this.tableRows.nth(index).locator('a:has-text("Edit")').click();
  }
}
