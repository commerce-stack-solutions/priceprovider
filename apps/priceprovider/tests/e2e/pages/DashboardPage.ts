import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class DashboardPage extends BasePage {
  readonly welcomeMessage: Locator;
  readonly logoutButton: Locator;
  readonly navigationMenu: Locator;
  readonly priceProviderTitle: Locator;

  constructor(page: Page) {
    super(page);
    this.welcomeMessage = page.locator('.welcome-message, h1, h2');
    this.logoutButton = page.locator('button:has-text("Logout"), a:has-text("Logout")');
    this.navigationMenu = page.locator('nav, .sidebar');
    this.priceProviderTitle = page.locator('text=Price Provider');
  }

  async navigateToDashboard(): Promise<void> {
    await this.navigate('/dashboard');
  }

  async isDashboardLoaded(): Promise<boolean> {
    return this.isVisible('.welcome-message, h1, h2');
  }

  async getWelcomeMessage(): Promise<string> {
    return this.getText('.welcome-message, h1, h2');
  }

  async logout(): Promise<void> {
    await this.click('button:has-text("Logout"), a:has-text("Logout")');
    await this.page.waitForNavigation();
  }

  async isUserLoggedIn(): Promise<boolean> {
    return this.isVisible('.welcome-message, h1, h2');
  }

  async navigateToSection(sectionName: string): Promise<void> {
    await this.click(`nav a:has-text(${sectionName})`);
    await this.page.waitForNavigation();
  }
}