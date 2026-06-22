import { test, expect } from '@playwright/test';
import { HomePage } from '../pages/HomePage';

test.describe('Authentication Flow', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ page }) => {
    homePage = new HomePage(page);
    await homePage.navigate('/en/home');
  });

  test('should display login button when not authenticated', async () => {
    await expect(homePage.loginButton).toBeVisible();
  });

  test('should display login required message when not authenticated', async () => {
    const message = homePage.loginRequiredMessage;
    await expect(message).toBeVisible();
  });

  test('should not display router outlet when not authenticated', async () => {
    await expect(homePage.routerOutlet).not.toBeVisible();
  });

  test('should not display logout button when not authenticated', async () => {
    await expect(homePage.logoutButton).not.toBeVisible();
  });

  test('should have login button with correct text', async () => {
    await expect(homePage.loginButton).toHaveText(/Login/i);
  });
});