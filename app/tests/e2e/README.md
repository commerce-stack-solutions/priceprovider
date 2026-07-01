# End-to-End Tests for PriceProvider

This directory contains the Playwright-based end-to-end tests for the PriceProvider application.

## Quick Start

### Installation

Install dependencies from the `app` directory:

```bash
cd app
npm install
# Note: You may need root/admin permissions to install system dependencies
sudo npx playwright install --with-deps
```

### Preparation

Before running the tests, ensure both the frontend is running.

**Start the Angular Frontend**:
   ```bash
   cd app
   npm start
   ```

### Running Tests

Once the services are up, run the tests from the `app` directory:

```bash
cd app
npm run test:e2e
```

Run with UI mode:
```bash
npm run test:e2e:ui
```

Run with visible browser:
```bash
npm run test:e2e:headed
```

## Authentication Mocking

The tests use a custom fixture in `fixtures/auth.fixture.ts` to mock the OIDC/Keycloak authentication.

- **How it works**: It intercepts OIDC discovery requests and pre-populates the browser's `sessionStorage` with mock JWT tokens.
- **Usage**: To use it in a test, import `test` from `../fixtures/auth.fixture` instead of `@playwright/test` and use the `authenticatedPage` fixture.
- **Benefit**: This allows testing authenticated states without needing a running Keycloak instance or real user credentials.

Example usage:
```typescript
import { test, expect } from '../fixtures/auth.fixture';

test('my test', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/en/home');
  // ...
});
```

## Project Structure

```
tests/e2e/
├── specs/                  # Test specification files
│   ├── authentication.spec.ts # Tests for unauthenticated state
│   ├── authenticated.spec.ts  # Tests for authenticated state (using mock)
│   └── pricerows.spec.ts      # Complex workflow tests (using mock)
├── pages/                 # Page Object Model classes
│   ├── BasePage.ts
│   ├── HomePage.ts
│   ├── PriceRowsPage.ts
│   └── PriceRowFormPage.ts
├── fixtures/              # Test fixtures and data
│   ├── auth.fixture.ts    # Mock authentication logic
│   └── testUsers.ts
├── playwright.config.ts   # Playwright configuration
└── README.md
```

## CI/CD

Tests run automatically in GitHub Actions on push and pull requests to develop/main branches. The workflow is defined in `.github/workflows/e2e.yml`.
