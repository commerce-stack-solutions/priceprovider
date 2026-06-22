import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { adminUser } from '../fixtures/testUsers';

test.describe('Dashboard Flow', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    await loginPage.navigateToLogin();
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
  });

  test('should display dashboard after login', async () => {
    await expect(dashboardPage.isDashboardLoaded()).resolves.toBe(true);
  });

  test('should display welcome message', async () => {
    const welcomeMessage = await dashboardPage.getWelcomeMessage();
    expect(welcomeMessage).toBeTruthy();
    expect(welcomeMessage.toLowerCase()).toContain('welcome') || 
      expect(welcomeMessage.toLowerCase()).toContain('dashboard');
  });

  test('should have navigation menu', async () => {
    await expect(dashboardPage.navigationMenu).toBeVisible();
  });

  test('should have logout button', async () => {
    await expect(dashboardPage.logoutButton).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    await dashboardPage.logout();
    await page.waitForURL('**/login');
    await expect(loginPage.isLoginFormVisible()).resolves.toBe(true);
  });
});

test.describe('Dashboard Navigation', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    await loginPage.navigateToLogin();
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
  });

  test('should navigate to dashboard from login', async ({ page }) => {
    await expect(page).toHaveURL(/dashboard/);
  });

  test('should maintain session across page reloads', async ({ page }) => {
    await page.reload();
    await expect(dashboardPage.isUserLoggedIn()).resolves.toBe(true);
  });
});