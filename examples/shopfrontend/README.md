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
| `customer-city-council` | `customer123` | `priceprovider.public/PriceRowReader` | ORG-CITY-COUNCIL |
| `customer-city-health` | `customer123` | `priceprovider.public/PriceRowReader` | ORG-CITY-HEALTH (deepest wins) |
| `customer-techcorp` | `customer123` | `priceprovider.public/PriceRowReader` | ORG-TECHCORP-EU |

## How It Works

1. **Anonymous**: Prices are fetched from the public API without authentication – returns non-group prices.
2. **Authenticated (with org group)**: After login, the user's `groups` claim is parsed. The deepest organization group path is used as the filter, and the group-specific price endpoint is called.

### Price API calls

The shop uses the Public Price API:
`GET /public/api/channels/{channelId}/countries/{countryIsoKey}/pricedresource/{pricedResourceId}/{priceType}?quantity={qty}&unit={unit}&currency={currency}`

Organization-specific pricing is automatically applied by the service based on the authenticated user's JWT groups.
