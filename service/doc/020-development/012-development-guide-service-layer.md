# Development Guide – Service Layer

The service layer contains domain services and business services. It is responsible for all business logic, transactions, and domain validation.

For an overview of the layering concept, see [Architecture Overview](../010-architecture/010-overview.md#architectural-layers).

## Package Structure

```
io.commercestacksolutions.priceproviderservice.service/
├── {entity}/
│   ├── {Entity}Service.java              # Interface defining the contract
│   ├── {Entity}ServiceImpl.java          # Default implementation
│   ├── validation/                        # Validation rules
│   │   └── *Rule.java
│   └── setup/                             # Data importers
│       └── {Entity}DataImporter.java
```

## Interface Driven Design (IDD) in the Service Layer

All services follow IDD principles: define an interface, implement it in a separate class, and only inject the interface elsewhere.

### Step 1: Define the Interface

Service interfaces should extend `EntityService<T>` to inherit common entity operations:

```java
package io.commercestacksolutions.priceproviderservice.service.unit;

import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Service interface for Unit entity operations.
 */
public interface UnitService extends EntityService<UnitEntity> {

    void deleteUnit(String symbol);

    UnitEntity findBySymbol(String symbol);

    Page<UnitEntity> getUnits(int page, int pageSize, List<String> sortBy, String sortDirection);

    UnitEntity getUnit(String symbol);
}
```

### Step 2: Implement the Service

```java
package io.commercestacksolutions.priceproviderservice.service.unit;

import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitEntityRepository unitEntityRepository;

    private final EntityValidator<UnitEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public UnitServiceImpl(List<ValidationRule<UnitEntity>> validationRules) {
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(UnitEntity.class);
    }

    @Override
    public Class<UnitEntity> getTargetClass() {
        return UnitEntity.class;
    }

    @Override
    public EntityValidator<UnitEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public UnitEntity save(UnitEntity unitEntity) throws EntityValidationException {
        validateEntity(unitEntity);
        updateAuditTimestamps(unitEntity); // sets createdAt / lastModifiedAt automatically
        return unitEntityRepository.save(unitEntity);
    }

    @Override
    public Page<UnitEntity> getUnits(int page, int pageSize, List<String> sortBy,
                                      String sortDirection, String query)
            throws InvalidParameterException, QueryParseException {
        Sort sort = Sort.by(Sort.Direction.fromString(
                sortDirection != null ? sortDirection : "asc"),
                sortBy != null && !sortBy.isEmpty() ? sortBy.toArray(new String[0]) : new String[]{"symbol"});
        Specification<UnitEntity> spec = null;
        if (query != null && !query.isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            spec = new SpecificationBuilder<UnitEntity>().build(expression);
        }
        return spec != null
                ? unitEntityRepository.findAll(spec, PageRequest.of(page, pageSize, sort))
                : unitEntityRepository.findAll(PageRequest.of(page, pageSize, sort));
    }

    @Override
    public UnitEntity getUnit(String symbol) {
        return unitEntityRepository.findById(symbol).orElse(null);
    }

    @Override
    public void deleteUnit(String symbol) {
        unitEntityRepository.deleteById(symbol);
    }
}
```

### Step 3: Inject and Use the Interface

Always inject the interface type, never the concrete implementation:

```java
@Service
public class UnitFacadeImpl implements UnitFacadeService {

    private final UnitService unitService; // Interface, not UnitServiceImpl

    @Autowired
    public UnitFacadeImpl(UnitService unitService) {
        this.unitService = unitService;
    }
}
```

### Naming Conventions

| Artifact | Convention | Example |
|----------|-----------|---------|
| Interface | `{Entity}Service` | `UnitService` |
| Implementation | `{Entity}ServiceImpl` | `UnitServiceImpl` |
| Alt. implementation | `{Entity}Service{Variant}Impl` | `UnitServiceAuditedImpl` |

## Service-Layer Validation

Domain validation is centralized in the service layer to ensure all callers (facades, other services) go through the same validation pipeline. Complex, cross-entity checks that need repositories must run here.

### ValidationRule Interface

```java
public interface ValidationRule<T> {
    List<Message> validate(T entity);
}
```

### Implementing a Validation Rule

Annotate with `@Component` so Spring discovers and auto-wires it:

```java
package io.commercestacksolutions.priceproviderservice.service.language.validation;

import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Validation rule: A language cannot be inactive and mandatory at the same time.
 */
@Component
public class LanguageInactiveMandatoryRule implements ValidationRule<LanguageEntity> {

    @Override
    public List<Message> validate(LanguageEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }
        if (Boolean.TRUE.equals(entity.getMandatory()) && !Boolean.TRUE.equals(entity.getActive())) {
            Message errorMessage = new Message(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_LANGUAGE_MANDATORY_MUST_BE_ACTIVE,
                Arrays.asList("active", "mandatory")
            );
            return Collections.singletonList(errorMessage);
        }
        return Collections.emptyList();
    }
}
```

### Cross-Entity Validation with Service Dependency

```java
package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.unit.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

/**
 * Validation rule: unit reference must exist
 */
@Component
public class UnitReferenceExistsRule implements ValidationRule<PriceRowEntity> {

    private final UnitService unitEntityService;

    @Autowired
    public UnitReferenceExistsRule(UnitService unitEntityService) {
        this.unitEntityService = unitEntityService;
    }

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }
        if (entity.getUnit() != null && entity.getUnit().getSymbol() != null) {
            if (unitEntityService.getUnit(entity.getUnit().getSymbol()) == null) {
                Message errorMessage = new Message(
                    Message.MessageType.ERROR,
                    "Invalid unit reference: " + entity.getUnit().getSymbol() + " does not exist",
                    List.of("unitRef")
                );
                return Collections.singletonList(errorMessage);
            }
        }
        return Collections.emptyList();
    }
}
```

### Key Rules for Validation Messages

1. **MUST** always provide field references in the `fields` array when the error relates to specific fields
2. **MUST NOT** include HTTP status codes in message objects for single-resource REST calls
3. **MUST NOT** include technical details, URLs, or stack traces in user-facing error messages
4. **SHOULD** use message keys (constants in `MessageKeys`) instead of hardcoded strings – translations for actionable messages are provided in the frontend app

### Message Keys

Always define message keys as constants in `MessageKeys`:

```java
// io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys
public static final String ERROR_LANGUAGE_MANDATORY_MUST_BE_ACTIVE = "error.language.mandatory.must.be.active";
public static final String ERROR_UNIT_REFERENCE_NOT_FOUND = "error.unit.reference.not.found";
```

### Testing Validation Rules

Write focused unit tests for each validation rule:

```java
class LanguageInactiveMandatoryRuleTest {

    private final LanguageInactiveMandatoryRule rule = new LanguageInactiveMandatoryRule();

    @Test
    void inactiveMandatoryLanguage_ShouldFail() {
        LanguageEntity language = new LanguageEntity();
        language.setActive(false);
        language.setMandatory(true);

        List<Message> errors = rule.validate(language);

        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
        assertTrue(errors.get(0).getFields().contains("active"));
        assertTrue(errors.get(0).getFields().contains("mandatory"));
    }

    @Test
    void activeLanguage_ShouldPass() {
        LanguageEntity language = new LanguageEntity();
        language.setActive(true);
        language.setMandatory(true);

        assertTrue(rule.validate(language).isEmpty());
    }
}
```

## Creating Alternative Implementations

You can create alternative implementations of service interfaces for different use cases (caching, auditing, etc.):

```java
@Service
@Primary // Use this implementation by default
public class UnitServiceAuditedImpl implements UnitService {

    private final UnitService delegate;
    private final AuditService auditService;

    @Autowired
    public UnitServiceAuditedImpl(
            @Qualifier("unitServiceImpl") UnitService delegate,
            AuditService auditService) {
        this.delegate = delegate;
        this.auditService = auditService;
    }

    @Override
    public UnitEntity save(UnitEntity unit) throws EntityValidationException {
        UnitEntity saved = delegate.save(unit);
        auditService.recordChange("unit", saved.getSymbol(), "SAVE");
        return saved;
    }
    // Delegate other methods...
}
```

## Query Filtering in the Service Layer

Services that support list/search functionality integrate query filtering through the `QueryParser` and `SpecificationBuilder`. The service layer is responsible for parsing the `q` query string (received from the facade/controller) into a `Specification<T>` that is passed to the repository.

```java
// In UnitServiceImpl.getUnits():
if (query != null && !query.isEmpty()) {
    QueryExpression expression = queryParser.parse(query);
    Specification<UnitEntity> spec = new SpecificationBuilder<UnitEntity>().build(expression);
    return unitEntityRepository.findAll(spec, pageable);
}
```

For the complete implementation guide (how to register filterable fields, supported operators, etc.), see [020-query-filtering-implementation.md](../030-features/020-query-filtering-implementation.md).

## `RequireMandatoryFieldsRule` — Annotation-Driven Mandatory Field Validation

Instead of writing a custom `ValidationRule` per entity to check mandatory fields, use the shared `RequireMandatoryFieldsRule<T>` from `io.commercestacksolutions.commons.service.entity.validation.rules`.

This rule reads the mandatory field list from the `EntityMetaInfoRegistry` (populated at startup from `@Id` / `@MandatoryField` annotations) and validates all required fields are non-null (and non-blank for `String`s).

Full details on the annotation system are in [060-meta-annotation-concept.md](../030-features/060-meta-annotation-concept.md).

### Wiring via configuration

`RequireMandatoryFieldsRule` is **not** a Spring component — declare one typed `@Bean` per entity in `RequireMandatoryFieldsValidationConfig`:

```java
@Configuration
public class RequireMandatoryFieldsValidationConfig {

    @Bean
    public ValidationRule<GroupEntity> groupRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
    }

    @Bean
    public ValidationRule<TaxClassEntity> taxClassRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(TaxClassEntity.class, registry);
    }

    // ... one @Bean per entity
}
```

Spring auto-wires these beans into each `*ServiceImpl` constructor via `List<ValidationRule<T>> validationRules`.

### Benefits

- **Single source of truth** — field annotations on the entity drive both the API's `$meta` response and the service-layer validation.
- **No code duplication** — no per-entity `*MandatoryFieldsRule` class needed.
- **Automatically kept in sync** — adding `@MandatoryField` to a new field is enough; no rule code needs to change.
