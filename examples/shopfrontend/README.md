# Shop Frontend Demo

A minimal demo shop frontend that consumes the Price Provider public API with optional Keycloak login.

## Features

- Product page with static product image, name, description and a price from the public Price Provider API
- SKU selector and quantity input with dynamic price update
- **Login button** in the upper right corner using full Keycloak OIDC Authorization Code + PKCE flow
- After login, organization-filtered prices are shown automatically (based on the user's `groups` claim)

## Setup

### Prerequisites

1. Keycloak running at `http://localhost:8081` with the `priceprovider` realm imported (`idp/keycloak/realm-export.json`)
2. Price Provider Service running at `http://localhost:8080`

### Install dependencies

```bash
cd examples/shopfrontend
npm install
```

### Start the demo server

```bash
npm start
```

Open `http://localhost:3000` in your browser.

## Demo Users (from Keycloak realm seed)

| Username | Password | Role | Organization |
|---|---|---|---|
| `customer-city-council` | `customer123` | `priceprovider.public:PriceRowReader` | ORG-CITY-COUNCIL |
| `customer-city-health` | `customer123` | `priceprovider.public:PriceRowReader` | ORG-CITY-HEALTH (deepest wins) |
| `customer-techcorp` | `customer123` | `priceprovider.public:PriceRowReader` | ORG-TECHCORP-EU |
| `customer-city-council-inspector` | `customer123` | `priceprovider.public:PriceRowInspector` | ORG-CITY-COUNCIL |

## How It Works

1. **Authentication required**: Public price endpoints require a user with `priceprovider.public:PriceRow:read`.
2. **Organization-scoped pricing**: After login, the user's `groups` claim is parsed. The deepest organization group path is used as the filter.

### Price API calls

The shop uses the Public Price API:
`GET /public/api/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}?quantity={qty}&unit={unit}&currency={currency}`

Organization-specific pricing is automatically applied by the service based on the authenticated user's JWT groups.
