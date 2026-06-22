# End-to-End Tests for PriceProvider

This directory contains the Playwright-based end-to-end tests for the PriceProvider application.

## Quick Start

### Installation

Install Playwright and its dependencies:

```bash
cd app
npm install
cd tests/e2e
npm install
```

### Configuration

Create a `.env.test` file in the `app` directory with the following variables:

```env
BASE_URL=http://localhost:4200
```

### Running Tests

Run all tests:
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
│   ├── login.spec.ts
│   ├── dashboard.spec.ts
│   └── navigation.spec.ts
├── pages/                 # Page Object Model classes
│   ├── BasePage.ts
│   ├── LoginPage.ts
│   └── DashboardPage.ts
├── fixtures/              # Test data
│   └── testUsers.ts
├── playwright.config.ts   # Configuration
└── README.md
```

## Test Coverage

- Login flow (valid, invalid, empty credentials)
- Dashboard functionality
- Application navigation
- Session management

## CI/CD

Tests run automatically in GitHub Actions on push and pull requests to develop/main branches.

Artifacts (reports, screenshots, videos, traces) are uploaded on test completion.