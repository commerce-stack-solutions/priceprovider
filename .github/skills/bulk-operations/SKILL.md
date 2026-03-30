---
name: bulk-operations
description: 'Skill for implementing bulk create-or-update operations with smart field matching in the backend service'
---

# Goal
Implement bulk create-or-update endpoints that support both ID-based updates and smart field matching for entity operations. This allows API clients to efficiently create or update multiple entities in a single request without needing to track technical IDs.

# Overview
Bulk operations provide an intelligent way to create or update multiple entities in a single API call. The system supports two modes:

1. **Update by ID**: When an entity includes an `id` field, update the existing entity
2. **Smart Matching** (PriceRow only) or **Natural Key Matching** (other entities): When no `id` is provided, find matching entities based on business fields or natural keys

# Matching Strategies per Entity

Different entities use different strategies when processing `bulk-create-or-update` requests:

| Entity | Match Strategy | Key Field(s) |
|--------|---------------|-------------|
| **PriceRow** | **Smart matching** (complex business fields) | `pricedResourceId` + `minQuantity` + `unitRef` + `currencyRef` + `taxClassRef` + `taxIncluded` + `priceType` + `validFrom` + `validTo` + `groupRefs` |
| **Unit** | **Natural key** | `symbol` |
| **Currency** | **Natural key** | `currencyKey` |
| **Language** | **Natural key** | `isoKey` |
| **TaxClass** | **Natural key** | `taxClassId` |
| **Group** | **Natural key** | `id` |
| **Organization** | **Natural key** | natural key field |

**Note**: Smart matching is only implemented for PriceRow because it has a technical auto-generated ID and no unique natural business key. All other entities use their natural key directly.

# Implementation Pattern

## 1. Natural Key Matching (Most Entities)

For entities with a natural business key (Unit, Currency, Language, etc.), the implementation is straightforward:

**Controller endpoint:**
```java
@PostMapping("/bulk-create-or-update")
@Operation(summary = "Bulk create or update entities")
public YourListRestEntity bulkCreateOrUpdate(
        @Parameter(description = "Maximum 100 entities")
        @RequestBody @Valid List<YourRestEntity> entities)
        throws DataMappingException, EntityValidationException {

    if (entities.size() > 100) {
        throw new InvalidParameterException("Maximum 100 entities allowed per request");
    }

    return yourFacadeService.bulkCreateOrUpdate(entities);
}
```

**Facade implementation:**
```java
@Override
@Transactional
public YourListRestEntity bulkCreateOrUpdate(List<YourRestEntity> restEntities)
        throws DataMappingException, EntityValidationException {

    List<YourEntity> processedEntities = new ArrayList<>();

    for (YourRestEntity restEntity : restEntities) {
        // Natural key lookup
        Optional<YourEntity> existing = yourEntityService.findByNaturalKey(restEntity.getNaturalKey());

        YourEntity entity;
        if (existing.isPresent()) {
            // Update existing entity
            entity = existing.get();
            // Map fields from restEntity to entity (excluding ID)
            yourMapper.updateEntity(entity, restEntity);
        } else {
            // Create new entity
            entity = yourMapper.toEntity(restEntity);
        }

        YourEntity saved = yourEntityService.save(entity);
        processedEntities.add(saved);
    }

    // Map to RestEntities and return
    RestResponseMappingContext context = new RestResponseMappingContext();
    Collection<YourRestEntity> resultEntities = yourMapper.convertAll(processedEntities, context);
    return new YourListRestEntity(null, null, resultEntities);
}
```

## 2. Smart Matching (PriceRow Only)

For PriceRow, which has complex business semantics and no natural key, implement smart field matching:

**Service layer - Smart matching logic:**
```java
@Override
public PriceRowEntity findMatchingPriceRow(PriceRowEntity criteria) {
    // Search for existing price row matching ALL identifying fields
    List<PriceRowEntity> matches = repository.findAll((root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();

        // Match all identifying fields
        predicates.add(cb.equal(root.get("pricedResourceId"), criteria.getPricedResourceId()));
        predicates.add(cb.equal(root.get("minQuantity"), criteria.getMinQuantity()));
        predicates.add(cb.equal(root.get("unitRef"), criteria.getUnitRef()));
        predicates.add(cb.equal(root.get("currencyRef"), criteria.getCurrencyRef()));
        predicates.add(cb.equal(root.get("taxClassRef"), criteria.getTaxClassRef()));
        predicates.add(cb.equal(root.get("taxIncluded"), criteria.getTaxIncluded()));
        predicates.add(cb.equal(root.get("priceType"), criteria.getPriceType()));

        // Handle nullable date fields
        if (criteria.getValidFrom() != null) {
            predicates.add(cb.equal(root.get("validFrom"), criteria.getValidFrom()));
        } else {
            predicates.add(cb.isNull(root.get("validFrom")));
        }

        if (criteria.getValidTo() != null) {
            predicates.add(cb.equal(root.get("validTo"), criteria.getValidTo()));
        } else {
            predicates.add(cb.isNull(root.get("validTo")));
        }

        // Match group references (collection comparison)
        // Implementation depends on how you want to match collections

        return cb.and(predicates.toArray(new Predicate[0]));
    });

    return matches.isEmpty() ? null : matches.get(0);
}
```

**Facade implementation:**
```java
@Override
@Transactional
public PriceRowListRestEntity bulkCreateOrUpdate(List<PriceRowRestEntity> restEntities)
        throws DataMappingException, EntityValidationException {

    List<PriceRowEntity> processedEntities = new ArrayList<>();

    for (PriceRowRestEntity restEntity : restEntities) {
        PriceRowEntity entity;

        if (restEntity.getId() != null) {
            // ID-based update
            entity = priceRowService.findById(restEntity.getId())
                .orElseThrow(() -> new NotFoundException("PriceRow", restEntity.getId()));
            priceRowMapper.updateEntity(entity, restEntity);
        } else {
            // Smart matching
            PriceRowEntity criteria = priceRowMapper.toEntity(restEntity);
            PriceRowEntity existing = priceRowService.findMatchingPriceRow(criteria);

            if (existing != null) {
                // Update only the price value (other fields define the match)
                entity = existing;
                entity.setPriceValue(restEntity.getPriceValue());
            } else {
                // Create new entity
                entity = criteria;
            }
        }

        PriceRowEntity saved = priceRowService.save(entity);
        processedEntities.add(saved);
    }

    // Map to RestEntities and return
    RestResponseMappingContext context = new RestResponseMappingContext();
    Collection<PriceRowRestEntity> resultEntities = priceRowMapper.convertAll(processedEntities, context);
    return new PriceRowListRestEntity(null, null, resultEntities);
}
```

# Error Handling with Partial Results

Bulk operations should support partial success - some entities succeed while others fail:

```java
@Override
@Transactional
public YourListRestEntity bulkCreateOrUpdate(List<YourRestEntity> restEntities)
        throws DataMappingException {

    List<YourRestEntity> results = new ArrayList<>();

    for (YourRestEntity restEntity : restEntities) {
        try {
            // Process entity (create or update)
            YourEntity saved = processEntity(restEntity);

            // Convert to RestEntity
            RestResponseMappingContext context = new RestResponseMappingContext();
            YourRestEntity result = yourMapper.convert(saved, context);
            results.add(result);

        } catch (EntityValidationException e) {
            // Add entity with error messages
            restEntity.setMessages(e.getMessages());
            results.add(restEntity);
        } catch (NotFoundException e) {
            // Add entity with error message
            Message errorMsg = new Message(
                MessageType.ERROR,
                "common.errors.notFound",
                Map.of("entity", "YourEntity", "id", restEntity.getId()),
                List.of("id")
            );
            restEntity.setMessages(List.of(errorMsg));
            results.add(restEntity);
        }
    }

    return new YourListRestEntity(null, null, results);
}
```

# API Endpoint Structure

**URL Pattern:**
```
POST /admin/api/{entity-name-plural}/bulk-create-or-update
```

**Request:**
- Content-Type: `application/json`
- Body: Array of RestEntity objects (max 100)
- Each entity may or may not include `id` field

**Response:**
- HTTP 200 OK (even with partial failures)
- Body: List of processed entities with success/error indicators
- Check `$messages` property for errors on individual entities

# Best Practices

## For API Clients

1. **Use natural keys** for entities with business identifiers (Unit, Currency, Language, etc.)
   - Include the natural key field (symbol, currencyKey, isoKey, etc.)
   - System will update if exists, create if not

2. **Use smart matching** for PriceRow
   - Omit `id` field and provide all identifying fields
   - System will find and update the correct price automatically

3. **Use ID-based updates** when you have the technical ID
   - Include the `id` field
   - More efficient than smart matching

4. **Batch operations efficiently**
   - Maximum 100 entities per request
   - Consider parallel requests for larger datasets

## For Implementers

1. **Validate batch size** - Enforce maximum 100 entities per request
2. **Support partial success** - Return results for all entities even if some fail
3. **Use transactions** - Ensure consistency within each entity's create/update
4. **Log errors** - Record validation and matching failures for debugging
5. **Update Postman collection** - Add example requests for bulk operations
6. **Document matching fields** - Clearly specify which fields are used for matching

# Limitations

- Maximum 100 entities per request (prevent performance issues)
- All identifying/natural key fields must be provided for matching to work
- Referenced entities (unitRef, currencyRef, etc.) must exist before assignment
- Null values in optional fields are treated as "no constraint" for matching

# Examples

## Example 1: Unit Bulk Create/Update (Natural Key)

```json
POST /admin/api/units/bulk-create-or-update
Content-Type: application/json

[
  {
    "symbol": "kg",
    "name": {"en": "Kilogram", "de": "Kilogramm"},
    "measure": "MASS"
  },
  {
    "symbol": "m",
    "name": {"en": "Meter", "de": "Meter"},
    "measure": "LENGTH"
  }
]
```

Result: Updates existing units or creates new ones based on `symbol`

## Example 2: PriceRow Bulk Create/Update (Smart Matching)

```json
POST /admin/api/pricerows/bulk-create-or-update
Content-Type: application/json

[
  {
    "pricedResourceId": "PRODUCT-001",
    "priceValue": 99.99,
    "minQuantity": 1,
    "unitRef": "piece",
    "currencyRef": "EUR",
    "taxClassRef": "de-vat-full",
    "taxIncluded": false,
    "priceType": "SALES_PRICE"
  },
  {
    "id": 123,
    "pricedResourceId": "PRODUCT-002",
    "priceValue": 149.99,
    "minQuantity": 1,
    "unitRef": "piece",
    "currencyRef": "EUR",
    "taxClassRef": "de-vat-full",
    "taxIncluded": false,
    "priceType": "SALES_PRICE"
  }
]
```

Result:
- First item: Smart matching finds existing price or creates new one
- Second item: ID-based update of price row 123

# Testing

## Test Scenarios

1. **Create new entities** (no match found)
2. **Update existing entities** (match found via natural key or smart matching)
3. **Mixed create and update** in single request
4. **ID-based updates**
5. **Validation errors** (partial success)
6. **Maximum batch size** enforcement (>100 entities)
7. **Referenced entities not found** errors

## Postman Collection

Add bulk operation examples to Postman collection:
- Happy path: successful bulk create/update
- Validation errors: missing mandatory fields
- Not found errors: invalid references
- Mixed operations: some succeed, some fail

# Relevant Resources

- [Bulk Update of Price Rows Guide](../../../service/doc/030-features/030-bulk-update-pricerows.md) - Comprehensive guide specific to PriceRow smart matching
- [Development Guide - Service Layer](../../../service/doc/020-development/012-development-guide-service-layer.md) - Service layer patterns
- [Development Guide - Controller Layer](../../../service/doc/020-development/014-development-guide-controller-layer.md) - REST API patterns
- Postman Collection: `service/postman/pps-postmancollection.json`
