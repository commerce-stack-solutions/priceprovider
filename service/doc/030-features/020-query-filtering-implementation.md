# Query Filtering Implementation Guide

This document provides the technical implementation pattern for adding query filtering support to new entities in the Price Provider Service.

## Overview

The query filtering infrastructure provides a reusable, generic approach for adding Lucene-like query filtering to any entity endpoint. The implementation follows a layered architecture pattern across Repository, Service, Facade, and Controller layers.

## Architecture

### Core Components

1. **QueryParser** (`io.commercestacksolutions.commons.query.QueryParser`)
   - Parses Lucene-like query syntax into AST (Abstract Syntax Tree)
   - Handles operators: `:`, `>`, `<`, `>=`, `<=`, `[TO]`, `.exists`
   - Supports logical operators: `AND`, `OR`, `NOT`
   - Supports parenthesized grouping

2. **QueryExpression** (`io.commercestacksolutions.commons.query.QueryExpression`)
   - Represents parsed query as tree structure
   - Nodes can be leaf (single filter) or composite (logical operations)

3. **SpecificationBuilder** (`io.commercestacksolutions.commons.query.SpecificationBuilder`)
   - Converts QueryExpression to JPA Specification
   - Generates type-aware predicates
   - Handles collections and single-valued references

4. **QueryFilterRuntimeException** (`io.commercestacksolutions.commons.query.QueryFilterRuntimeException`)
   - Runtime wrapper for checked `InvalidParameterException`
   - Used within JPA Specification lambdas
   - Unwrapped by service layer

## Implementation Pattern

When adding query filtering to a new entity, follow these steps:

### 1. Repository Layer

Extend the repository interface with `JpaSpecificationExecutor`:

```java
@Repository
public interface YourEntityRepository extends JpaRepository<YourEntity, IdType>, 
                                               JpaSpecificationExecutor<YourEntity> {
    // Existing methods...
}
```

**Note**: This change alone provides no breaking changes - existing methods still work.

### 2. Service Interface

Add `query` parameter to the list method signature:

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

### 3. Service Implementation

Update the service implementation:

**Add imports:**
```java
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.QueryExpression;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.QueryFilterRuntimeException;
import io.commercestacksolutions.commons.query.SpecificationBuilder;
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

**Update the get/list method:**
```java
@Override
public Page<YourEntity> getEntities(
        int page, 
        int pageSize, 
        List<String> sortBy, 
        String sortDirection, 
        String query) throws InvalidParameterException {
    
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

### 4. Facade Interface

Update the facade service interface:

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

### 5. Facade Implementation

Update the facade implementation to pass through the query parameter:

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
    
    Page<YourEntity> entitiesPage = yourEntityService.getEntities(
        page, pageSize, sortBy, sortDirection, query);  // Pass query parameter
    
    // Rest of the mapping logic...
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

### 6. Controller

Update the controller to expose the `q` parameter:

**Add import:**
```java
import io.commercestacksolutions.commons.exception.InvalidParameterException;
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

## Exception Handling

The exception handling flow is crucial for proper error responses:

1. **InvalidParameterException** - Checked exception for invalid query fields
2. **QueryFilterRuntimeException** - Runtime wrapper used in JPA Specification lambdas
3. **ExceptionHandlerAdvice** - Catches and unwraps exceptions to return proper HTTP 400 responses

The service layer unwraps `QueryFilterRuntimeException` and rethrows `InvalidParameterException`:

```java
try {
    return repository.findAll(spec, pageRequest);
} catch (QueryFilterRuntimeException e) {
    throw e.getInvalidParameterException();
}
```

## Security Considerations

The implementation includes several security measures:

1. **Input Sanitization**: QueryParser sanitizes input to remove dangerous characters
2. **Depth Limiting**: Maximum nesting depth of 10 levels prevents stack overflow
3. **ReDoS Protection**: Regex patterns use possessive quantifiers
4. **SQL Injection Protection**: JPA Criteria API with parameterized queries
5. **Field Validation**: Unknown fields throw InvalidParameterException with field name

### Input Sanitization: handling of special / Unicode characters in query values
The sanitization regex allows Unicode letters, numbers and currency symbols while still restricting other potentially dangerous characters. 
The allowed character set preserves necessary query syntax characters (colon, brackets, parentheses, comparison and wildcard characters).

Technical details (for implementers):
- regex (current implementation):
  [^\p{L}\p{N}\s:\*\[\]\(\)\-_.TZ<>=\p{Sc}]

  - `\p{L}` permits Unicode letters
  - `\p{N}` permits Unicode numbers
  - `\p{Sc}` permits currency symbols (€, $, ¥, etc.)
  - explicit `[` `]` `(` `)` preserved for range/group syntax

Security notes & recommendations:

Because the JPA Criteria API is used to build parameterized predicates, injection risks from allowing additional value characters are mitigated. Nevertheless, the strict validation of field names and length limits should remain.
- Keep field-name whitelist strict. Do not allow arbitrary characters in field names used for JPA path resolution.
- Sanitize or limit value length to a reasonable max (e.g. 1024 chars) to reduce DoS risk.
- Ensure the application server/container is configured to decode request parameters as UTF-8 (Spring Boot defaults are usually fine but check `server.tomcat.uri-encoding` and `spring.servlet.encoding.charset` if problems are observed).

## Testing

### Unit Tests

- The `QueryParser` unit tests were extended to cover currency symbols. New tests include:
  - `symbol:$` -> parsed value `$`
  - `symbol:€` -> parsed value `€`

- Existing parser tests (range, exists, comparison, wildcards, logical ops) remain unchanged.

Run only parser unit tests locally (fast):

```bash
# from repo root on Windows (cmd.exe)
cd service
gradlew.bat test --tests io.commercestacksolutions.commons.query.QueryParserTest
```

### Integration Tests

- Controller integration tests that assert actual DB-backed filtering (e.g. `CurrencyControllerQueryFilterTest`) should be re-run. They previously showed 0 hits for currency-symbol queries; after this change they are expected to return matching rows.
- If integration tests still fail, verify:
  - The HTTP client encodes parameters in UTF-8 (e.g. `€` → `%E2%82%AC`) and the container decodes to UTF-8.
  - The test dataset includes entries with the expected symbols.

### Regression checklist

- Run `QueryParserTest` unit tests — green (done locally during change).
- Run full test-suite in CI (may require DB/container) — ensure no regressions in other areas.
- Re-run `CurrencyControllerQueryFilterTest` and `PriceRowControllerQueryFilterTest`.

## Best Practices

1. **Consistent Pattern**: Follow the exact pattern shown above for all entities
2. **Logging**: Log parse errors for debugging
3. **Documentation**: Update API documentation with query examples (including examples with currency symbols)
4. **Postman**: Add example requests to Postman collection
5. **Error Handling**: Always catch and handle QueryFilterRuntimeException
6. **Testing**: Test both happy path and error scenarios

## Example Entity Field Types

The specification builder handles various field types automatically:

- **String**: Exact match or contains (case-insensitive)
- **Enum**: Substring match on enum name (case-insensitive) - e.g., `groupType:ORG` matches `ORGANIZATION`
- **Number**: Comparisons and ranges
- **Boolean**: Exact match
- **Date/DateTime**: Comparisons and ranges
- **References (Single)**: Null/not-null checks, exact ID match
- **References (Collection)**: Empty/not-empty checks

## Additional Resources

- Usage Guide: `service/doc/030-features/010-query-filtering-usage.md`
- Query Parser Tests: `service/src/test/java/de/ebusyness/commons/query/QueryParserTest.java`
- Specification Builder Tests: `service/src/test/java/de/ebusyness/commons/query/SpecificationBuilderTest.java`
- Postman Collection: `service/postman/pps-postmancollection.json`
