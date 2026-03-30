---
name: postman-collection-tester
description: 'Skill for running and maintaining the Postman collection integration tests for the Price Provider Service API'
---

# Goal
Run the Postman collection against the Price Provider Service to validate all API endpoints, or update the collection to reflect new API changes and sample data expectations.

# Prerequisites

## 1. Start the Service with the Dev Profile
The Postman collection relies on sample data that is loaded when the Spring Boot service starts with the `dev` profile.

```bash
cd service
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Wait until you see `Started PriceproviderserviceApplication` in the logs before running the collection.

The dev profile enables:
- H2 in-memory database with pre-loaded sample data (channels, groups, organizations, price rows)
- Swagger UI at `http://localhost:8080/swagger-ui.html`
- H2 console at `http://localhost:8080/h2-console`

## 2. Install Newman (CLI runner for Postman collections)

```bash
npm install -g newman newman-reporter-html
```

# Running the Collection

## Full Collection Run

```bash
cd service
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --reporters cli,html \
  --reporter-html-export reports/newman/collection-report.html
```

## Run a Specific Folder Only

```bash
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --folder "Public Price API" \
  --reporters cli
```

Available folders:
- `Health`
- `Units (Admin API)`
- `Languages (Admin API)`
- `Currencies (Admin API)`
- `Price Rows (Admin API)`
- `Public Price API`
- `Tax Classes (Admin API)`
- `Groups (Admin API)`
- `Organizations (Admin API)`
- `Countries (Admin API)`
- `Channels (Admin API)`

## With Timeouts (for slow environments)

```bash
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --reporters cli,html \
  --reporter-html-export reports/newman/collection-report.html \
  --timeout-request 60000 \
  --timeout-script 30000
```

# Important Notes

## Single-Run Design
The collection is designed for **a single automated run against a fresh database**. Requests create, update, and delete records, so re-running without restarting the service will cause failures due to changed state (e.g., already-deleted entities, already-created duplicates).

Always restart the service with the `dev` profile to reset the database before a new run.

## Sample Data Dependency
The Public Price API tests and several Admin API tests depend on specific sample data loaded at startup. The collection uses:
- Products: `DEMO-PRODUCT-001`, `DEMO-PRODUCT-002`, `DEMO-PRODUCT-003`
- Channels: `dach-sales-channel`, `euro-sales-channel`, `global-sales-channel`
- Groups: `DEMO-GROUP-STANDARD`, `DEMO-GROUP-PREMIUM`, `DEMO-GROUP-VIP`, `GRP-26-SALE-PROMOTION-*`
- Tax classes: `de-vat-full`, `de-vat-reduced`, `ch-vat-full`, `us-sales-tax`
- Countries: `DE`, `AT`, `CH`, `US`, `GB`, and others

This sample data is defined in:
```
service/src/main/resources/initialize/essential/   ← always loaded (countries, currencies, etc.)
service/src/main/resources/initialize/sample/      ← loaded when dev profile is active (sample-data-on: true)
```

Data files follow the naming convention `{EntityTypeName}.{4-digit-number}.{optional-descriptor}.json`, for example:
- `Language.0010.json`, `Currency.0010.json`, `Country.0010.json` (essential)
- `Channel.0010.json`, `Group.0010.json`, `Organization.0010.json` (sample)
- `PriceRow.0010.DEMO-PRODUCT-001.CHF.SALES_PRICE.json` … `PriceRow.0170.USD.json` (sample)

## Adapting Sample Data to Match Collection Expectations
When adding new features (e.g., channel-country pricing), update the sample data files to include the required references. For example:
- Price row data files must include `channelRefs` so they are returned by the channel-based Public Price API
- Tax class entities must include `countryRef` (mandatory field)
- Country entities must exist before they can be referenced in tax classes or channels

## Collection File Location
```
service/postman/pps-postmancollection.json
```

# Updating the Collection

When modifying or extending the collection:

1. Open the collection in Postman or edit the JSON directly
2. Ensure all request bodies for entities with mandatory fields include those fields
3. Verify that the test assertions match the expected API behavior
4. Run with Newman to confirm all tests pass on a fresh dev database

## Key Mandatory Fields per Entity (as of current version)
| Entity      | Mandatory Fields (besides ID)       |
|-------------|-------------------------------------|
| TaxClass    | `taxRate`, `countryRef`             |
| PriceRow    | `taxClassRef`, `priceValue`, `unitRef`, `currencyRef` |
| Unit        | `symbol`, `name` (with mandatory lang) |
| Country     | `isoKey`, `name` (with mandatory lang) |
| Channel     | `id`, `countryRefs`                 |
| Group       | `id`, `name`                        |

# Relevant Resources
- [Postman Collection Guide](../../../service/doc/020-development/040-postman.md) – detailed documentation on collection structure, Newman usage, and configuration
- [Public Price API Integration Guide](../../../service/doc/030-features/040-public-price-api/010-integration-guide.md)
- [Channels & Countries Business Guide](../../../service/doc/030-features/050-channels-countries/010-business-guide.md)
- Newman documentation: <https://www.npmjs.com/package/newman>
- Postman: <https://www.postman.com/>
