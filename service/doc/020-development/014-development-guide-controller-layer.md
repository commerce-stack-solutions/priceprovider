# Development Guide – Controller Layer

The controller layer is responsible for REST endpoint definition, input validation, API contract (OpenAPI), and central exception handling.

For an overview of the layering concept, see [Architecture Overview](../010-architecture/010-overview.md#architectural-layers).  
For general REST API concepts and typical call patterns, see [040-api-reference/010-general-concept.md](../040-api-reference/010-general-concept.md).  
For error codes and error response format, see [040-api-reference/020-error-codes.md](../040-api-reference/020-error-codes.md).

## Package Structure

```
io.commercestacksolutions.priceproviderservice.web.controller/
├── {entity}/
│   └── {Entity}Controller.java     # REST controller
├── ExceptionHandlerAdvice.java     # Central exception handler (@ControllerAdvice)
└── {Type}Validator.java            # Input validators
```

## Implementing a REST Controller

Controllers depend on facade interfaces and declare exceptions in method signatures:

```java
@RestController
@RequestMapping("/admin/api/units")
@Tag(name = "Units", description = "Unit management API - handles measurement units like meters, kilograms, etc.")
public class UnitController {

    private final UnitFacadeService unitFacade; // Always inject interface

    @Autowired
    public UnitController(UnitFacadeService unitFacade) {
        this.unitFacade = unitFacade;
    }

    @Operation(summary = "Get list of units",
               description = "Supports advanced filtering via 'q' (Lucene-like syntax), pagination and sorting.")
    @GetMapping
    public UnitListRestEntity getUnits(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,
            @RequestParam(value = "sort-direction", required = false) String sortDirection,
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return unitFacade.getUnits(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(summary = "Get unit by symbol")
    @GetMapping("/{symbol}")
    public UnitRestEntity getUnit(
            @PathVariable("symbol") String symbol,
            @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        return unitFacade.getUnit(symbol, expand);
    }

    @Operation(summary = "Partially update unit (RFC 6902 JSON Patch)")
    @PatchMapping("/{symbol}")
    public UnitRestEntity patch(
            @PathVariable("symbol") String symbol,
            @RequestBody JsonNode patch
    ) throws NotFoundException, DataMappingException, EntityValidationException {
        return unitFacade.patch(symbol, patch);
    }

    @Operation(summary = "Create or replace unit (idempotent)")
    @PutMapping("/{symbol}")
    public UnitRestEntity createOrRecreate(
            @PathVariable("symbol") String symbol,
            @RequestBody UnitRestEntity unitRestEntity
    ) throws DataMappingException, EntityValidationException {
        return unitFacade.createOrRecreate(symbol, unitRestEntity);
    }

    @Operation(summary = "Create new unit")
    @PostMapping("/create")
    public UnitRestEntity create(
            @RequestBody UnitRestEntity unitRestEntity
    ) throws DataMappingException, EntityAlreadyExistsException, InvalidParameterException, EntityValidationException {
        return unitFacade.create(unitRestEntity);
    }

    @Operation(summary = "Delete unit")
    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> delete(@PathVariable("symbol") String symbol) {
        try {
            unitFacade.delete(symbol);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Bulk delete units")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteUnits(
            @RequestBody List<String> symbols
    ) throws DataIntegrityException {
        unitFacade.bulkDeleteUnits(symbols);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create or update multiple units (max 100)")
    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<UnitListRestEntity> createOrUpdateAllUnits(
            @RequestBody List<UnitRestEntity> unitRestEntities
    ) throws DataMappingException, InvalidParameterException {
        UnitListRestEntity result = unitFacade.createOrUpdateAllUnits(unitRestEntities);
        // Check for top-level error messages (e.g. max items exceeded)
        List<Message> msgs = result.getMessages();
        if (msgs != null && msgs.stream().anyMatch(m -> m.getType() == Message.MessageType.ERROR)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}
```

## Exception Handling

REST calls follow a consistent exception handling strategy based on **checked exceptions** that are propagated through the call stack and handled centrally by `ExceptionHandlerAdvice`.

### Core Principles

#### 1. Use Checked Exceptions

All domain and business exceptions **MUST** be checked exceptions (extending `Exception`, not `RuntimeException`):

```java
// ✅ Correct
public class NotFoundException extends Exception { ... }

// ❌ Incorrect
public class NotFoundException extends RuntimeException { ... }
```

**Why checked exceptions?**  
Forces explicit handling/declaration, makes error paths visible, prevents silent failures, enables better compile-time verification.

#### 2. Propagate Exceptions Through the Call Stack

**Do NOT** catch and suppress exceptions in intermediate layers. Declare them in method signatures:

```java
// ✅ Correct: Declare exceptions in method signature
@Override
public UnitRestEntity getUnit(String symbol, Set<String> expand)
        throws NotFoundException, DataMappingException {
    UnitEntity unit = unitService.getUnit(symbol);
    if (unit == null) {
        throw new NotFoundException(MessageKeys.ERROR_UNIT_NOT_FOUND);
    }
    return unitRestEntityMapper.convert(unit, context);
}

// ❌ Incorrect: Catch and convert in controller
@GetMapping("/{symbol}")
public ResponseEntity<?> getUnit(@PathVariable String symbol) {
    try {
        return ResponseEntity.ok(unitFacade.getUnit(symbol, Set.of()));
    } catch (NotFoundException e) {
        return ResponseEntity.notFound().build(); // Don't do this!
    }
}
```

#### 3. Central Exception Handling via ExceptionHandlerAdvice

`ExceptionHandlerAdvice` is the **single point** where exceptions are converted to HTTP responses:

```java
@ControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getErrorResponse());
    }

    @ExceptionHandler(EntityValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleEntityValidationException(EntityValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getErrorResponse());
    }

    @ExceptionHandler(DataIntegrityException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityException(DataIntegrityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getErrorResponse());
    }
    // ... other handlers
}
```

### Standard Exception Types

| Exception | HTTP Status | When to Use |
|-----------|------------|-------------|
| `NotFoundException` | 404 | Entity lookup returns null |
| `EntityValidationException` | 400 | Domain validation rules violated |
| `InvalidParameterException` | 400 | Invalid input parameters |
| `DataIntegrityException` | 409 | Unique/FK constraint violations |
| `EntityAlreadyExistsException` | 409 | Duplicate primary key on create |
| `DataMappingException` | 400 | Entity ↔ REST entity conversion error |
| `QueryParseException` | 400 | Invalid `q` query filter syntax |

### Exception Handling Flow

```
Controller Layer
  - Declares exceptions in method signature
  - Does NOT catch domain exceptions
        ↓ throws XxxException
Facade Layer
  - Declares exceptions in method signature
  - Throws domain exceptions when needed
        ↓ throws XxxException
Service Layer
  - Declares exceptions in method signature
  - Throws validation, not-found exceptions
        ↓ throws XxxException
ExceptionHandlerAdvice (@ControllerAdvice)
  - Catches with @ExceptionHandler
  - Maps to HTTP status codes
  - Returns ResponseEntity<ErrorResponse>
```

### Best Practices

```java
// ✅ DO: Use message keys for error messages
throw new NotFoundException(MessageKeys.ERROR_UNIT_NOT_FOUND, Map.of("symbol", symbol));

// ❌ DON'T: Use hardcoded strings
throw new NotFoundException("Unit not found: " + symbol);

// ✅ DO: Include field references in messages
Message error = new Message(Message.MessageType.ERROR,
    MessageKeys.ERROR_VALIDATION_REQUIRED_FIELD,
    Map.of("fieldName", "unitRef"),
    List.of("unitRef")); // Field reference for UI highlighting

// ❌ DON'T: Include HTTP status codes in messages (single-resource REST calls)
// Status codes belong in HTTP response headers only
```

## OpenAPI / Swagger Documentation

The service uses SpringDoc OpenAPI 3.0 for API documentation. Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

### Annotating Endpoints

```java
@Tag(name = "Units", description = "Unit management endpoints")
@RestController
public class UnitController {

    @Operation(summary = "Get unit by symbol", description = "Returns a unit by its unique symbol")
    @ApiResponse(responseCode = "200", description = "Unit found")
    @ApiResponse(responseCode = "404", description = "Unit not found")
    @GetMapping("/{symbol}")
    public ResponseEntity<UnitRestEntity> getUnit(@PathVariable String symbol) throws ... {
        // ...
    }
}
```

## PATCH Request Examples

PATCH uses RFC 6902 JSON Patch standard. Content-Type: `application/json-patch+json`.

### Update a Single Field

```json
[
  { "op": "replace", "path": "/pricedResourceId", "value": "PROD-123" }
]
```

### Update Multiple Fields

```json
[
  { "op": "replace", "path": "/pricedResourceId", "value": "PROD-456" },
  { "op": "replace", "path": "/priceValue", "value": 99.99 },
  { "op": "replace", "path": "/taxIncluded", "value": true }
]
```

### Remove a Field

```json
[
  { "op": "remove", "path": "/customerId" }
]
```

### Update a Nested Map Field (e.g., UnitEntity.name)

```json
[
  { "op": "add", "path": "/name/en", "value": "meter" },
  { "op": "add", "path": "/name/de", "value": "Meter" }
]
```
