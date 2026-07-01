import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;
  readonly pageTitle: Locator;

  constructor(page: Page) {
    super(page);
    this.usernameInput = page.locator('input[type="text"][name="username"]');
    this.passwordInput = page.locator('input[type="password"][name="password"]');
    this.loginButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('.error-message, .alert-danger');
    this.pageTitle = page.locator('h1, h2, h3');
  }

  async navigateToLogin(): Promise<void> {
    await this.navigate('/login');
  }

  async login(username: string, password: string): Promise<void> {
    await this.fillUsername(username);
    await this.fillPassword(password);
    await this.clickLogin();
  }

  async fillUsername(username: string): Promise<void> {
    await this.fill('input[type="text"][name="username"]', username);
  }

  async fillPassword(password: string): Promise<void> {
    await this.fill('input[type="password"][name="password"]', password);
  }

  async clickLogin(): Promise<void> {
    await this.click('button[type="submit"]');
    await this.page.waitForNavigation();
  }

  async getErrorMessage(): Promise<string> {
    return this.getText('.error-message, .alert-danger');
  }

  async isLoginSuccessful(): Promise<boolean> {
    const currentUrl = await this.getCurrentUrl();
    return !currentUrl.includes('/login');
  }

  async isLoginFormVisible(): Promise<boolean> {
    return this.isVisible('input[type="text"][name="username"]');
  }
}