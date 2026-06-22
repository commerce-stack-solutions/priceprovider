import { test, expect } from '@playwright/test';
import { HomePage } from '../pages/HomePage';

test.describe('Login Page', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ page }) => {
    homePage = new HomePage(page);
    await homePage.navigate('/en/home');
  });

  test('should display login button', async () => {
    await expect(homePage.loginButton).toBeVisible();
  });

  test('should display login required message', async () => {
    await expect(homePage.loginRequiredMessage).toBeVisible();
  });

  test('should have login button with Login text', async () => {
    await expect(homePage.loginButton).toContainText('Login');
  });
});