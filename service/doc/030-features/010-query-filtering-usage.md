# Query Filtering Usage Guide

This document explains how to use the query filtering feature in the Price Provider Service API.

## Overview

All list API endpoints support advanced filtering using a Lucene-like query syntax via the `q` parameter. This allows you to filter results by any entity field using simple or complex queries.

## Basic Syntax

### Simple Field Matching

```
GET /api/languages?q=active:true
GET /api/currencies?q=currencyKey:EUR
GET /api/units?q=measure:length
```

### String Contains

Use wildcards (`*`) for substring matching on both string and enum fields:

```
GET /api/organizations?q=name:*company*
GET /api/groups?q=name:*admin*
GET /api/groups?q=groupType:*ORG*
```

**Note:** Wildcards are required for substring matching. Without wildcards, the system performs exact matching.

## Comparison Operators

### Numeric Comparisons

```
GET /api/pricerows?q=priceValue:>100
GET /api/pricerows?q=priceValue:<50
GET /api/pricerows?q=priceValue:>=10
GET /api/taxclasses?q=taxRate:<=0.20
```

### Range Queries

Use `[min TO max]` syntax for range queries:

```
GET /api/pricerows?q=priceValue:[10 TO 100]
GET /api/taxclasses?q=taxRate:[0.15 TO 0.20]
GET /api/units?q=factor:[0.001 TO 1]
GET /api/units?q=factor:[* TO 0.1]
GET /api/units?q=factor:[0.1 TO *]
```

### Date/DateTime Ranges

```
GET /api/pricerows?q=validFrom:[2024-01-01T00:00:00Z TO 2024-12-31T23:59:59Z]
```

## Existence Checks

Check if a field has a value (is not null) or if a collection is not empty:

```
GET /api/units?q=baseUnitRef.exists:true
GET /api/units?q=baseUnitRef.exists:false
GET /api/groups?q=subRefs.exists:true
GET /api/organizations?q=parentRefs.exists:false
GET /api/pricerows?q=groupRefs.exists:true
```

## Logical Operators

### AND Operator

Combine multiple conditions that must all be true:

```
GET /api/languages?q=active:true AND mandatory:true
GET /api/pricerows?q=currency:EUR AND taxIncluded:true AND priceValue:>10
```

### OR Operator

Match any of multiple conditions:

```
GET /api/currencies?q=currencyKey:EUR OR currencyKey:USD OR currencyKey:GBP
GET /api/units?q=measure:length OR measure:mass
```

### NOT Operator

Exclude results matching a condition:

```
GET /api/languages?q=NOT active:false
GET /api/pricerows?q=NOT currency:EUR
```

### Combining Operators with Parentheses

Group conditions for complex logic:

```
GET /api/pricerows?q=(currency:EUR OR currency:USD) AND priceValue:[10 TO 100]
GET /api/units?q=(measure:length OR measure:mass) AND factor:>0.1
GET /api/languages?q=(active:true OR mandatory:true) AND NOT isoKey:deprecated
GET /api/groups?q=(subRefs.exists:true OR parentRefs.exists:true) AND name.exists:true
```

## Supported Data Types

| Data Type | Operators | Example |
|-----------|-----------|---------|
| **String** | equals, contains, exists | `name:value`, `name:*partial*`, `name.exists:true` |
| **Boolean** | equals, exists | `active:true`, `active.exists:true` |
| **Enum** | equals, contains (with wildcards), exists | `groupType:ORGANIZATION`, `groupType:*ORG*`, `groupType.exists:true` |
| **Number** | equals, >, <, >=, <=, range, exists | `price:100`, `price:>50`, `price:[10 TO 100]` |
| **Date/DateTime** | equals, >, <, >=, <=, range, exists | `validFrom:[2024-01-01T00:00:00Z TO 2024-12-31T23:59:59Z]` |
| **Reference (Single)** | equals, exists | `baseUnit:kg`, `baseUnit.exists:true` |
| **Reference (Collection)** | contains, exists | `groups.exists:true`, `subs.exists:false` |


## Combining with Other Parameters

Query filtering works alongside existing API parameters:

```
GET /api/units?q=measure:length&page=0&page-size=10&sort-by=symbol&sort-direction=asc
GET /api/pricerows?q=currency:EUR&$expand=$includes.unit,$includes.currency
```

### Parameters Compatibility

- `page`: Page number (0-based)
- `page-size`: Results per page
- `sort-by`: Field(s) to sort by
- `sort-direction`: `asc` or `desc`
- `$expand`: Include related entities
- `q`: Query filter (this feature)

## Entity-Specific Examples

### Languages

```
GET /api/languages?q=active:true
GET /api/languages?q=active:true AND mandatory:true
GET /api/languages?q=isoKey:en OR isoKey:de OR isoKey:fr
```

### Units

```
GET /api/units?q=measure:length
GET /api/units?q=factor:[0.001 TO 1]
GET /api/units?q=baseUnitRef.exists:true AND measure:length
GET /api/units?q=(measure:length OR measure:mass) AND factor:>0.1
```

### Currencies

```
GET /api/currencies?q=currencyKey:EUR
GET /api/currencies?q=currencyKey:EUR OR currencyKey:USD OR currencyKey:GBP
```

### Tax Classes

```
GET /api/taxclasses?q=taxRate:>0.15
GET /api/taxclasses?q=taxRate:[0.15 TO 0.20]
GET /api/taxclasses?q=taxClass:standard
```

### Price Rows

```
GET /api/pricerows?q=priceValue:[10 TO 100]
GET /api/pricerows?q=currency:EUR AND taxIncluded:true
GET /api/pricerows?q=priceValue:>50 AND currency:EUR AND taxIncluded:true
GET /api/pricerows?q=unitRef.exists:true AND currencyRef.exists:true
GET /api/pricerows?q=groupRefs.exists:true AND priceValue:<100
```

### Groups

```
GET /api/groups?q=name:*admin*
GET /api/groups?q=subRefs.exists:true
GET /api/groups?q=(subRefs.exists:true OR parentRefs.exists:true)
GET /api/groups?q=groupType:ORGANIZATION
GET /api/groups?q=groupType:*ORG*
GET /api/groups?q=groupType:*PROMO*
```

### Organizations

```
GET /api/organizations?q=name:*company*
GET /api/organizations?q=subRefs.exists:true AND name.exists:true
GET /api/organizations?q=parentRefs.exists:false
```

## URL Encoding

Special characters in query strings must be URL-encoded:

- Space: `%20`
- Colon: `%3A`
- Plus: `%2B`
- Parenthesis: `(` = `%28`, `)` = `%29`
- Brackets: `[` = `%5B`, `]` = `%5D`

Example:
```
Raw query: active:true AND name:*test*
Encoded:   active%3Atrue%20AND%20name%3A*test*
```

## Error Handling

Invalid queries return HTTP 400 with an error response:

```json
{
  "messages": [{
    "type": "ERROR",
    "messageKey": "common.errors.query.fieldUnknown",
    "fields": ["invalidFieldName"]
  }]
}
```

Common errors:
- Unknown field name
- Invalid syntax
- Malformed range expression
- Unclosed parentheses

## Best Practices

1. **Start simple**: Test with basic queries before building complex ones
2. **Use parentheses**: Group conditions clearly to avoid ambiguity
3. **Check field names**: Refer to API documentation for exact field names
4. **URL encode**: Always encode special characters in query parameters
5. **Test in Postman**: Use the provided Postman collection for examples
6. **Combine wisely**: Use with pagination and sorting for optimal results

## Limitations

- Maximum nesting depth: 10 levels
- Query string max length: Limited by HTTP standards (~2000 characters)
- Field names are case-sensitive
- Only entity fields are searchable (not computed or derived values)

### Collections and operator limitations

The query filtering implementation differentiates between single-valued references (e.g. `unitRef`, `currencyRef`, `baseUnitRef`) and collection-valued references (e.g. `subRefs`, `parentRefs`, `groupRefs`). Collection-valued paths have a constrained, deliberate operator set to avoid ambiguous semantics and to keep predicates efficient.

- Supported operators on collection-valued fields:
  - `.exists:true` — matches entities where the collection is not empty
  - `.exists:false` — matches entities where the collection is empty

- Operators not supported on collections (these will produce an error):
  - Comparison operators: `>`, `<`, `>=`, `<=`
  - Range queries: `[min TO max]`
  - Direct equality/contains checks on the collection itself (e.g. `subRefs:GROUP_A`) are not supported in the general parser-based filters
  - Attempting to access nested fields directly on a collection (e.g. `subRefs.id:GROUP_A` or `parentRefs.id:GROUP_A`) is not supported and typically results in a `fieldInvalid`/`fieldUnknown` error depending on the exact path resolution

## Additional Resources

- Postman Collection: `service/postman/pps-postmancollection.json`
- Implementation Guide: `service/doc/030-features/020-query-filtering-implementation.md`
- API Documentation: `service/doc/README.md`
