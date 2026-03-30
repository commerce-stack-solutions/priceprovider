---
name: query-filtering
description: 'Skill for adding Lucene-like query filtering support to entity endpoints in the backend service'
---

# Goal
Add query filtering capability to an entity endpoint, allowing users to filter lists using Lucene-like query syntax (e.g., `field:value`, `field:>100`, `(field1:value1 OR field2:value2) AND field3:value3`).

# Overview
The query filtering infrastructure provides a reusable, generic approach for adding search/filter capabilities to any entity endpoint. The implementation follows a layered architecture pattern across Repository, Service, Facade, and Controller layers.

# Implementation Steps

## Step 1: Update Repository Layer
Extend the repository interface with `JpaSpecificationExecutor` to enable dynamic query execution.

```java
@Repository
public interface YourEntityRepository extends JpaRepository<YourEntity, IdType>,
                                               JpaSpecificationExecutor<YourEntity> {
    // Existing methods remain unchanged
}
```

**Note**: This change is non-breaking - existing repository methods continue to work.

## Step 2: Update Service Interface
Add `query` parameter to the list/get method signature and declare `InvalidParameterException`.

```java
public interface YourService {
    Page<YourEntity> getEntities(
        int page,
        int pageSize,
        List<String> sortBy,
        String sortDirection,
        String query  // Add this parameter
    ) throws InvalidParameterException;  // Add checked exception

    // Other methods...
}
```

## Step 3: Update Service Implementation

**Add required imports:**
```java
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.QueryExpression;
import de.ebusyness.commons.query.QueryParser;
import de.ebusyness.commons.query.QueryFilterRuntimeException;
import de.ebusyness.commons.query.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

**Add fields:**
```java
private static final Logger logger = LoggerFactory.getLogger(YourServiceImpl.class);
private final QueryParser queryParser;
```

**Initialize in constructor:**
```java
public YourServiceImpl(YourEntityRepository repository, /* other dependencies */) {
    this.repository = repository;
    this.queryParser = new QueryParser();
    // Initialize other dependencies...
}
```

**Update the get/list method with query filtering logic:**
```java
@Override
public Page<YourEntity> getEntities(
        int page,
        int pageSize,
        List<String> sortBy,
        String sortDirection,
        String query) throws InvalidParameterException {

    // Build PageRequest with sorting
    PageRequest pageRequest;
    if (sortBy != null && !sortBy.isEmpty()) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        List<Sort.Order> orders = new ArrayList<>();
        for (String field : sortBy) {
            orders.add(new Sort.Order(direction, field));
        }
        pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
    } else {
        pageRequest = PageRequest.of(page, pageSize);
    }

    // Parse and apply query filter if provided
    if (query != null && !query.trim().isEmpty()) {
        try {
            QueryExpression expression = queryParser.parse(query);
            Specification<YourEntity> spec = SpecificationBuilder.build(expression);
            try {
                return repository.findAll(spec, pageRequest);
            } catch (QueryFilterRuntimeException e) {
                // Unwrap and rethrow the checked exception
                throw e.getInvalidParameterException();
            }
        } catch (QueryParser.QueryParseException e) {
            logger.error("Failed to parse query: " + query, e);
            // Return empty page on parse error
            return Page.empty(pageRequest);
        }
    }

    return repository.findAll(pageRequest);
}
```

## Step 4: Update Facade Interface
Add `query` parameter and `InvalidParameterException` to the facade service interface.

```java
public interface YourFacadeService {
    YourListRestEntity getEntities(
        int page,
        int pageSize,
        List<String> sortBy,
        String sortDirection,
        Set<String> expand,
        String query  // Add this parameter
    ) throws DataMappingException, InvalidParameterException;  // Add InvalidParameterException

    // Other methods...
}
```

## Step 5: Update Facade Implementation
Pass the query parameter through to the service layer.

```java
@Override
@Transactional
public YourListRestEntity getEntities(
        int page,
        int pageSize,
        List<String> sortBy,
        String sortDirection,
        Set<String> expand,
        String query) throws DataMappingException, InvalidParameterException {

    // Pass query parameter to service
    Page<YourEntity> entitiesPage = yourEntityService.getEntities(
        page, pageSize, sortBy, sortDirection, query);

    // Rest of mapping logic remains unchanged
    RestResponseMappingContext context = new RestResponseMappingContext();
    context.addExpandPaths(expand);
    PagingInfo pagingInfo = new PagingInfo(
        entitiesPage.getNumber(),
        entitiesPage.getSize(),
        entitiesPage.getTotalElements(),
        entitiesPage.getTotalPages());
    SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty()
        ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc")
        : null;
    Collection<YourRestEntity> restEntities = yourRestEntityMapper.convertAll(
        entitiesPage.getContent(), context);

    return new YourListRestEntity(pagingInfo, sortingInfo, restEntities);
}
```

## Step 6: Update Controller
Expose the `q` query parameter in the REST endpoint.

**Add import:**
```java
import de.ebusyness.commons.exception.InvalidParameterException;
```

**Update the GET mapping:**
```java
@GetMapping
@Operation(summary = "Get all entities",
           description = "Retrieve a paginated list of entities with optional filtering")
@Parameter(name = "q",
           description = "Query filter (Lucene-like syntax). Examples: 'field:value', 'field:[min TO max]', '(field1:value1 OR field2:value2) AND field3:value3'",
           example = "field:value AND field2:>100")
public YourListRestEntity getEntities(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(value = "page", defaultValue = "0") int page,
        @Parameter(description = "Page size")
        @RequestParam(value = "page-size", defaultValue = "10") int pageSize,
        @Parameter(description = "Fields to sort by")
        @RequestParam(value = "sort-by", required = false) List<String> sortBy,
        @Parameter(description = "Sort direction (asc/desc)")
        @RequestParam(value = "sort-direction", required = false) String sortDirection,
        @Parameter(description = "Fields to expand")
        @RequestParam(value = "$expand", required = false) Set<String> expand,
        @Parameter(description = "Query filter")
        @RequestParam(value = "q", required = false) String q)
        throws DataMappingException, InvalidParameterException {  // Add InvalidParameterException

    return yourFacadeService.getEntities(page, pageSize, sortBy, sortDirection, expand, q);
}
```

## Step 7: Testing
After implementation, create or update tests:

1. **Controller Integration Tests**: Test actual DB-backed filtering with various query patterns
2. **Postman Collection**: Add example requests demonstrating query filtering
3. **Update API Documentation**: Include query filter examples in OpenAPI/Swagger annotations

**Test Examples:**
- Simple field match: `field:value`
- Comparison: `price:>100`
- Range: `date:[2024-01-01 TO 2024-12-31]`
- Logical operators: `(status:ACTIVE OR status:PENDING) AND price:<1000`
- Exists check: `optionalField.exists:true`

# Supported Field Types

The query filtering infrastructure automatically handles various field types:

- **String**: Exact match or contains (case-insensitive)
- **Enum**: Substring match on enum name (case-insensitive) - e.g., `groupType:ORG` matches `ORGANIZATION`
- **Number**: Comparisons (`>`, `<`, `>=`, `<=`) and ranges (`[min TO max]`)
- **Boolean**: Exact match (`true` or `false`)
- **Date/DateTime**: Comparisons and ranges
- **References (Single)**: Null/not-null checks, exact ID match
- **References (Collection)**: Empty/not-empty checks

# Security & Error Handling

**Exception Flow:**
1. `InvalidParameterException` - Checked exception for invalid query fields
2. `QueryFilterRuntimeException` - Runtime wrapper used in JPA Specification lambdas (unwrapped by service layer)
3. `ExceptionHandlerAdvice` - Catches exceptions and returns HTTP 400 responses

**Built-in Security Measures:**
- Input sanitization removes dangerous characters
- Depth limiting (max 10 levels) prevents stack overflow
- ReDoS protection via possessive quantifiers in regex
- SQL injection protection through JPA Criteria API with parameterized queries
- Field validation throws `InvalidParameterException` for unknown fields

# Best Practices

1. **Follow the pattern exactly** - Consistency across all entities is crucial
2. **Log parse errors** - Use logger for debugging query parsing issues
3. **Update documentation** - Keep OpenAPI/Swagger annotations current with examples
4. **Update Postman collection** - Add example requests for each queryable entity
5. **Test thoroughly** - Include happy path and error scenarios in tests

# Relevant Resources

- [Query Filtering Usage Guide](../../../service/doc/030-features/010-query-filtering-usage.md) - User guide with query syntax examples
- [Query Filtering Implementation Guide](../../../service/doc/030-features/020-query-filtering-implementation.md) - Comprehensive technical implementation guide
- Postman Collection: `service/postman/pps-postmancollection.json`
