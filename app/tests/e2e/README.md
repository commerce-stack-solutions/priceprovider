# End-to-End Tests for PriceProvider

This directory contains the Playwright-based end-to-end tests for the PriceProvider application.

## Quick Start

### Installation

Install dependencies from the `app` directory:

```bash
cd app
npm install
npx playwright install --with-deps
```

### Configuration

Create a `.env.test` file in the `app` directory with the following variables:

```env
BASE_URL=http://localhost:4200
```

### Running Tests

Run all tests from the `app` directory:
```bash
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

## Project Structure

```
tests/e2e/
├── specs/                  # Test specification files
│   ├── authentication.spec.ts
│   ├── authenticated.spec.ts
│   └── pricerows.spec.ts
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

## Authentication Mocking

The tests use a custom fixture in `fixtures/auth.fixture.ts` to mock the OIDC/Keycloak authentication. This allows testing authenticated states without requiring a running Keycloak instance. It intercepts OIDC discovery requests and pre-populates `sessionStorage` with mock JWT tokens.

## Test Coverage

- Authentication states (unauthenticated vs. authenticated)
- Price Row management (listing and adding price rows)
- Basic navigation and layout verification

## CI/CD

Tests run automatically in GitHub Actions on push and pull requests to develop/main branches. The workflow is defined in `.github/workflows/e2e.yml`.
