import { test, expect } from '@playwright/test';
import { HomePage } from '../pages/HomePage';

test.describe('Dashboard and Navigation', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ page }) => {
    homePage = new HomePage(page);
    await homePage.navigate('/en/home');
  });

  test('should display header', async () => {
    await expect(homePage.header).toBeVisible();
  });

  test('should display sidebar', async () => {
    await expect(homePage.sidebar).toBeVisible();
  });

  test('should have correct URL', async ({ page }) => {
    await expect(page).toHaveURL(/en/home/);
  });
});