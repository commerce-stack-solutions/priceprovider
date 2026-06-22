import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { adminUser } from '../fixtures/testUsers';

test.describe('Application Navigation', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
  });

  test('should navigate from login to dashboard', async ({ page }) => {
    await loginPage.navigateToLogin();
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
    await expect(dashboardPage.isDashboardLoaded()).resolves.toBe(true);
  });

  test('should navigate back to login after logout', async ({ page }) => {
    await loginPage.navigateToLogin();
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
    await dashboardPage.logout();
    await page.waitForURL('**/login');
    await expect(loginPage.isLoginFormVisible()).resolves.toBe(true);
  });

  test('should handle direct dashboard access without login', async ({ page }) => {
    await dashboardPage.navigateToDashboard();
    await page.waitForURL('**/login');
    await expect(loginPage.isLoginFormVisible()).resolves.toBe(true);
  });
});

test.describe('Page Title Verification', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
  });

  test('login page should have correct title', async ({ page }) => {
    await loginPage.navigateToLogin();
    const title = await loginPage.getTitle();
    expect(title).toContain('Login') || expect(title).toContain('Price Provider');
  });

  test('dashboard page should have correct title', async ({ page }) => {
    await loginPage.navigateToLogin();
    await loginPage.login(adminUser.username, adminUser.password);
    await page.waitForURL('**/dashboard');
    const title = await dashboardPage.getTitle();
    expect(title).toContain('Dashboard') || expect(title).toContain('Price Provider');
  });
});