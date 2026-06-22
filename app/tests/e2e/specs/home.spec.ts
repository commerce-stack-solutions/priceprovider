import { test, expect } from '@playwright/test';
import { HomePage } from '../pages/HomePage';

test.describe('Home Page', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ page }) => {
    homePage = new HomePage(page);
    await homePage.navigate('/en/home');
  });

  test('should have correct page title', async ({ page }) => {
    const title = await page.title();
    expect(title).toContain('Price Provider');
  });

  test('should display header component', async () => {
    await expect(homePage.header).toBeVisible();
  });

  test('should display sidebar component', async () => {
    await expect(homePage.sidebar).toBeVisible();
  });

  test('should display main content area', async () => {
    await expect(homePage.page.locator('.app-container')).toBeVisible();
  });
});