# Next.js Commerce + Tyk + Keycloak + Price Provider (Infrastructure Example)

This example provides a **local infrastructure setup** to connect **Next.js Commerce (vercel/commerce)** to the Price Provider public API through **Tyk API Gateway**.

Target folder: `examples/nextjs-commerce`

## What is included

- `docker-compose.yml`
  - PostgreSQL
  - Keycloak (realm import from `idp/keycloak/realm-export.json`)
  - Price Provider Service (using existing image `price-provider-service`)
  - Redis
  - Tyk Gateway
- `tyk/apps/priceprovider-public-api.json`
  - Seeded API mapping from Tyk to Price Provider public API
- `tyk/policies/default.json`
  - Seeded default Tyk policy file
- `seed/.env.example`
  - Environment template for Next.js Commerce
- `seed/priceprovider-mapping.example.ts`
  - Mapping helper example for integrating Price Provider calls in a Next.js Commerce provider

## Architecture

```text
Next.js Commerce  ->  Tyk Gateway  ->  Price Provider Service
       |                                     |
       +---------- OIDC login via Keycloak --+
```

- Price reads are done via Tyk endpoint `http://localhost:8088/commerce/prices/...`.
- Keycloak issues tokens for authenticated users.
- Public read endpoints can be consumed anonymously; authenticated calls add organization context via `groups` claim.
- Candidate inspection endpoint (`/candidates`) remains protected and requires inspector permission.

## Start local infrastructure

From this folder:

```bash
cd examples/nextjs-commerce
docker compose up -d
```

Endpoints:

- Tyk Gateway: `http://localhost:8088`
- Price Provider Service: `http://localhost:8080`
- Keycloak: `http://localhost:8081`

> The compose file uses existing local service image `price-provider-service:0.0.0-SNAPSHOT`.
> Build it first when needed:
>
> ```bash
> cd service
> ./dockerimage-create.sh 0.0.0-SNAPSHOT
> ```

## Tyk mapping

Seeded API definition (`tyk/apps/priceprovider-public-api.json`):

- Tyk listen path: `/commerce/prices/`
- Upstream target: `http://service:8080/public/api/`

This means:

```text
GET http://localhost:8088/commerce/prices/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}?quantity=1&unit=piece&currency=EUR
```

is proxied to:

```text
GET http://service:8080/public/api/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}?quantity=1&unit=piece&currency=EUR
```

## Next.js Commerce integration (seed)

1. Copy `seed/.env.example` into your Next.js Commerce project as `.env.local`.
2. Use `seed/priceprovider-mapping.example.ts` as reference to map product/SKU requests to Price Provider public API.
3. For logged-in users, pass the Keycloak access token as `Authorization: Bearer <token>`.

## Users, passwords, groups, roles (Keycloak realm seed)

### Admin users

| Username | Password | Role |
|---|---|---|
| `super-user` | `superuser123` | `priceprovider.admin:Superuser` |
| `admin-user` | `admin123` | `priceprovider.admin:Admin` |
| `contributor-user` | `contributor123` | `priceprovider.admin:Contributor` |
| `reader-user` | `reader123` | `priceprovider.admin:Reader` |

### Commerce/public API users

| Username | Password | Role | Group scope |
|---|---|---|---|
| `customer-city-council` | `customer123` | `priceprovider.public:PriceRowReader` | `/organizations/ORG-CITY-COUNCIL` |
| `customer-city-health` | `customer123` | `priceprovider.public:PriceRowReader` | `/organizations/ORG-CITY-COUNCIL/ORG-CITY-HEALTH` |
| `customer-techcorp` | `customer123` | `priceprovider.public:PriceRowReader` | `/organizations/ORG-TECHCORP-GROUP/ORG-TECHCORP-EU` |
| `customer-city-council-inspector` | `customer123` | `priceprovider.public:PriceRowInspector` | `/organizations/ORG-CITY-COUNCIL` |

## How it works

1. **Client request**: Next.js Commerce requests a product price via Tyk.
2. **Gateway routing**: Tyk forwards the request to Price Provider public API.
3. **Auth context (optional)**:
   - Without token: anonymous public read endpoints work.
   - With token: service extracts `groups` from JWT and applies organization-scoped pricing.
4. **Protected inspection endpoint**: `/candidates` requires `priceprovider.public:PriceRow:inspect` (inspector role).

## Stop

```bash
docker compose down
```
