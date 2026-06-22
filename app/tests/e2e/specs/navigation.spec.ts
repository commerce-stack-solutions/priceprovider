import { test, expect } from '@playwright/test';
import { HomePage } from '../pages/HomePage';

test.describe('Application Navigation', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ page }) => {
    homePage = new HomePage(page);
  });

  test('should navigate to English home page', async ({ page }) => {
    await homePage.navigate('/en/home');
    await expect(page).toHaveURL(/en/home/);
  });

  test('should navigate to German home page', async ({ page }) => {
    await homePage.navigate('/de/home');
    await expect(page).toHaveURL(/de/home/);
  });

  test('should display login UI on all language pages', async ({ page }) => {
    await homePage.navigate('/en/home');
    await expect(homePage.loginButton).toBeVisible();
    
    await homePage.navigate('/de/home');
    await expect(homePage.loginButton).toBeVisible();
  });
});