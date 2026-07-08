import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class PriceRowsPage extends BasePage {
  readonly addPriceRowButton: Locator;
  readonly tableRows: Locator;
  readonly deleteSelectedButton: Locator;

  constructor(page: Page) {
    super(page);
    this.addPriceRowButton = page.locator('a:has-text("Add Price Row")');
    this.tableRows = page.locator('tbody tr');
    this.deleteSelectedButton = page.locator('button:has-text("Delete Selected")');
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

  async deleteRow(index: number): Promise<void> {
    // There isn't a single delete button in the row based on HTML, maybe it's in detail or multiple selection
    // Assuming we use selection
    await this.tableRows.nth(index).locator('input[type="checkbox"]').check();
    await this.deleteSelectedButton.click();
  }
}
