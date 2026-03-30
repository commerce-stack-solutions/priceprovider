# API Reference – Error Codes

This document describes the error response format, HTTP status codes, and common error messages returned by the Price Provider Service API.

## Error Response Format

All API errors are returned as a JSON `ErrorResponse` object:

```json
{
  "$messages": [
    {
      "type": "ERROR",
      "messageKey": "error.unit.not.found",
      "message": "Unit with symbol 'xyz' was not found",
      "fields": ["symbol"],
      "parameters": {
        "symbol": "xyz"
      }
    }
  ]
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `$messages` | Array | List of error messages |
| `$messages[].type` | String | Message severity: `ERROR`, `WARNING`, `INFO` |
| `$messages[].messageKey` | String | Localization key for the message |
| `$messages[].message` | String | Human-readable error description |
| `$messages[].fields` | Array | Field names affected by the error (for UI highlighting) |
| `$messages[].parameters` | Object | Dynamic values interpolated into the message |

## HTTP Status Codes

| Status Code | Meaning | When Returned |
|-------------|---------|---------------|
| `200 OK` | Success | GET, PUT, PATCH, bulk operations succeeded |
| `201 Created` | Created | POST /create succeeded |
| `204 No Content` | Deleted | DELETE succeeded |
| `400 Bad Request` | Validation / Input error | Validation failed, invalid parameters, malformed query, missing required params |
| `404 Not Found` | Not found | Entity does not exist, or unknown URL |
| `409 Conflict` | Conflict | Duplicate key on create, or referential integrity violation on delete |
| `500 Internal Server Error` | Server error | Unexpected internal error |

## Exception Types and Corresponding Status Codes

| Exception Class | HTTP Status | Common Cause |
|----------------|------------|--------------|
| `NotFoundException` | 404 | Entity not found by ID or natural key |
| `EntityValidationException` | 400 | Domain validation rules violated |
| `InvalidParameterException` | 400 | Invalid query parameter value or format |
| `MissingServletRequestParameterException` | 400 | Required query parameter missing (e.g., `quantity`, `unit`, `currency` on Public Price API) |
| `QueryParseException` | 400 | Malformed `q` filter string (Lucene syntax error) |
| `QueryFilterRuntimeException` | 400 | Invalid field or value in `q` filter |
| `DataIntegrityException` | 409 | Entity still referenced by other entities |
| `EntityAlreadyExistsException` | 409 | Entity with same key already exists |
| `DataMappingException` | 400 | Error converting entity to/from REST DTO |
| `IOException` | 500 | I/O error (e.g., file read error during data initialization) |
| `Exception` (catch-all) | 500 | Unexpected unhandled error |

## Common Error Examples

### 404 – Entity Not Found

```json
{
  "$messages": [
    {
      "type": "ERROR",
      "messageKey": "error.unit.not.found",
      "message": "Unit with symbol 'xyz' was not found",
      "fields": ["symbol"]
    }
  ]
}
```

### 400 – Validation Failed

```json
{
  "id": 123,
  "priceValue": -5.00,
  "$messages": [
    {
      "type": "ERROR",
      "messageKey": "error.pricerow.value.must.be.positive",
      "message": "Price value must be positive",
      "fields": ["priceValue"]
    },
    {
      "type": "ERROR",
      "messageKey": "error.pricerow.validfrom.before.validto",
      "message": "Valid from date must be before valid to date",
      "fields": ["validFrom", "validTo"]
    }
  ]
}
```

### 400 – Missing Required Parameter

```json
{
  "$messages": [
    {
      "type": "ERROR",
      "message": "Required request parameter 'quantity' is missing. Parameter type: java.math.BigDecimal"
    }
  ]
}
```

### 400 – Query Parse Error

```json
{
  "$messages": [
    {
      "type": "ERROR",
      "message": "Query parse error at position 12: unexpected token '>>'"
    }
  ]
}
```

### 409 – Conflict (Already Exists)

```json
{
  "$messages": [
    {
      "type": "ERROR",
      "messageKey": "error.unit.already.exists",
      "message": "Unit with symbol 'piece' already exists",
      "fields": ["symbol"]
    }
  ]
}
```

### 409 – Conflict (Data Integrity)

```json
{
  "$messages": [
    {
      "type": "ERROR",
      "messageKey": "error.unit.still.referenced",
      "message": "Unit 'piece' cannot be deleted because it is still referenced by price rows",
      "fields": ["symbol"]
    }
  ]
}
```

## Message Key Conventions

Message keys follow the pattern: `error.{entity}.{description}`

| Prefix | Description |
|--------|-------------|
| `error.unit.*` | Unit entity errors |
| `error.pricerow.*` | Price row entity errors |
| `error.currency.*` | Currency entity errors |
| `error.taxclass.*` | Tax class entity errors |
| `error.language.*` | Language entity errors |
| `error.group.*` | Group entity errors |
| `error.organization.*` | Organization entity errors |
| `error.validation.*` | Generic validation errors |
| `error.reference.*` | Reference integrity errors |

## Validation Message Rules

When implementing validation, follow these rules:

1. **MUST** always provide `fields` references when the error relates to specific fields
2. **MUST NOT** include HTTP status codes in message objects for single-resource REST calls
3. **MUST NOT** include technical details, stack traces, or internal class names in user-facing messages
4. **SHOULD** provide clear, actionable messages that help the API consumer understand what to fix

For single-resource REST calls, the HTTP status code in the response header communicates the error category. Message objects communicate the domain-level details.

**Exception**: Bulk operations (`bulk-create-or-update`) MAY include a status code per item to indicate per-item success or failure within the batch.
