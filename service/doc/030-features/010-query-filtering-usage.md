# Query Filtering Usage Guide

This document explains how to use the query filtering feature in the Price Provider Service API.

## Overview

All list API endpoints support advanced filtering using a **Lucene-inspired** query syntax via the `q` parameter. The notation is deliberately optimized for **readable URL queries**: it avoids characters that require URI-percent-encoding as much as possible (no `+`, `%`, `"`, `{}` etc.), so filters remain legible in browser address bars, server logs, and Postman.

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

## Collection Membership Operators

For collection-valued reference fields (Many-to-Many / One-to-Many associations), two membership operators allow filtering by specific referenced IDs.

### `hasAny` – at least one match (OR semantics)

Returns entities whose collection contains **at least one** of the listed IDs:

```
GET /api/pricerows?q=groupRefs.hasAny:(PREMIUM,VIP)
GET /api/channels?q=allowedCountryRefs.hasAny:(DE,AT,CH)
```

### `hasAll` – all must match (AND semantics)

Returns entities whose collection contains **all** of the listed IDs:

```
GET /api/pricerows?q=groupRefs.hasAll:(PREMIUM,VIP)
GET /api/channels?q=allowedCountryRefs.hasAll:(DE,AT)
```

### Syntax rules

- Values are wrapped in parentheses and separated by commas: `field.hasAny:(ID1,ID2,ID3)`
- Whitespace around commas is trimmed: `(ID1, ID2)` is equivalent to `(ID1,ID2)`
- Values are case-sensitive
- The list must contain at least one non-empty value; an empty list `()` returns HTTP 400

### Combining with other operators

These operators compose freely with `AND`, `OR`, `NOT` and parenthesised groupings:

```
GET /api/pricerows?q=groupRefs.hasAny:(PREMIUM,VIP) AND taxIncluded:true
GET /api/pricerows?q=groupRefs.hasAll:(PREMIUM,VIP) AND priceValue:<100
GET /api/pricerows?q=NOT groupRefs.hasAll:(PREMIUM,VIP)
GET /api/channels?q=allowedCountryRefs.hasAny:(DE,AT,CH) AND priceRepresentationMode:FORCE_GROSS
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
| **Reference (Collection)** | hasAny, hasAll, exists | `groupRefs.hasAny:(A,B)`, `groupRefs.hasAll:(A,B)`, `groupRefs.exists:true` |


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
GET /api/pricerows?q=groupRefs.hasAny:(PREMIUM,VIP)
GET /api/pricerows?q=groupRefs.hasAll:(PREMIUM,VIP)
GET /api/pricerows?q=groupRefs.hasAny:(PREMIUM,VIP) AND taxIncluded:true
GET /api/pricerows?q=NOT groupRefs.hasAll:(PREMIUM,VIP)
```

### Channels

```
GET /api/channels?q=allowedCountryRefs.exists:true
GET /api/channels?q=allowedCountryRefs.hasAny:(DE,AT,CH)
GET /api/channels?q=allowedCountryRefs.hasAll:(DE,AT)
GET /api/channels?q=allowedCountryRefs.hasAny:(DE,AT) AND priceRepresentationMode:FORCE_GROSS
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

The query syntax is designed to minimise the need for percent-encoding. Most queries can be used directly in a browser address bar or as a plain URL parameter value.  Characters that *do* require encoding in strict RFC 3986 compliance:

- Space: `%20` (required in AND/OR/range expressions with spaces)
- Brackets: `[` = `%5B`, `]` = `%5D` (required in range expressions)

Characters that do **not** require encoding and can be used literally:

- Colon `:`, parentheses `()`, comma `,`, asterisk `*`, comparison operators `<>>=<=`, dot `.`, hyphen `-`, underscore `_`

Example – raw vs encoded:
```
Raw query: priceValue:[10 TO 100] AND taxIncluded:true
Encoded:   priceValue:%5B10%20TO%20100%5D%20AND%20taxIncluded:true
```

Spaces inside `hasAny`/`hasAll` value lists are not needed; use compact comma-separated IDs to avoid encoding:
```
Raw (compact, no encoding needed): groupRefs.hasAny:(PREMIUM,VIP)
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
- Empty value list in `hasAny`/`hasAll`
- `hasAny`/`hasAll` applied to a non-collection field

## Best Practices

1. **Start simple**: Test with basic queries before building complex ones
2. **Use parentheses**: Group conditions clearly to avoid ambiguity
3. **Check field names**: Refer to API documentation for exact field names
4. **Avoid unnecessary encoding**: Use compact IDs in `hasAny`/`hasAll` lists to keep URLs readable
5. **Test in Postman**: Use the provided Postman collection for examples
6. **Combine wisely**: Use with pagination and sorting for optimal results

## Limitations

- Maximum nesting depth: 10 levels
- Query string max length: Limited by HTTP standards (~2000 characters)
- Field names are case-sensitive
- Only entity fields are searchable (not computed or derived values)

### Collections and operator limitations

The query filtering implementation differentiates between single-valued references (e.g. `unitRef`, `currencyRef`, `baseUnitRef`) and collection-valued references (e.g. `subRefs`, `parentRefs`, `groupRefs`, `allowedCountryRefs`). Collection-valued paths have a constrained, deliberate operator set to avoid ambiguous semantics and to keep predicates efficient.

- Supported operators on collection-valued fields:
  - `.exists:true` — matches entities where the collection is not empty
  - `.exists:false` — matches entities where the collection is empty
  - `.hasAny:(id1,id2,...)` — matches entities whose collection contains **at least one** of the listed IDs
  - `.hasAll:(id1,id2,...)` — matches entities whose collection contains **all** of the listed IDs

- Operators not supported on collections (these will produce an error):
  - Comparison operators: `>`, `<`, `>=`, `<=`
  - Range queries: `[min TO max]`
  - Direct equality/contains checks on the collection itself (e.g. `subRefs:GROUP_A`) are not supported in the general parser-based filters
  - Attempting to access nested fields directly on a collection (e.g. `subRefs.id:GROUP_A` or `parentRefs.id:GROUP_A`) is not supported and typically results in a `fieldInvalid`/`fieldUnknown` error depending on the exact path resolution

## Additional Resources

- Postman Collection: `service/postman/pps-postmancollection.json`
- Implementation Guide: `service/doc/030-features/020-query-filtering-implementation.md`
- API Documentation: `service/doc/README.md`
