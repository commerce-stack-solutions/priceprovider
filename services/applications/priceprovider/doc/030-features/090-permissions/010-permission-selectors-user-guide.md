# Permission Selectors - Business User Guide

## Overview

Permission selectors allow you to create fine-grained access control rules that restrict which specific data records users can access. Instead of granting access to all price rows, you can limit access based on field values like currency, price type, or organization.

## Why Use Permission Selectors?

**Use Cases:**
- Restrict currency traders to only see prices in their assigned currency (EUR, USD, etc.)
- Prevent customers from seeing sensitive price types (purchase prices, material costs)
- Allow organization-specific access based on group assignments
- Create read-only access for specific channels or countries

**Benefits:**
- Improved data security through principle of least privilege
- Flexible access control without code changes
- Self-service role management for administrators
- Audit trail of who can access what data

## Permission Format

### Basic Format

```
priceprovider.<scope>:<DataType>[<selector>]:<Action>
```

**Components:**
- **scope**: `admin` for administrative access, `public` for customer-facing API
- **DataType**: The type of data (e.g., `PriceRow`, `Channel`, `Currency`)
- **selector**: Optional filter expression in square brackets `[...]`
- **Action**: `read`, `write`, or `delete`

### Examples

**Without Selector (Global Access):**
```
priceprovider.admin:PriceRow:read
```
→ Can read ALL price rows

**With Selector (Restricted Access):**
```
priceprovider.admin:PriceRow[currencyRef=='EUR']:read
```
→ Can ONLY read EUR price rows

```
priceprovider.public:PriceRow[groupRefs isEmpty AND priceType=='SALES_PRICE']:read
```
→ Can ONLY read sales prices without group assignment (typical for anonymous users)

## Selector Syntax

### Simple Comparisons

**Equality:**
```
currencyRef == 'EUR'
priceType == 'SALES_PRICE'
taxIncluded == true
```

**Inequality:**
```
priceType != 'PURCHASE_PRICE'
currencyRef != 'JPY'
```

### Collection Operators

**Check if Empty:**
```
groupRefs isEmpty
channelRefs isEmpty
```
→ True when the field is null or has no values

**Has Any (At Least One Match):**
```
channelRefs hasAny('WEBSHOP', 'RETAIL')
```
→ True if the price row is assigned to WEBSHOP OR RETAIL channel

**Has All (Must Have All):**
```
channelRefs hasAll('WEBSHOP', 'MOBILE')
```
→ True if the price row is assigned to BOTH WEBSHOP AND MOBILE channels

### Logical Operators

**AND - Both conditions must be true:**
```
currencyRef == 'EUR' AND priceType == 'SALES_PRICE'
```

**OR - At least one condition must be true:**
```
priceType == 'SALES_PRICE' OR priceType == 'RENTAL_BASE_PRICE'
```

**NOT - Negates a condition:**
```
NOT priceType == 'PURCHASE_PRICE'
```

**Parentheses for Grouping:**
```
(currencyRef == 'EUR' OR currencyRef == 'USD') AND priceType == 'SALES_PRICE'
```

## Common Scenarios

### Scenario 1: Currency-Specific Access

**Requirement:** EUR price contributor can only manage EUR prices

**Permissions Needed:**
```
priceprovider.admin:PriceRow[currencyRef=='EUR']:read
priceprovider.admin:PriceRow[currencyRef=='EUR']:write
priceprovider.admin:PriceRow[currencyRef=='EUR']:delete
```

**Result:** User can view, edit, and delete only EUR price rows. USD, JPY, and other currencies are hidden.

### Scenario 2: Anonymous Public Access

**Requirement:** Website visitors should only see public sales and rental prices without organization restrictions

**Permissions Needed:**
```
priceprovider.public:PriceRow[groupRefs isEmpty AND (priceType=='SALES_PRICE' OR priceType=='RENTAL_BASE_PRICE' OR priceType=='RENTAL_DAILY_RATE')]:read
```

**Result:** Anonymous users can only see:
- Sales prices
- Rental base prices and daily rates
- Without any group/organization assignment

### Scenario 3: Read-Only Channel Manager

**Requirement:** Channel coordinator can view prices for their channels but cannot edit

**Permissions Needed:**
```
priceprovider.admin:PriceRow[channelRefs hasAny('WEBSHOP', 'RETAIL')]:read
priceprovider.admin:Channel:read
```

**Result:** User can:
- View price rows assigned to WEBSHOP or RETAIL
- View all channels (to see channel details)
- Cannot edit or delete any data

### Scenario 4: Exclude Sensitive Price Types

**Requirement:** Customer-facing users should never see purchase prices or material costs

**Permissions Needed:**
```
priceprovider.public:PriceRow[NOT (priceType=='PURCHASE_PRICE' OR priceType=='MATERIAL_COST')]:read
```

**Result:** All price rows EXCEPT purchase prices and material costs are visible.

## Creating Roles with Selectors

### Step 1: Create Permission Entities

In the Admin UI, go to **App Permissions** and create new permissions:

1. Name: `priceprovider.admin:PriceRow[currencyRef=='EUR']:read`
2. Description: `Read EUR price rows`

Repeat for write and delete actions.

### Step 2: Create Role

In **App Roles**, create a new role:

1. Name: `priceprovider.admin:EURPriceContributor`
2. Description: `Manage EUR price rows only`
3. Assign the three permissions created above

### Step 3: Assign Role to User

In Keycloak (Identity Provider):

1. Go to the `priceprovider` realm
2. Find the user
3. Go to **Role Mappings** tab
4. Assign the `priceprovider.admin:EURPriceContributor` role

### Step 4: Test

1. Log in as the user
2. Navigate to Price Rows
3. Verify only EUR prices are visible
4. Try to create/edit a USD price → Should be denied

## Important Notes

### Multiple Permissions (Union Logic)

When a user has multiple permissions for the same data type and action, they are combined with **OR** logic.

**Example:**
User has both:
- `priceprovider.admin:PriceRow[currencyRef=='EUR']:read`
- `priceprovider.admin:PriceRow[currencyRef=='USD']:read`

**Result:** User can see EUR **OR** USD prices (union of both)

### Global Permission Overrides

If a user has **any** permission without a selector, it overrides all selector-based permissions.

**Example:**
User has:
- `priceprovider.admin:PriceRow:read` (no selector)
- `priceprovider.admin:PriceRow[currencyRef=='EUR']:read`

**Result:** User can see **ALL** price rows (global permission takes precedence)

### API Context Separation

Permissions are scoped to their API context:
- `priceprovider.admin:*` permissions only work in Admin API
- `priceprovider.public:*` permissions only work in Public API

A user with `priceprovider.admin:PriceRow:read` cannot use it to access the Public API.

### Write and Delete Checks

For write and delete operations, the system checks permissions against:
1. The **existing** object (before changes)
2. The **resulting** object (after changes)

**Example:**
User has `priceprovider.admin:PriceRow[currencyRef=='EUR']:write`

- Can edit EUR price to change price amount ✅
- **Cannot** change currency from EUR to USD ❌ (would violate permission on result)
- **Cannot** edit USD price ❌ (no permission on existing object)

## Field Reference

### PriceRow Fields

| Field | Type | Description | Example Values |
|-------|------|-------------|----------------|
| `currencyRef` | String | Currency reference | `'EUR'`, `'USD'`, `'GBP'` |
| `priceType` | Enum | Type of price | `'SALES_PRICE'`, `'PURCHASE_PRICE'`, `'RENTAL_BASE_PRICE'` |
| `taxIncluded` | Boolean | Whether tax is included | `true`, `false` |
| `groupRefs` | Collection | Organization groups | `[]`, `['ORG-001']` |
| `channelRefs` | Collection | Sales channels | `[]`, `['WEBSHOP', 'RETAIL']` |
| `unitRef` | String | Unit of measure | `'piece'`, `'kg'`, `'m'` |
| `pricedResourceId` | String | Product/resource ID | `'PROD-001'` |

### Available Price Types

- `SALES_PRICE` - Standard sales price
- `RENTAL_BASE_PRICE` - Base price for rentals
- `RENTAL_DAILY_RATE` - Daily rental rate
- `PURCHASE_PRICE` - Purchase/procurement price (sensitive)
- `MATERIAL_COST` - Material cost (sensitive)
- `LIST_PRICE` - Manufacturer's suggested price
- `PROMOTIONAL_PRICE` - Temporary promotional price

## Troubleshooting

### Problem: User Can't See Any Data

**Possible Causes:**
1. No permissions assigned to user's role
2. Selector is too restrictive (no data matches)
3. Wrong API context (using admin permission in public API)

**Solution:**
1. Check user's role assignments in Keycloak
2. Verify role has appropriate permissions
3. Test selector syntax with broader criteria first
4. Check logs for permission evaluation details

### Problem: User Sees More Data Than Expected

**Possible Causes:**
1. User has a global permission (no selector)
2. User has multiple roles with overlapping permissions
3. Selector uses OR instead of AND

**Solution:**
1. Review all permissions assigned to user
2. Remove or refine global permissions
3. Check selector logic (use parentheses for complex conditions)

### Problem: Can't Create/Edit Records

**Possible Causes:**
1. Write permission missing
2. Selector doesn't match the new/updated record
3. Trying to change fields that would violate permission

**Solution:**
1. Ensure user has write permission, not just read
2. Verify new/updated record matches selector criteria
3. Don't try to change restricted fields (e.g., currency for EUR-only user)

## Best Practices

1. **Start Broad, Then Narrow:** Begin with simple selectors and add complexity as needed
2. **Document Intent:** Use clear descriptions in permission/role names
3. **Test Thoroughly:** Create test users to verify permission behavior
4. **Use Sample Data:** Create test data in different currencies, types, etc.
5. **Audit Regularly:** Review permission assignments periodically
6. **Prefer Roles:** Assign roles to users, not individual permissions
7. **Keep It Simple:** Complex selectors are harder to maintain and understand

## Getting Help

- **Debug Logging:** Ask administrators to enable debug logging to see permission evaluation details
- **Test in Dev:** Always test new permission configurations in development first
- **Documentation:** Refer to the [Technical Guide](./020-permission-selectors-developer-guide.md) for implementation details
- **Support:** Contact your system administrator for permission-related issues
