import { test, expect } from '@playwright/test';
import { HomePage } from '../pages/HomePage';

test.describe('Authentication Flow (Unauthenticated)', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ page }) => {
    homePage = new HomePage(page);
    await homePage.navigateToHome();
  });

  test('should display login button when not authenticated', async () => {
    await expect(homePage.loginButton.first()).toBeVisible();
  });

  test('should display login required message when not authenticated', async () => {
    await expect(homePage.loginRequiredMessage).toBeVisible();
  });

  test('should not display router outlet when not authenticated', async () => {
    await expect(homePage.routerOutlet).not.toBeVisible();
  });
});
