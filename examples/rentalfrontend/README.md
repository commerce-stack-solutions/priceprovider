# Tool Rental Frontend Demo

A specialized example for a **tools and device rental service**. Unlike a standard shop, this demo calculates prices based on a two-component model:
1.  **Base Price**: A one-time fee per rented device (e.g., for cleaning or setup).
2.  **Daily Rate**: A recurring fee for each day of the rental period.

## Features

- **Device Selection**: Choose between different professional tools (Drill, Saw, Mower).
- **Rental Period**: Select start and end dates using a calendar interface. The total days are calculated automatically.
- **Bulk Discounts**:
    - The **Base Price** can decrease as you rent more devices (Quantity Tiers: 1+, 3+, 5+).
    - The **Daily Rate** can decrease for longer rental periods (Duration Tiers: 1+, 7+, 14+).
- **Price Matrix**: A live-updating table showing all available discount tiers for the selected tool.
- **Keycloak Integration**: Login to see organization-specific contract prices and bulk tiers.

## Setup

### Prerequisites

1.  **Keycloak** running at `http://localhost:8081`.
2.  **Price Provider Service** running at `http://localhost:8080`.
3.  **Sample Data** enabled in the backend (`application.yaml` -> `sample-data-on: true`).

### Start the demo server

```bash
cd examples/rentalfrontend
npm install
npm start
```

Open [http://localhost:3001](http://localhost:3001) in your browser.

## Demo Users

These users are pre-configured in the Keycloak `priceprovider` realm:

| Username | Password | Organization | Note |
|---|---|---|---|
| `rental-builder-pro` | `rental123` | `ORG-BUILDER-PRO` | Specialized contract for construction tools. |
| `rental-green-land` | `rental123` | `ORG-GREEN-LAND` | Specialized contract for landscaping tools. |

## Technical Implementation

### Bulk Pricing API
This frontend demonstrates the "Best Price" bulk API. Instead of fetching prices one by one, it uses a single call to retrieve the best available price for a specific configuration:

`GET /public/api/channels/rental-channel/countries/DE/pricerows/{priceType}?pricedresourceIds={toolId}&quantity={qty}&unit={unit}`

### Price Matrix API
To show the overview of all discount tiers, the frontend uses the `all-quantities` endpoint:

`GET /public/api/channels/rental-channel/countries/DE/pricedresource/{toolId}/{priceType}/all-quantities`

This returns a list of all price rows applicable to the current user, sorted by their minimum quantity breakpoints.
