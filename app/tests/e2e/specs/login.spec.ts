import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { adminUser, invalidUser, emptyUser } from '../fixtures/testUsers';

test.describe('Login Flow', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    await loginPage.navigateToLogin();
  });

  test('should display login form', async () => {
    await expect(loginPage.isLoginFormVisible()).resolves.toBe(true);
    await expect(loginPage.pageTitle).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
    await expect(dashboardPage.isUserLoggedIn()).resolves.toBe(true);
  });

  test('should show error message with invalid credentials', async () => {
    await loginPage.login(invalidUser.username, invalidUser.password);
    await expect(loginPage.isLoginFormVisible()).resolves.toBe(true);
    const errorMessage = await loginPage.getErrorMessage();
    expect(errorMessage).toContain('Invalid') || expect(errorMessage).toContain('error');
  });

  test('should not login with empty credentials', async () => {
    await loginPage.login(emptyUser.username, emptyUser.password);
    await expect(loginPage.isLoginFormVisible()).resolves.toBe(true);
  });

  test('should navigate to dashboard after successful login', async ({ page }) => {
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
    await expect(page).toHaveURL(/dashboard/);
  });
});

test.describe('Login Page Validation', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.navigateToLogin();
  });

  test('should show required error for empty username', async () => {
    await loginPage.fillPassword('test123');
    await loginPage.clickLogin();
    const errorMessage = await loginPage.getErrorMessage();
    expect(errorMessage).toBeTruthy();
  });

  test('should show required error for empty password', async () => {
    await loginPage.fillUsername('testuser');
    await loginPage.clickLogin();
    const errorMessage = await loginPage.getErrorMessage();
    expect(errorMessage).toBeTruthy();
  });
});