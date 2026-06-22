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
    this.loginButton = page.locator('button:has-text("Login")');
    this.logoutButton = page.locator('button:has-text("Logout")');
    this.loginRequiredMessage = page.locator('text=/login required/i');
    this.noAdminAccessMessage = page.locator('text=/no admin access/i');
  }

  async navigateToHome(lang: string = 'en'): Promise<void> {
    await this.navigate(`/${lang}/home`);
  }

  async isAuthenticated(): Promise<boolean> {
    return this.isVisible('router-outlet');
  }

  async isNotAuthenticated(): Promise<boolean> {
    return this.isVisible('button:has-text("Login")');
  }

  async clickLogin(): Promise<void> {
    await this.click('button:has-text("Login")');
  }

  async clickLogout(): Promise<void> {
    await this.click('button:has-text("Logout")');
  }
}