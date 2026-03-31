# Postman Collection Guide

## Purpose

The Postman collection serves two purposes:

1. **Prepared REST API calls** – ready-made requests for all endpoints for simple reuse and manual testing during development
2. **Automated testing** – the collection includes validation scripts (test assertions) that can be run via Newman; note that only a single automated run is possible without resetting data, since requests create and modify records

## Collection Location

```
service/postman/pps-postmancollection.json
```

## Tools

### Postman

The primary tool. Download from [postman.com](https://www.postman.com/).

### Hoppscotch (Alternative)

[Hoppscotch](https://hoppscotch.io/) is a lightweight, open-source alternative to Postman that runs in the browser. It can import and execute Postman collections and is useful when a full Postman installation is not desired.

The collection uses **OAuth 2.0 (Resource Owner Password Credentials)** auth so that Hoppscotch can handle token acquisition natively — no script execution or separate token-fetch step is needed. When you open an Admin API request in Hoppscotch, the OAuth 2.0 settings (token URL, client ID, username, password) are pre-populated from the collection; click **Get Access Token** to fetch and attach the Bearer token.

## Importing the Collection into Postman

1. **Open Postman** (download from [postman.com](https://www.postman.com/) if needed)
2. Click **Import** in the top left
3. Select **File** and choose `service/postman/pps-postmancollection.json`
4. The collection is imported with all endpoints pre-configured

## Configuration

The collection uses the following variables (all set to local defaults):

| Variable           | Default                   | Description                                                  |
|--------------------|---------------------------|--------------------------------------------------------------|
| `baseUrl`          | `http://localhost:8080`   | Price Provider Service base URL                              |
| `keycloakUrl`      | `http://localhost:8081`   | Keycloak base URL                                            |
| `oidcRealm`        | `priceprovider`           | Keycloak realm name                                          |
| `oidcClientId`     | `priceprovider-app`       | OIDC public client ID (used by Admin API auth)               |
| `oidcUsername`     | `admin-user`              | Test user with full Admin role                               |
| `oidcPassword`     | `admin123`                | Password for the test user                                   |
| `accessToken`      | *(empty)*                 | Admin JWT — populated by the Authentication folder requests  |
| `customerAccessToken` | *(empty)*              | Customer JWT — populated by the Authentication folder        |
| `rentalAccessToken`   | *(empty)*              | Rental JWT — populated by the Rental token requests          |

To customize, click on the collection name and go to the **Variables** tab.

## Authentication (Keycloak OIDC)

The Admin API requires a valid JWT Bearer token. The collection uses **OAuth 2.0 (Resource Owner Password Credentials grant)** at the collection level so tokens are handled natively by the client tool.

### How it works

The collection auth is configured as OAuth 2.0 ROPC with the Keycloak token endpoint and the admin credentials from the collection variables (`{{oidcUsername}}`, `{{oidcPassword}}`). Each folder or request that needs a different identity has its own OAuth 2.0 configuration:

| Scope | OAuth 2.0 config | Token variable |
|-------|-----------------|----------------|
| All Admin API folders (inherit collection) | `oidcClientId` / `oidcUsername` / `oidcPassword` | `{{accessToken}}` |
| Torque authenticated request (Public Price API) | `oidcClientId` / `customer-city-health` / `customer123` | `{{customerAccessToken}}` |
| Rental (Example) folder (Public Price API) | `rentalfrontend` / `rental-builder-pro` / `rental123` | `{{rentalAccessToken}}` |

The `Health` and `Public Price API` folders are marked `noauth` — they need no token (the Rental sub-folder overrides this with its own OAuth 2.0 config).

The **Authentication** folder contains explicit token-fetch requests that store tokens in the relevant variables. These are used automatically by Newman (which runs the collection sequentially) and are also available as manual helpers in Postman and Hoppscotch.

### Prerequisites

Start the full stack including Keycloak:

```bash
docker-compose up
```

Keycloak starts on port **8081** and auto-imports the `priceprovider` realm with all sample users.

### Running with authentication

**Hoppscotch**: When you open an Admin API request, Hoppscotch shows the pre-configured OAuth 2.0 settings. Click **Generate Token** (or equivalent) to fetch the Bearer token and send the request. The token URL, client ID, username, and password are all pre-filled from the collection.

**Postman**: Same as Hoppscotch — open the authorization tab for a request or the collection, click **Get New Access Token**, and use the pre-filled ROPC configuration.

**Newman**: Run the full collection. The `Authentication` folder executes first and stores tokens in the `{{accessToken}}`, `{{customerAccessToken}}`, and `{{rentalAccessToken}}` variables which are then used by the OAuth 2.0 auth configs of subsequent requests:

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

| Username               | Role                              | Password     | Description                    |
|------------------------|-----------------------------------|--------------|--------------------------------|
| `admin-user`           | `priceprovider.admin/Admin`       | `admin123`   | Full access to all Admin APIs  |
| `contributor-user`     | `priceprovider.admin/Contributor` | `admin123`   | Read + write on all entities   |
| `reader-user`          | `priceprovider.admin/Reader`      | `admin123`   | Read-only on all entities      |
| `customer-city-council`| `priceprovider.public/PriceRowReader` | `customer123` | Org-scoped public prices   |
| `customer-city-health` | `priceprovider.public/PriceRowReader` | `customer123` | Sub-org-scoped public prices |
| `customer-techcorp`    | `priceprovider.public/PriceRowReader` | `customer123` | Different org public prices |
| `rental-builder-pro`   | *(rental tenant)*                 | `rental123`  | Rental company A (client: `rentalfrontend`) |
| `rental-green-land`    | *(rental tenant)*                 | `rental123`  | Rental company B (client: `rentalfrontend`) |

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

The `Authentication` folder runs first and populates `{{accessToken}}`, `{{customerAccessToken}}`, and `{{rentalAccessToken}}`; all Admin API folders then use the stored tokens via their OAuth 2.0 auth configurations automatically.

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
- `Public Price API` (includes the `Rental (Example)` subfolder)
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
