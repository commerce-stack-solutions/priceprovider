# API Reference – General Concepts

This document describes the general REST API concepts used throughout the Price Provider Service.

## Base URL

All admin endpoints are served under:

```
/api/admin/
```

The public price API is served under:

```
/public/api/
```

## Typical Calls for an Entity

For every managed entity (e.g., `units`, `currencies`, `pricerows`, `groups`, `taxclasses`, `languages`, `organizations`) the following standard operations are available:

| Method   | URL                                              | Description                                                            |
|----------|--------------------------------------------------|------------------------------------------------------------------------|
| `GET`    | `/api/admin/{entity-name-plural}/`               | Get paginated list of entities                                         |
| `GET`    | `/api/admin/{entity-name-plural}/{id}`           | Get entity by ID                                                       |
| `DELETE` | `/api/admin/{entity-name-plural}/{id}`           | Delete entity by ID                                                    |
| `PUT`    | `/api/admin/{entity-name-plural}/{id}`           | Create or recreate entity (idempotent, full replacement)               |
| `PATCH`  | `/api/admin/{entity-name-plural}/{id}`           | Partial update using RFC 6902 JSON Patch                               |
| `POST`   | `/api/admin/{entity-name-plural}/create`         | Create a new entity                                                    |
| `POST`   | `/api/admin/{entity-name-plural}/bulk-delete`    | Bulk delete the specified entities (max. 100)                          |
| `POST`   | `/api/admin/{entity-name-plural}/bulk-create-or-update` | Bulk create or update entities (max. 100)                     |

### Examples

```
GET  /api/admin/units/
GET  /api/admin/units/piece
DELETE /api/admin/units/piece
PUT  /api/admin/units/piece
PATCH /api/admin/units/piece
POST /api/admin/units/create
POST /api/admin/units/bulk-delete
POST /api/admin/units/bulk-create-or-update
```

## GET – Query Parameters

### Pagination and Sorting

| Parameter    | Default | Description                      |
|-------------|---------|----------------------------------|
| `page`       | `0`     | Page number (0-based)            |
| `page-size`  | `20`    | Items per page                   |
| `sort-by`    | entity-specific | Field(s) to sort by      |
| `sort-direction` | `asc` | Sort direction: `asc` or `desc` |

### Response Expansion Parameters

| Parameter  | Description |
|------------|-------------|
| `$expand`  | Comma-separated list of expansion paths to include in the response (e.g., `$info.taxation,$includes.unit`) |
| `$include` | Alternative to `$expand` for related entity inclusion |
| `$meta`    | Include entity metadata: identity fields, mandatory fields, and enum values (see below) |
| `$messages`| Request message fields (validation results, warnings) |

### `$meta` – Entity Metadata

When `$meta` is included in `$expand`, the response contains a `$meta` block with structural information about the entity:

| Field           | Type              | Description |
|-----------------|-------------------|-------------|
| `identityFields`| `string[]`        | Fields that uniquely identify this entity (from `@Id` annotation) |
| `mandatoryFields`| `string[]`       | Fields that are required when creating or updating the entity |
| `enumValues`    | `{ [field]: string[] }` | All allowed values for every enum-typed field (both mandatory and optional) |

**Example request:**

```
GET /api/admin/groups/?$expand=$meta
GET /api/admin/groups/GRP-001?$expand=$includes,$info,$meta
```

**Example `$meta` response:**

```json
"$meta": {
  "identityFields": ["id"],
  "mandatoryFields": ["id", "name", "groupType"],
  "enumValues": {
    "groupType": ["ORGANIZATION", "PROMOTION"]
  }
}
```

The `$meta` expand is supported by all admin entity endpoints (groups, organizations, units, currencies, languages, countries, channels, taxclasses, pricerows).

`enumValues` always lists every enum constant for every enum field — regardless of whether that field is mandatory — so front-end components can always populate a selector without additional requests.

### Query Filtering

| Parameter | Description |
|-----------|-------------|
| `q`       | Lucene-like query filter string (e.g., `q=currency:EUR AND priceValue:>10`) |

For full query syntax documentation, see [010-query-filtering-usage.md](../030-features/010-query-filtering-usage.md).

### Example GET List Request

```
GET /api/admin/pricerows/?page=0&page-size=10&sort-by=priceValue&sort-direction=desc&q=currency:EUR&$expand=$info,$meta
```

## How GET Works

- Returns a paginated list of entities (list endpoints) or a single entity (by-ID endpoints)
- Supports optional query filtering via the `q` parameter
- Supports optional response expansion via `$expand`
- Returns `200 OK` on success, `404 Not Found` if entity does not exist (by-ID)

## How PUT Works

`PUT` is an **idempotent create-or-replace** operation:

- If the entity with the given ID does not exist → creates it
- If the entity already exists → **fully replaces** it (all fields are overwritten)
- The ID in the URL path takes precedence over any ID in the request body
- Returns `200 OK` with the resulting entity

```
PUT /api/admin/units/piece
Content-Type: application/json

{
  "symbol": "piece",
  "measure": "quantity",
  "factor": 1.0,
  "name": { "en": "piece", "de": "Stück" }
}
```

## How PATCH Works (RFC 6902 JSON Patch)

`PATCH` performs a **partial update** using [RFC 6902 JSON Patch](https://tools.ietf.org/html/rfc6902):

- Only the specified fields are modified; all other fields remain unchanged
- Content-Type: `application/json-patch+json`
- Operations: `add`, `remove`, `replace`, `move`, `copy`, `test`
- Returns `200 OK` with the updated entity
- Returns `404 Not Found` if entity does not exist

```
PATCH /api/admin/units/piece
Content-Type: application/json-patch+json

[
  { "op": "replace", "path": "/measure", "value": "count" },
  { "op": "add", "path": "/name/fr", "value": "pièce" }
]
```

**Important**: Use PATCH for incremental updates to avoid accidentally overwriting fields not included in the request (which would happen with PUT).

## How POST Works (Create and Bulk Operations)

### Create (`POST /create`)

Creates a new entity. The ID is assigned by the server (or can be provided for natural-key entities).

- Returns `201 Created` with the created entity on success
- Returns `400 Bad Request` with validation errors if the entity is invalid
- Returns `409 Conflict` if the entity already exists (for natural-key entities)

```
POST /api/admin/units/create
Content-Type: application/json

{
  "symbol": "kg",
  "measure": "mass",
  "factor": 1.0,
  "name": { "en": "kilogram", "de": "Kilogramm" }
}
```

### Bulk Create or Update (`POST /bulk-create-or-update`)

Creates or updates multiple entities in a single request (max. 100 items).

- **Entities with a natural business key** (Units, Currencies, Languages, TaxClasses, Groups, Organizations): matched by their natural key (`symbol`, `currencyKey`, etc.) – if the key exists the entity is updated, otherwise created
- **PriceRow** (no unique natural key): supports **smart matching** – if no `id` is provided, the system searches for an existing row with the same combination of business fields (`pricedResourceId`, `minQuantity`, `unitRef`, `currencyRef`, `taxClassRef`, `priceType`, `validFrom`, `validTo`, `groupRefs`); if found it updates, otherwise creates a new row
- Returns `200 OK` / `207 Multi-Status` with the list of resulting entities; per-item error messages are embedded in the response
- Returns `400 Bad Request` if the batch exceeds the limit

For a detailed overview of matching strategies per entity, see [030-bulk-update-pricerows.md](../030-features/030-bulk-update-pricerows.md#bulk-create-or-update-matching-strategy-per-entity).

```
POST /api/admin/pricerows/bulk-create-or-update
Content-Type: application/json

[
  {
    "pricedResourceId": "PROD-001",
    "priceValue": 99.99,
    "minQuantity": 1,
    "unitRef": "piece",
    "currencyRef": "EUR",
    "priceType": "SALES_PRICE"
  }
]
```

### Bulk Delete (`POST /bulk-delete`)

Deletes multiple entities by their IDs in a single request (max. 100 items).

- Accepts a JSON array of IDs
- Returns `200 OK` (even if some IDs were not found)
- Returns `400 Bad Request` if the batch exceeds the limit

```
POST /api/admin/pricerows/bulk-delete
Content-Type: application/json

[1, 2, 3, 42]
```

## How DELETE Works

`DELETE` removes a single entity by its ID:

- Returns `204 No Content` on success
- Returns `404 Not Found` if entity does not exist
- Returns `409 Conflict` if entity is still referenced by other entities (referential integrity)

```
DELETE /api/admin/units/piece
```

## Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON specification:

```
http://localhost:8080/v3/api-docs
```
