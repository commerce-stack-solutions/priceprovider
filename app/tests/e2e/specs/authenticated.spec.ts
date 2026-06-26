import { test, expect } from '../fixtures/auth.fixture';
import { HomePage } from '../pages/HomePage';

test.describe('Authenticated State', () => {
  let homePage: HomePage;

  test.beforeEach(async ({ authenticatedPage }) => {
    homePage = new HomePage(authenticatedPage);
    await homePage.navigateToHome();
    // Wait for the app to recognize the authenticated state from sessionStorage
    await authenticatedPage.waitForLoadState('networkidle');
  });

  test('should not display login button when authenticated', async ({ authenticatedPage }) => {
    // Increased timeout for stabilization
    await expect(homePage.loginButton.first()).not.toBeVisible({ timeout: 10000 });
  });

  test('should display logout button when authenticated', async ({ authenticatedPage }) => {
    await expect(homePage.logoutButton).toBeVisible();
  });

  test('should display content (router outlet) when authenticated', async ({ authenticatedPage }) => {
    await expect(homePage.routerOutlet).toBeVisible();
  });

  test('should not display login required message when authenticated', async ({ authenticatedPage }) => {
    await expect(homePage.loginRequiredMessage).not.toBeVisible();
  });
});
