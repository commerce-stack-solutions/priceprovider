# Public Price API - Integration Guide

## Overview

The Public Price API provides a read-only interface for third-party services (Shop, PIM, ERP, etc.) to query price information for products, materials, or services. The API implements sophisticated price matching logic considering:

- **Date validity**: Prices are matched based on their validity period (validFrom/validTo)
- **Quantity tiers**: Different prices for different minimum quantities
- **Organization-specific pricing**: Special prices for organizations, automatically derived from the authenticated user's JWT.
- **Currency and unit**: Exact matching required
- **Price type**: Specify one price type (SALES_PRICE, PURCHASE_PRICE, or MATERIAL_COST)
- **Tax calculation**: Flexible tax handling (net, gross, or as declared)

## Base URL

```
http://localhost:8080/public/api
```

## Endpoints

### 1. Get Best Matching Price

Finds the single best matching price based on the provided criteria.

```
GET /public/api/channels/{channelId}/countries/{countryIsoKey}/pricedresource/{pricedResourceId}/{priceType}
```

**Path Parameters:**
- `channelId` (String): The unique identifier of the channel
- `countryIsoKey` (String): The ISO Alpha-2 country code
- `pricedResourceId` (String): The unique identifier of the priced resource
- `priceType` (String): The price type - one of: SALES_PRICE, PURCHASE_PRICE, MATERIAL_COST

**Required Query Parameters:**
- `quantity` (BigDecimal): The quantity to check (e.g., "10.00")
- `unit` (String): Unit symbol (e.g., "piece", "kg", "l")
- `currency` (String): Currency key (e.g., "EUR", "USD")

**Optional Parameters:**
- `$expand` (String): Comma-separated expansion paths (e.g., "$info.taxation,$includes.unit")

**Example Request:**
```
GET /public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE?quantity=15&unit=piece&currency=EUR&$expand=$info.taxation,$includes.unit
```

**Example Response:**
```json
{
  "id": 2,
  "pricedResourceId": "DEMO-PRODUCT-001",
  "priceValue": 106.99,
  "minQuantity": 10.00,
  "unitRef": "piece",
  "currencyRef": "EUR",
  "taxClassRef": "de-vat-full",
  "priceType": "SALES_PRICE",
  "validFrom": "2024-01-01T00:00:00Z",
  "validTo": null,
  "groupRefs": [],
  "taxIncluded": true,
  "$info": {
    "taxation": {
      "taxValue": 17.09,
      "taxRate": 0.19,
      "taxIncludedInfo": "included (gross)"
    },
    "originalPrice": {
      "originalPriceValue": 89.99,
      "originalTaxIncluded": false
    }
  },
  "$includes": {
    "unit": {
      "symbol": "piece",
      "name": {
        "en": "Piece",
        "de": "Stück"
      },
      "measure": "count",
      "baseUnitRef": null,
      "factor": 1.00
    }
  }
}
```

### 2. Get All Matching Prices

Returns all prices that match the criteria, ranked by priority (best match first).

```
GET /public/api/channels/{channelId}/countries/{countryIsoKey}/pricedresource/{pricedResourceId}/{priceType}/all-candidates
```

**Parameters:** Same as "Get Best Matching Price" (path parameters and query parameters)

**Example Response:**
```json
{
  "items": [
    {
      "id": 2,
      "pricedResourceId": "DEMO-PRODUCT-001",
      "priceValue": 106.99,
      "minQuantity": 10.00,
      "unitRef": "piece",
      "currencyRef": "EUR",
      "taxIncluded": true,
      ...
    },
    {
      "id": 1,
      "pricedResourceId": "DEMO-PRODUCT-001",
      "priceValue": 118.99,
      "minQuantity": 1.00,
      "unitRef": "piece",
      "currencyRef": "EUR",
      "taxIncluded": true,
      ...
    }
  ]
}
```

## Price Selection Priority

When multiple prices match your criteria, the API uses the following priority to select the best match:

1. **Organization Context** - Prices assigned to the user's organization (derived from JWT) win
2. **Group Distance** - Prices assigned to closer groups in the hierarchy win
   - Direct group assignment beats parent group
   - Parent group beats grandparent group
   - Group-specific prices beat generic prices
3. **Valid Date** - More recent prices (newer validFrom) win
4. **Quantity Tier** - Higher minQuantity (closer to requested quantity) wins

For detailed information about the internal architecture and how to customize the price determination logic, see the [Developer Guide](./020-developer-guide.md).

## Price Matching Algorithm

The API uses a sophisticated matching algorithm that considers multiple criteria:

### 1. Date Range Matching

Prices are checked against the current date (or reference date):
- `validFrom` must be <= current date (or null)
- `validTo` must be >= current date (or null)

### 2. Quantity Matching

The requested quantity must be >= price's `minQuantity`. When multiple prices match:
- **Nearest minQuantity wins** (higher minQuantity is better)
- Example: For quantity=15, a price with minQuantity=10 beats minQuantity=1

### 3. Currency and Unit Matching

- **Exact match required** for both currency and unit
- No automatic conversion is performed

### 4. Price Type Matching

The `priceType` path parameter specifies which price type to query:
- **SALES_PRICE** - Selling prices for customers
- **PURCHASE_PRICE** - Buying prices from suppliers
- **MATERIAL_COST** - Internal material costs

Only prices matching the specified type are considered.

### 5. Group Hierarchy

When an organization is identified via JWT:
- Prices assigned to that organization's group (or parent groups) are considered
- **Group-specific prices beat generic prices** (no group assignment)
- Parent group hierarchy is automatically traversed using a single SQL recursive CTE query
- Each group in the hierarchy gets a **distance level**:
  - Level 0 = exact group match
  - Level 1 = direct parent
  - Level 2 = grandparent
  - etc.

### 6. Priority Ranking

When multiple prices match all criteria, they are ranked by:
1. **Group Distance Level** (lowest > highest) - Prices assigned to nearer groups win
2. **Nearest validFrom** (more recent > older)
3. **Nearest minQuantity** (higher > lower)

**Note:** Currency, Unit, and PriceType are pre-filtered at the database level, so all candidates match these criteria.

## Taxation Modes

The taxation mode (net/gross) is determined by the channel configuration.

### FORCE_GROSS

Returns prices **with tax included**:
- If original price is net: calculates gross price
- If original price is gross: returns as-is
- `taxIncluded` field in response = `true`

### FORCE_NET

Returns prices **without tax**:
- If original price is net: returns as-is
- If original price is gross: calculates net price
- `taxIncluded` field in response = `false`

## Expansion (`$expand`)

The `$expand` parameter controls which optional fields are included in the response:

### Available Expansion Paths

**Info Section:**
- `$info` - All info metadata
- `$info.taxation` - Tax calculation details
- `$info.originalPrice` - Original price before conversion

**Includes Section:**
- `$includes` - All related entities
- `$includes.unit` - Full unit details
- `$includes.currency` - Full currency details
- `$includes.taxClass` - Full tax class details

**Special Value:**
- `all` - Expands everything

### Examples

```
?$expand=$info.taxation
?$expand=$includes.unit,$includes.currency
?$expand=$info,$includes
?$expand=all
```

## Response Status Codes

- **200 OK** - Request successful, price found
- **404 Not Found** - No matching price OR invalid resource/unit/currency/priceType
- **400 Bad Request** - Invalid request parameters

## Integration Examples

### Example 1: Basic Price Query (Sales Price)

**Scenario:** Get sales price for 5 pieces of DEMO-PRODUCT-001 in EUR

```bash
curl -X GET "http://localhost:8080/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE?quantity=5&unit=piece&currency=EUR"
```

### Example 2: Organization-Specific Pricing (via JWT)

**Scenario:** Get organization contract sales price

```bash
curl -H "Authorization: Bearer <JWT>" \
     -X GET "http://localhost:8080/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE?quantity=15&unit=piece&currency=EUR"
```

## Error Handling

### No Matching Price Found

```json
{
  "timestamp": "2024-01-27T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No matching price found for the given criteria",
  "path": "/public/api/channels/dach-sales-channel/countries/DE/pricedresource/INVALID-PRODUCT/SALES_PRICE"
}
```

## Best Practices

1. **Cache prices appropriately** - Prices change infrequently, consider caching with TTL
2. **Use authenticated context** when available - Provides better pricing for organizations
3. **Request minimal expansion** - Only expand fields you need for better performance
4. **Handle 404 gracefully** - No price found is a normal business case
5. **Use `all` for price tiers** - Shows customers volume discounts
6. **Choose the correct priceType** - Use SALES_PRICE for selling, PURCHASE_PRICE for procurement, MATERIAL_COST for costing

## Swagger Documentation

Interactive API documentation is available at:
```
http://localhost:8080/swagger-ui.html
```
