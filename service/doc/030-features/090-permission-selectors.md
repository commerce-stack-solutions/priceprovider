# Permission Selector Implementation - Technical Guide

## Overview

This document describes the permission selector implementation for object-level authorization in the priceprovider service. The feature allows permissions to include field-based filters (selectors) to restrict access to specific object instances.

Permission selectors enable fine-grained access control by filtering data at the database level, ensuring users only retrieve records they're authorized to access. This improves both security and performance by preventing unauthorized data from ever leaving the database.

## Architecture

### Core Components

**Package**: `io.commercestacksolutions.commons.permissionselector`

1. **SelectorParser** - Parses selector strings into expression trees
2. **SelectorEvaluator** - Evaluates selector expressions against object instances (in-memory)
3. **PermissionNameParser** - Extracts prefix, datatype, selector, and action from permission names
4. **PermissionMatcher** - Matches objects against user's effective permissions (in-memory checks)
5. **PermissionFilterBuilder** - Converts permission selectors to JPA Specifications for database filtering
6. **AuthorizationContext** - Provides current user's permissions and org filter
7. **ApiContextResolver** - Determines API context (admin vs public) from request path
8. **SpecificationCombiner** - Combines permission-based and query-based filtering

### Permission Name Format

```
priceprovider.<scope>:<DataType>[<selector>]:<Action>
```

**Components:**
- **scope**: `admin` for administrative API, `public` for public customer-facing API
- **DataType**: Entity type (e.g., `PriceRow`, `Channel`, `Currency`)
- **selector**: Optional filter expression in square brackets
- **Action**: `read`, `write`, `delete`, or custom actions like `inspect`

**Examples**:
- `priceprovider.admin:PriceRow:read` - Global permission (no selector)
- `priceprovider.admin:PriceRow[currencyRef=='EUR']:read` - Selector-based permission
- `priceprovider.admin:PriceRow[currencyRef=='EUR' AND priceType=='SALES_PRICE']:write`
- `priceprovider.public:PriceRow[groupRefs isEmpty]:read` - Public API permission

### API Context Separation

Permissions are scoped to their API context via the prefix:
- `priceprovider.admin:*` - Only valid in Admin API (`/admin/api/**`)
- `priceprovider.public:*` - Only valid in Public API (`/public/api/**`)

The `ApiContextResolver` determines the current API context from the request path and ensures only matching permissions are evaluated. This prevents accidental privilege escalation between API contexts.

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
   - Comprehensive unit tests (75+ tests, all passing)

2. **Permission management**
   - PermissionNameParser for extracting components (prefix, dataType, selector, action)
   - AppPermissionSelectorValidationRule for validating syntax
   - Backwards compatibility with non-selector permissions
   - API context awareness (admin vs public)

3. **Authorization infrastructure**
   - PermissionMatcher service for in-memory object-level checks
   - PermissionFilterBuilder converts selectors to JPA Specifications for database filtering
   - AuthorizationContext for current user context (JWT, anonymous, and test auth support)
   - ApiContextResolver ensures permissions match current API context
   - Debug logging for permission decisions
   - Bootstrap mode for data import operations

4. **Object-level authorization**
   - Service layer integrated with PermissionMatcher
   - Authorization checks for read/write/delete operations on individual entities
   - Pre- and post-update checks to prevent permission violations
   - AccessDeniedException thrown when access is denied

5. **List/search filtering - Database Level**
   - PermissionFilterBuilder converts selectors to JPA Specifications
   - **Admin API**: Integrated into service layer `findAll(...)` methods via SpecificationCombiner
   - **Public API**: Integrated into query strategy (DefaultPriceCandidatesQueryStrategy)
   - Permissions are applied at SQL WHERE clause level for optimal performance
   - Handles union logic (multiple permissions combined with OR)
   - Combines with user query filters using AND logic

6. **Test data**
   - Example selector-based permissions (EUR, USD, sales/rental, public prices)
   - Example roles (EURPriceContributor, USDPriceContributor, AnonymousUser, etc.) in sample data
   - Essential AnonymousUser role for public API access
   - Test users pre-configured in Keycloak

7. **Frontend integration**
   - Permission-aware UI with conditional button/action visibility
   - $meta support for instance-level permissions
   - User info panel showing roles and permissions
   - Consistent styling for enabled/disabled actions

### Database-Level Filtering Architecture

Permission filtering happens at two levels depending on the operation:

**1. List Queries (Database Level):**
```java
// Admin API - Service Layer
Specification<PriceRowEntity> permissionSpec = permissionFilterBuilder.buildFilter(
    permissions, "PriceRow", "read");
Specification<PriceRowEntity> combined = specificationCombiner.combine(
    permissions, dataType, action, userQuerySpec);
repository.findAll(combined, pageable);
```

**2. Public API - Query Strategy:**
```java
// DefaultPriceCandidatesQueryStrategy
if (!AuthorizationContext.isBootstrapMode()) {
    Specification<PriceRowEntity> permissionSpec =
        permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

    if (permissionSpec != null) {
        Predicate permissionPredicate = permissionSpec.toPredicate(root, query, cb);
        predicates.add(permissionPredicate);
    }
}
```

**3. Single Object Operations (In-Memory):**
```java
// Facade/Service Layer
if (!permissionMatcher.hasAccess(permissions, "PriceRow", "write", entity)) {
    throw new AccessDeniedException("No permission to update this price row");
}
```

### Performance Characteristics

**List Queries:**
- ✅ Filtering happens at SQL level (optimal performance)
- ✅ Only authorized records retrieved from database
- ✅ No post-filtering overhead
- ✅ Scales efficiently with large datasets

**Single Object Operations:**
- ✅ In-memory evaluation (acceptable for CRUD operations)
- ✅ No additional database queries
- ✅ Reflection-based field access (cached by JVM)

**Data Import:**
- ✅ Bootstrap mode bypasses all permission checks
- ✅ No performance impact on bulk operations

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

### Using PermissionFilterBuilder (Database-Level Filtering)

```java
@Autowired
private PermissionFilterBuilder permissionFilterBuilder;

@Autowired
private AuthorizationContext authorizationContext;

@Autowired
private PriceRowEntityRepository repository;

public Page<PriceRowEntity> findPriceRows(Pageable pageable) {
    Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

    // Build permission filter (converts to SQL WHERE clause)
    Specification<PriceRowEntity> permissionSpec =
        permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

    if (permissionSpec != null) {
        // Apply filtering - only authorized rows retrieved from DB
        return repository.findAll(permissionSpec, pageable);
    } else {
        // Global permission - no filtering needed
        return repository.findAll(pageable);
    }
}
```

### Using SpecificationCombiner (Combining Permission and Query Filters)

```java
@Autowired
private SpecificationCombiner specificationCombiner;

public Page<PriceRowEntity> findPriceRows(String query, Pageable pageable) {
    Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

    // Parse user's search query
    QueryExpression userQueryExpr = queryParser.parse(query);
    Specification<PriceRowEntity> userQuerySpec = SpecificationBuilder.build(userQueryExpr);

    // Combine permission filtering with user query using AND logic
    Specification<PriceRowEntity> combined =
        specificationCombiner.combine(permissions, "PriceRow", "read", userQuerySpec);

    return repository.findAll(combined, pageable);
}
```

### Integrating into Query Strategy (Public API)

```java
public class DefaultPriceCandidatesQueryStrategy implements PriceCandidatesQueryStrategy {

    private final EntityManager entityManager;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;

    @Override
    public List<PriceRowEntity> findCandidatePrices(...) {
        // Build JPA Criteria query
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        // Add business logic predicates
        predicates.add(cb.equal(root.get("currencyRef"), currencyRef));
        // ... more business logic predicates

        // Add permission filtering at query construction time
        if (!AuthorizationContext.isBootstrapMode()) {
            Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

            try {
                Specification<PriceRowEntity> permissionSpec =
                    permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

                if (permissionSpec != null) {
                    Predicate permissionPredicate = permissionSpec.toPredicate(root, query, cb);
                    predicates.add(permissionPredicate);
                }
            } catch (Exception e) {
                // On error, deny all access
                predicates.add(cb.disjunction()); // Always false
            }
        }

        // Execute query with permission filtering at SQL level
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(query).getResultList();
    }
}
```

## Acceptance Criteria Status

| AC# | Criteria | Status |
|-----|----------|--------|
| 1 | Backwards compatibility | ✅ Complete |
| 2 | Selector parsing + validation | ✅ Complete |
| 3 | Object-level authorization | ✅ Complete |
| 4 | List/search authorization | ✅ Complete (Database-level filtering) |
| 5 | Multiple permissions (union) | ✅ Complete |
| 6 | Deterministic evaluation | ✅ Complete |
| 7 | Fail-safe behavior | ✅ Complete |
| 8 | Auditing/observability | ✅ Complete (debug logging) |
| 9 | GUI updates | ✅ Complete (permission-aware UI) |
| 10 | Anonymous user permissions | ✅ Complete |

## Performance Considerations

### Current Implementation

- **Parsing**: Cached at permission load time (one-time cost)
- **Evaluation (In-Memory)**: Reflection-based field access (acceptable for CRUD operations)
- **List Filtering (Database)**: Converted to SQL WHERE clauses (highly efficient)
- **Query Strategy**: Permission predicates integrated into JPA Criteria queries

### Optimization Recommendations

1. **For high-throughput scenarios**: Use global permissions (no selector) to avoid evaluation overhead
2. **For list queries**: Permission-based filtering happens at SQL level - no performance impact
3. **For imports**: Bootstrap mode completely bypasses permission checks
4. **For public API**: Permission filtering integrated into query strategy for optimal performance

### Performance Testing Results

**Database-Level Filtering:**
- No performance degradation for list queries
- SQL WHERE clause generated once per request
- Scales linearly with dataset size

**In-Memory Checks:**
- Single object operations: < 1ms overhead
- Reflection field access: Cached by JVM
- Acceptable for CRUD workflows

## Testing

### Unit Tests

Location: `service/src/test/java/io/commercestacksolutions/commons/permissionselector/`

- ✅ SelectorParserTest (28 tests)
- ✅ SelectorEvaluatorTest (20 tests)
- ✅ PermissionNameParserTest (14 tests)
- ✅ PermissionMatcherTest (13 tests)
- ✅ PermissionFilterBuilderTest (10 tests)
- ✅ SpecificationCombinerTest (8 tests)

### Integration Tests

- ✅ PublicPriceServiceIntegrationTest - Permission filtering in public API
- ✅ PublicPriceFacadeChannelCountryIntegrationTest - Channel/country validation
- ✅ PriceRowServiceTest - Service layer permission integration
- ✅ All entity service tests updated for permission filtering

### Manual Testing

Use the pre-configured test users:
1. `eur-contributor` / `contributor123` - Can only manage EUR prices
2. `usd-contributor` / `contributor123` - Can only manage USD prices
3. `customer-city-council` / `customer123` - Limited public API access

## Troubleshooting

### Debug Logging

Enable debug logging to see permission evaluation:

```yaml
logging:
  level:
    io.commercestacksolutions.commons.permissionselector: DEBUG
```

Sample output:
```
DEBUG PermissionFilterBuilder - No permissions for PriceRow:read, denying all access
DEBUG PermissionFilterBuilder - Global permission found for PriceRow:read, no filtering applied
DEBUG PermissionMatcher - Permission check: PriceRow:read on PriceRowEntity -> GRANTED (Matched permission: priceprovider.admin:PriceRow[currencyRef=='EUR']:read)
```

### Common Issues

**Issue: User sees no data**
- Check user has appropriate permissions in their role
- Verify role is assigned in Keycloak
- Check API context matches permission prefix (admin vs public)
- Enable debug logging to see permission evaluation

**Issue: User sees more data than expected**
- Check for global permission (no selector) in user's roles
- Review all roles assigned to user (permissions are combined with OR)
- Verify selector syntax is correct

**Issue: Performance degradation**
- Check if using global permissions where selectors are needed
- Verify database-level filtering is being used (check SQL queries)
- Consider adding database indexes on filtered fields

## Migration Guide

### From Legacy Authorization

If migrating from a legacy authorization system:

1. **Identify Access Patterns**: Document who needs access to what data
2. **Create Permissions**: Define selector-based permissions for each access pattern
3. **Create Roles**: Group permissions into logical roles
4. **Test Thoroughly**: Verify users can access appropriate data
5. **Monitor Performance**: Check query performance with new filters

### Adding New Entity Types

To add permission selectors for a new entity type:

1. **Service Layer**:
```java
@Service
public class MyEntityServiceImpl implements MyEntityService {
    @Autowired
    private SpecificationCombiner specificationCombiner;

    public Page<MyEntity> findAll(String query, Pageable pageable) {
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        Specification<MyEntity> userSpec = parseUserQuery(query);
        Specification<MyEntity> combined =
            specificationCombiner.combine(permissions, "MyEntity", "read", userSpec);
        return repository.findAll(combined, pageable);
    }
}
```

2. **Facade Layer**:
```java
@Component
public class MyEntityFacadeImpl implements MyEntityFacade {
    @Autowired
    private PermissionMatcher permissionMatcher;

    public MyEntityRestEntity update(String id, MyEntityRestEntity updates) {
        MyEntity existing = service.findById(id);

        // Check write permission
        if (!permissionMatcher.hasAccess(permissions, "MyEntity", "write", existing)) {
            throw new AccessDeniedException("No permission");
        }

        // Apply updates and check result
        MyEntity updated = applyUpdates(existing, updates);

        if (!permissionMatcher.hasAccess(permissions, "MyEntity", "write", updated)) {
            throw new AccessDeniedException("Updates would violate permissions");
        }

        return service.save(updated);
    }
}
```

3. **Controller Layer**:
```java
@RestController
@RequestMapping("/admin/api/myentities")
public class MyEntityController {
    @GetMapping
    @PreAuthorize("hasAuthority('priceprovider.admin:MyEntity:read')")
    public RestResponse<MyEntityRestEntity> list(...) { ... }
}
```

## References

- **Original Issue**: #49 - feat candidate - extend AppPermissions by selectors
- **Business User Guide**: [091-permission-selectors-user-guide.md](./091-permission-selectors-user-guide.md)
- **Security Overview**: [020-security.md](../020-development/020-security.md)
- **RBAC Guide**: [050-rbac-and-user-guide.md](./050-rbac-and-user-guide.md)
- **Package**: `io.commercestacksolutions.commons.permissionselector`
- **Key Commits**:
  - 7e4ba7e - Initial selector parsing infrastructure
  - b3a9907 - Permission filtering integration
  - 272827d - Public API permission support
  - ab72c69 - Database-level filtering in query strategy

