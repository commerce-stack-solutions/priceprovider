# Permission Selector Implementation - Technical Guide

## Overview

This document describes the permission selector implementation for object-level authorization in the priceprovider service. The feature allows permissions to include field-based filters (selectors) to restrict access to specific object instances.

## Architecture

### Core Components

**Package**: `io.commercestacksolutions.commons.permissionselector`

1. **SelectorParser** - Parses selector strings into expression trees
2. **SelectorEvaluator** - Evaluates selector expressions against object instances
3. **PermissionNameParser** - Extracts datatype, selector, and action from permission names
4. **PermissionMatcher** - Matches objects against user's effective permissions
5. **AuthorizationContext** - Provides current user's permissions and org filter

### Permission Name Format

```
priceprovider.admin:<DataType>[<selector>]:<Action>
```

**Examples**:
- `priceprovider.admin:PriceRow:read` - Global permission (no selector)
- `priceprovider.admin:PriceRow[currencyRef=='EUR']:read` - Selector-based permission
- `priceprovider.admin:PriceRow[currencyRef=='EUR' AND priceType=='SALES_PRICE']:write`

### Supported Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equality | `currencyRef == 'EUR'` |
| `!=` | Inequality | `priceType != 'PURCHASE_PRICE'` |
| `hasAny('a','b')` | At least one match | `channelRefs hasAny('ch1','ch2')` |
| `hasAll('a','b')` | All must match | `channelRefs hasAll('ch1','ch2')` |
| `isEmpty` | Null/empty check | `groupRefs isEmpty` |
| `AND` | Logical AND | `field1=='a' AND field2=='b'` |
| `OR` | Logical OR | `field1=='a' OR field2=='b'` |
| `NOT` | Negation | `NOT priceType=='PURCHASE_PRICE'` |
| `( ... )` | Grouping | `(a OR b) AND c` |

### Operator Precedence

1. Parentheses `( ... )`
2. Comparison/functions (`==`, `!=`, `hasAny`, `hasAll`, `isEmpty`)
3. `NOT`
4. `AND`
5. `OR`

## Supported Field Types

- ✅ String fields
- ✅ Boolean fields
- ✅ Enum fields (compared by string name)
- ✅ Referenced entities (single) - compared by `@ReferenceKey` or `@Id`
- ✅ Referenced entity collections - compared by `@ReferenceKey` or `@Id` of elements
- ❌ Transient/computed fields
- ❌ Complex structured types

## Implementation Status

### ✅ Completed

1. **Core parsing and evaluation infrastructure**
   - SelectorTokenizer, SelectorParser, SelectorExpression AST
   - SelectorEvaluator with reflection-based field access
   - Comprehensive unit tests (48 tests, all passing)

2. **Permission management**
   - PermissionNameParser for extracting components
   - AppPermissionSelectorValidationRule for validating syntax
   - Backwards compatibility with non-selector permissions

3. **Authorization infrastructure**
   - PermissionMatcher service for object-level checks
   - AuthorizationContext for current user context
   - Debug logging for permission decisions

4. **Test data**
   - Example selector-based permissions (EUR, USD, sales/rental)
   - Example roles (EURPriceContributor, AnonymousUser, etc.)

### 🚧 In Progress / TODO

1. **Object-level authorization integration**
   - Integrate PermissionMatcher into PriceRow service methods
   - Add authorization checks for create/update/delete operations

2. **List/search filtering**
   - Create PermissionFilterBuilder to convert selectors to JPA Specifications
   - Integrate into PriceRowService.findAll() methods
   - Handle union logic (multiple permissions = OR)

3. **Frontend updates**
   - Add $meta support for instance-level permissions
   - Update detail/edit views to enable/disable actions
   - Handle mixed permissions in list views

4. **Testing and documentation**
   - Postman collection updates with selector test cases
   - Business user guide for selector syntax
   - Performance testing

## Usage Examples

### Creating Selector-Based Permissions

```json
{
  "name": "priceprovider.admin:PriceRow[currencyRef=='EUR']:read",
  "description": "Read EUR price rows"
}
```

### Creating Roles with Selectors

```json
{
  "name": "priceprovider.admin:EURPriceContributor",
  "description": "Manage EUR price rows only",
  "permissionRefs": [
    "priceprovider.admin:PriceRow[currencyRef=='EUR']:read",
    "priceprovider.admin:PriceRow[currencyRef=='EUR']:write",
    "priceprovider.admin:PriceRow[currencyRef=='EUR']:delete"
  ]
}
```

### Using PermissionMatcher (Service Layer)

```java
@Autowired
private PermissionMatcher permissionMatcher;

@Autowired
private AuthorizationContext authorizationContext;

public void updatePriceRow(String id, PriceRowEntity updates) {
    PriceRowEntity existing = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("PriceRow not found"));

    // Check write permission against existing object
    Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
    if (!permissionMatcher.hasAccess(permissions, "PriceRow", "write", existing)) {
        throw new AccessDeniedException("No permission to update this price row");
    }

    // Apply updates
    // ...

    // Check permission against resulting object (after updates)
    if (!permissionMatcher.hasAccess(permissions, "PriceRow", "write", existing)) {
        throw new AccessDeniedException("Updates would violate permission constraints");
    }

    repository.save(existing);
}
```

## Acceptance Criteria Status

| AC# | Criteria | Status |
|-----|----------|--------|
| 1 | Backwards compatibility | ✅ Complete |
| 2 | Selector parsing + validation | ✅ Complete |
| 3 | Object-level authorization | 🚧 Infrastructure ready, integration pending |
| 4 | List/search authorization | 🚧 TODO |
| 5 | Multiple permissions (union) | ✅ Complete |
| 6 | Deterministic evaluation | ✅ Complete |
| 7 | Fail-safe behavior | ✅ Complete |
| 8 | Auditing/observability | ✅ Complete (debug logging) |
| 9 | GUI updates | 🚧 TODO |
| 10 | Anonymous user permissions | ✅ Complete |

## Performance Considerations

### Current Implementation

- **Parsing**: Cached at permission load time (one-time cost)
- **Evaluation**: Reflection-based field access (acceptable for CRUD operations)
- **List filtering**: Not yet implemented (will convert to JPA Specifications)

### Optimization Recommendations

1. **For high-throughput scenarios**: Use global permissions (no selector) to avoid evaluation overhead
2. **For list queries**: Permission-based filtering will be converted to SQL WHERE clauses (efficient)
3. **For imports**: Consider batch permission checks or use global permissions for import users

## Testing

### Unit Tests

Location: `service/src/test/java/io/commercestacksolutions/commons/permissionselector/`

- ✅ SelectorParserTest (28 tests)
- ✅ SelectorEvaluatorTest (20 tests)
- ✅ PermissionNameParserTest (14 tests)
- ✅ PermissionMatcherTest (13 tests)

### Integration Tests

TODO: Postman collection updates with:
- Selector-based permission creation/validation
- Object-level authorization checks
- List filtering with selectors
- Anonymous user access scenarios

## References

- Original Issue: #49 - feat candidate - extend AppPermissions by selectors
- Commits: 7e4ba7e, 8d77228, b3a9907
- Package: `io.commercestacksolutions.commons.permissionselector`
