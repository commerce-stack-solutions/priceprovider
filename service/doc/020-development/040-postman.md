# Postman Collection Guide

## Purpose

The Postman collection serves two purposes:

1. **Prepared REST API calls** – ready-made requests for all endpoints for simple reuse and manual testing during development
2. **Automated testing** – the collection includes validation scripts (test assertions) that can be run via Newman; note that only a single automated run is possible without resetting data, since requests create and modify records

## Collection Location

```
service/postman/pps-postmancollection.json   ← collection with all requests and test scripts
service/postman/pps-environment.json         ← environment file with all pre-configured variables
```

## Tools

### Postman

The primary tool. Download from [postman.com](https://www.postman.com/).

### Hoppscotch (Alternative)

[Hoppscotch](https://hoppscotch.io/) is a lightweight, open-source alternative to Postman that runs in the browser. It can import and execute Postman collections and is useful when a full Postman installation is not desired.

## Importing the Collection into Postman

1. **Open Postman** (download from [postman.com](https://www.postman.com/) if needed)
2. Click **Import** in the top left
3. Select **File** and choose `service/postman/pps-postmancollection.json`
4. Import the environment: **Import** → **File** → `service/postman/pps-environment.json`
5. Select **Price Provider Service - Local** as the active environment
6. The collection is imported with all endpoints pre-configured

## Importing the Collection into Hoppscotch

1. Open [hoppscotch.io](https://hoppscotch.io/) in your browser
2. Go to **Collections** → **Import** → choose `service/postman/pps-postmancollection.json`
3. Go to **Environments** → **Import** → choose `service/postman/pps-environment.json`
4. Select **Price Provider Service - Local** as the active environment (top-right environment picker)
5. All variables are pre-configured — run the `Authentication / Get Admin Access Token` request first to populate `{{accessToken}}`

## Configuration

The collection uses the following variables (all set to local defaults):

| Variable              | Default                   | Description                                               |
|-----------------------|---------------------------|-----------------------------------------------------------|
| `baseUrl`             | `http://localhost:8080`   | Price Provider Service base URL                           |
| `keycloakUrl`         | `http://localhost:8081`   | Keycloak base URL                                         |
| `oidcRealm`           | `priceprovider`           | Keycloak realm name                                       |
| `oidcClientId`        | `priceprovider-app`       | OIDC public client ID                                     |
| `oidcUsername`        | `admin-user`              | Test user with full Admin role                            |
| `oidcPassword`        | `admin123`                | Password for the test user                                |
| `accessToken`         | *(empty)*                 | Admin JWT token — populated by the login request          |
| `customerAccessToken` | *(empty)*                 | Customer JWT token — populated by the customer login      |
| `rentalAccessToken`   | *(empty)*                 | Rental JWT token — populated by the rental login requests |

In **Postman**, these are collection variables (set on the **Variables** tab of the collection).  
In **Hoppscotch**, import `pps-environment.json` and select it as the active environment — the login requests will then populate the token variables automatically into that environment.

## Authentication (Keycloak OIDC)

The Admin API requires a valid JWT Bearer token. The collection includes an **Authentication** folder with a pre-configured login request.

### How it works

1. The `Authentication / Get Admin Access Token (Keycloak)` request performs an OIDC [Resource Owner Password Credentials](https://oauth.net/2/grant-types/password/) (ROPC) grant against Keycloak.
2. The test script extracts the returned `access_token` and stores it in the **environment variable** `{{accessToken}}` (via `pm.environment.set()`).
3. All Admin API folders inherit **Bearer `{{accessToken}}`** from the collection-level auth setting automatically.
4. The `Health` and `Public Price API` folders are marked `noauth` and need no token.
5. The `Rental (Example)` requests (inside `Public Price API`) store their tokens in `{{rentalAccessToken}}` using the same `pm.environment.set()` mechanism.

> **Hoppscotch compatibility**: The scripts use `pm.environment.set()` exclusively, which is supported in both Newman and Hoppscotch. Make sure you have an active environment selected in Hoppscotch before running the login requests.

### Prerequisites

Start the full stack including Keycloak:

```bash
docker-compose up
```

Keycloak starts on port **8081** and auto-imports the `priceprovider` realm with all sample users.

### Running with authentication

**Postman / Hoppscotch**: Run the `Authentication / Get Admin Access Token (Keycloak)` request first, then execute any other request. In Hoppscotch, ensure an active environment is selected so that `{{accessToken}}` is stored correctly.

**Newman**: Pass the `Authentication` folder first (it is the first folder in the collection, so a full run handles this automatically):

```bash
cd service
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --reporters cli,html \
  --reporter-html-export reports/newman/collection-report.html
```

To override the Keycloak credentials (e.g. different environment):

```bash
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --env-var "keycloakUrl=http://keycloak.example.com" \
  --env-var "oidcUsername=admin-user" \
  --env-var "oidcPassword=admin123" \
  --reporters cli
```

### Available test users

| Username               | Role                              | Description                    |
|------------------------|-----------------------------------|--------------------------------|
| `admin-user`           | `priceprovider.admin/Admin`       | Full access to all Admin APIs  |
| `contributor-user`     | `priceprovider.admin/Contributor` | Read + write on all entities   |
| `reader-user`          | `priceprovider.admin/Reader`      | Read-only on all entities      |
| `customer-city-council`| `priceprovider.public/PriceRowReader` | Org-scoped public prices   |
| `customer-city-health` | `priceprovider.public/PriceRowReader` | Sub-org-scoped public prices |
| `customer-techcorp`    | `priceprovider.public/PriceRowReader` | Different org public prices |

All passwords are `admin123` (dev/test only).

## Available Endpoints in the Collection

The collection covers the following entity (Units shown as example; all other entities follow the same structure):

**Units**
- `GET /admin/api/units` – Get all units (with pagination, sorting, query filtering)
- `GET /admin/api/units/{symbol}` – Get unit by symbol
- `POST /admin/api/units/create` – Create new unit
- `PUT /admin/api/units/{symbol}` – Create or replace unit (idempotent)
- `PATCH /admin/api/units/{symbol}` – Partial update (RFC 6902 JSON Patch)
- `DELETE /admin/api/units/{symbol}` – Delete unit
- `POST /admin/api/units/bulk-delete` – Bulk delete
- `POST /admin/api/units/bulk-create-or-update` – Bulk create or update
- Localized name management examples (`/name/en`, `/name/de`)

... (other entities: Currencies, Languages, TaxClasses, Groups, Organizations, PriceRows follow the same endpoint structure)

**Price Rows** (additional)
- Query filtering examples with `q` parameter
- Bulk create or update with smart field matching

**Public Price API**
- `GET /public/api/channels/.../countries/.../pricedresource/...` – Get best price for a product
- `GET /public/api/channels/.../countries/.../pricedresource/...` with group context
- `GET /public/api/channels/.../countries/.../pricedresource/.../all-candidates` – Get all matching prices
- Validation scripts that check response status and data structure

## Error Scenarios Covered

The collection includes angry-path requests to verify proper error handling:

- **400 Bad Request** – Invalid query filter syntax (e.g., unsupported operators), missing required parameters
- **404 Not Found** – Requesting a non-existent entity by ID
- **409 Conflict** – Trying to delete an entity that is still referenced by other entities

The Postman test scripts assert the expected HTTP status codes and error message keys.

## Important Notes

- **PUT vs PATCH**:
  - `PUT` performs a full replacement of the resource (idempotent recreate operation)
  - `PATCH` performs a partial update using RFC 6902 JSON Patch standard
  - Use PATCH for incremental updates to avoid unintended data loss
- All PATCH requests use `Content-Type: application/json-patch+json`
- **Data is modified** by the collection – automated runs with Newman should be run against a clean database instance to ensure deterministic results

## Running with Newman (CLI Automation)

[Newman](https://www.npmjs.com/package/newman) is the CLI runner for Postman collections. It enables automated test execution in CI/CD pipelines.

### Prerequisites

- Node.js (LTS) and npm installed

### Installation

```bash
# Install Newman and HTML reporter globally
npm install -g newman newman-reporter-html
```

### Quick Start

Run the complete collection against a local backend (requires Keycloak running on port 8081):

```bash
cd service
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --reporters cli,html \
  --reporter-html-export reports/newman/collection-report.html
```

The `Authentication` folder runs first and populates `{{accessToken}}`; all Admin API folders then inherit the Bearer token automatically.

### Run Only a Specific Folder

```bash
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --folder "Authentication" \
  --folder "Units (Admin API)" \
  --reporters cli
```

Available folders:
- `Authentication` ← always run first when testing Admin API endpoints
- `Health`
- `Units (Admin API)`, `Languages (Admin API)`, `Currencies (Admin API)`, `Price Rows (Admin API)`
- `Public Price API` ← includes `Rental (Example)` sub-folder
- `Tax Classes (Admin API)`, `Groups (Admin API)`, `Organizations (Admin API)`, `Countries (Admin API)`, `Channels (Admin API)`

### With Timeouts (for slow environments)

```bash
newman run postman/pps-postmancollection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --reporters cli,html \
  --reporter-html-export reports/newman/collection-report.html \
  --timeout-request 60000 \
  --timeout-script 30000
```

### Windows (cmd.exe)

```cmd
cd C:\Projects\priceproviderservice\service
newman run postman/pps-postmancollection.json --env-var "baseUrl=http://localhost:8080" --reporters cli,html --reporter-html-export reports/newman/collection-report.html
```

Keycloak credentials are embedded in the collection variables. Override them with additional `--env-var` flags if needed (e.g., `--env-var "oidcPassword=secret"`).

### Expected Behavior

- Happy-path requests (valid filters) return HTTP 200 and the Postman test scripts in the collection report success
- Angry-path requests (e.g., invalid query operators) return HTTP 400; test scripts validate this explicitly

## Further Resources

- Newman docs: <https://www.npmjs.com/package/newman>
- Newman HTML reporter: <https://www.npmjs.com/package/newman-reporter-html>
- Postman: <https://www.postman.com/>
- Hoppscotch: <https://hoppscotch.io/>
