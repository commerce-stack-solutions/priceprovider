import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class HomePage extends BasePage {
  readonly header: Locator;
  readonly sidebar: Locator;
  readonly routerOutlet: Locator;
  readonly loginButton: Locator;
  readonly logoutButton: Locator;
  readonly loginRequiredMessage: Locator;
  readonly noAdminAccessMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.header = page.locator('app-header');
    this.sidebar = page.locator('app-sidebar');
    this.routerOutlet = page.locator('router-outlet');
    // More specific locators to avoid strict mode violations (multiple buttons/text)
    this.loginButton = page.locator('.login-required-container button:has-text("Login"), app-header button[title="Login"]');
    this.logoutButton = page.locator('.no-admin-access-container button:has-text("Logout"), app-header button[title="Logout"]');
    this.loginRequiredMessage = page.locator('.login-required-container h2');
    this.noAdminAccessMessage = page.locator('.no-admin-access-container h2');
  }

  async navigateToHome(lang: string = 'en'): Promise<void> {
    await this.navigate(`/${lang}/home`);
  }

  async isAuthenticated(): Promise<boolean> {
    return this.routerOutlet.isVisible();
  }

  async isNotAuthenticated(): Promise<boolean> {
    return this.loginButton.first().isVisible();
  }

  async clickLogin(): Promise<void> {
    await this.loginButton.first().click();
  }

  async clickLogout(): Promise<void> {
    await this.logoutButton.first().click();
  }
}