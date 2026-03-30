# Development Guide – Data Access Layer

The data access layer contains JPA entities and their corresponding repository interfaces. It is responsible for all database interactions.

For an overview of the layering concept, see [Architecture Overview](../010-architecture/010-overview.md#architectural-layers).

## Package Structure

```
de.ebusyness.priceproviderservice.dataaccess/
├── {entity}/
│   ├── entity/
│   │   └── {Entity}.java               # JPA entity class
│   ├── {Entity}Repository.java         # Spring Data JPA repository
│   └── {Entity}Projection.java         # Optional: view projections
```

## JPA Entities

Entity classes represent the persisted data model. They are plain JPA-annotated classes.

### AuditableEntity

All entities in this project **must** implement the `AuditableEntity` interface from `de.ebusyness.commons.dataaccess.entity`. This interface requires two audit timestamp fields that are automatically managed by the service layer's `EntityService.updateAuditTimestamps()` method before each `save()`.

```java
package de.ebusyness.commons.dataaccess.entity;

import java.time.OffsetDateTime;

public interface AuditableEntity {
    OffsetDateTime getCreatedAt();
    void setCreatedAt(OffsetDateTime createdAt);
    OffsetDateTime getLastModifiedAt();
    void setLastModifiedAt(OffsetDateTime lastModifiedAt);
}
```

The `EntityService` default method sets `createdAt` on first save and updates `lastModifiedAt` on every save:

```java
default void updateAuditTimestamps(T entity) {
    if (entity instanceof AuditableEntity) {
        AuditableEntity auditable = (AuditableEntity) entity;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (auditable.getCreatedAt() == null) {
            auditable.setCreatedAt(now);
        }
        auditable.setLastModifiedAt(now);
    }
}
```

### Example Entity

```java
package de.ebusyness.priceproviderservice.dataaccess.unit.entity;

import de.ebusyness.commons.dataaccess.entity.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "units")
public class UnitEntity implements AuditableEntity {

    @Id
    private String symbol;

    @Column(name = "measure")
    private String measure;

    @Column(precision = 19, scale = 9)
    private BigDecimal factor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_unit_symbol")
    private UnitEntity baseUnitRef;

    @ElementCollection
    @CollectionTable(name = "unit_localized_names", joinColumns = @JoinColumn(name = "symbol"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "name")
    @MetaMandatoryField
    private Map<String, String> name;

    // Audit timestamps - managed by EntityService.updateAuditTimestamps()
    private OffsetDateTime createdAt;
    private OffsetDateTime lastModifiedAt;

    // Getters and setters...

    @Override
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public OffsetDateTime getLastModifiedAt() { return lastModifiedAt; }

    @Override
    public void setLastModifiedAt(OffsetDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
}
```

### Conventions

- Entity class names end with `Entity` (e.g., `UnitEntity`, `PriceRowEntity`)
- **Every entity MUST implement `AuditableEntity`** to ensure consistent audit tracking
- Use `@Table(name = ...)` to explicitly define table names
- Use `@Column(name = ...)` to map fields to specific column names
- Prefer `@ElementCollection` for simple value collections (localized names, tags)
- Use `@ManyToOne` / `@OneToMany` for entity associations
- For field names that represent relations always use Ref as ending. (e.g. language -> languageRef, allowedCountries -> allowedCountryRefs) This naming convention finally helps matching the REST API style guide for references.   

## Repositories

Repository interfaces extend `JpaRepository` and optionally `JpaSpecificationExecutor` (for query filtering support).

### Basic Repository

```java
package de.ebusyness.priceproviderservice.dataaccess.unit;

import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitEntityRepository extends JpaRepository<UnitEntity, String>,
                                               JpaSpecificationExecutor<UnitEntity> {
    // Spring Data JPA provides basic CRUD automatically
    // Add custom query methods here
}
```

### Naming Convention

- Repository class names end with `Repository` (e.g., `UnitEntityRepository`, `PriceRowEntityRepository`)
- Always annotate with `@Repository`

## Query Filtering via Specifications

Adding `JpaSpecificationExecutor<T>` to the repository enables dynamic query filtering via `Specification<T>` objects. This is the recommended approach for all list/search endpoints.

For the complete implementation guide, see [020-query-filtering-implementation.md](../030-features/020-query-filtering-implementation.md).

## Data Initialization

> **Note:** Although data initialization uses `EntityService` (a service layer component), it is documented here because it concerns populating the database with initial data and is tightly coupled to the entity/repository structure. For the service layer details, see [012-development-guide-service-layer.md](012-development-guide-service-layer.md).

The service supports both automatic data initialization at startup and manual data loading through a GUI interface.

### Bootstrap Mechanism

When the database is completely empty (no permissions AND no roles exist), the service automatically creates minimal permissions and roles to enable initial access. This "bootstrap" mechanism runs at service startup via `@PostConstruct` in `SetupDataImportManager.bootstrapMinimalAccess()`.

**Bootstrap creates:**
- `priceprovider.admin:ServiceInitialization:write` permission
- `priceprovider.admin:AppRole:read` permission
- `priceprovider.admin:Admin` role containing both permissions

This solves the chicken-and-egg problem of needing permissions to load permissions. For complete details on bootstrap behavior, Keycloak configuration, and troubleshooting, see [Service Initialization](../030-features/070-service-initialization.md).

### Data Loading Modes

The service supports two data loading approaches:

1. **Automatic loading** (legacy): Data is loaded automatically at startup based on configuration flags
2. **Manual loading** (recommended): Data is loaded on-demand via the Service Initialization GUI

The loading mode is controlled by configuration flags in `application.yaml`.

### Architecture

```
JSON Files → AbstractSetupDataImporter.importFilesFromDirectory()
           → filters files by {EntityTypeName}.*.json prefix, sorted alphanumerically
           → importFile() for each matched file
           → EntityService.getTargetClass() (gets entity type)
           → Jackson deserializes JSON to List<EntityType>
           → EntityService.save() (persists each entity)
```

**Components:**
- **`EntityService`**: Interface providing `getTargetClass()` and `save()`
- **`AbstractSetupDataImporter`**: Abstract base class implementing `SetupDataImporter`; provides `loadEssentialData()` / `loadSampleData()` driven by config flags and auto-discovers files from the data folder
- **`SetupDataImporter`**: Interface defining the contract for data loaders
- **`{Entity}DataImporter`**: Concrete implementations per entity (e.g., `UnitDataImporter`)

### Implementation Steps

#### 1. EntityService

Each entity's service must implement `EntityService<T>`:

```java
@Service
public class LanguageEntityService implements EntityService<LanguageEntity> {
    @Override
    public Class<LanguageEntity> getTargetClass() {
        return LanguageEntity.class;
    }
    // ... other service methods
}
```

#### 2. Data Loader

```java
@Component
public class LanguageDataImporter extends AbstractSetupDataImporter<LanguageEntity> {

    @Autowired
    public LanguageDataImporter(LanguageEntityService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 50; // Lower number = higher priority (loads first)
    }

    @Override
    public String getEntityTypeName() {
        return "Language";
    }
}
```

`AbstractSetupDataImporter` provides default `loadEssentialData()` and `loadSampleData()` implementations that automatically discover and load all matching files from the respective data folder directories, controlled by the `essential-data-on` and `sample-data-on` configuration flags.

#### 3. Data Files

Place JSON files in the resources directory following the naming convention:

```
{EntityTypeName}.{4-digit-number}.{optional-descriptor}.json
```

- Essential data: `src/main/resources/initialize/essential/`
- Sample data: `src/main/resources/initialize/sample/`

The 4-digit number controls the load order within an entity type (alphanumeric sort). Files for an entity type are identified by their prefix matching `{EntityTypeName}.`.

Examples:
- `Language.0010.json` — essential languages
- `Currency.0010.json` — essential currencies
- `PriceRow.0010.DEMO-PRODUCT-001.EUR.SALES_PRICE.json` — sample price rows
- `PriceRow.0020.DEMO-PRODUCT-001.USD.SALES_PRICE.json` — more sample price rows

Example `Language.0010.json`:

```json
[
  { "isoKey": "de", "active": true, "mandatory": true },
  { "isoKey": "en", "active": true, "mandatory": true }
]
```

### Priority System

Data loaders execute in priority order (lower number = higher priority):

| Priority | Entity        |
|----------|---------------|
| 50       | Languages     |
| 60       | Currencies    |
| 65       | Countries     |
| 70       | Groups, TaxClasses |
| 75       | Organizations |
| 80       | Channels      |
| 100      | Units         |
| 200      | PriceRows     |

### Configuration

```yaml
# application.yaml (production/default - manual loading mode)
service-config:
  initialize:
    data-folder: "classpath:data/"
    essential-data-on: false  # Do not auto-load essential data
    sample-data-on: false     # Do not auto-load sample data

# application-dev.yaml (development profile - automatic loading mode)
service-config:
  initialize:
    essential-data-on: true   # Auto-load essential data on startup
    sample-data-on: true      # Auto-load sample data on startup
```

- **`essential-data-on`**: When `true`, automatically loads all `{EntityTypeName}.*.json` files from the `essential/` subfolder at startup. When `false`, data must be loaded manually via the GUI.
- **`sample-data-on`**: When `true`, automatically loads all `{EntityTypeName}.*.json` files from the `sample/` subfolder at startup. When `false`, data must be loaded manually via the GUI.

**Recommended configuration:**
- Production: Both flags `false` → use GUI for manual, controlled data loading
- Development: Both flags `true` → automatic loading for faster development iteration

For GUI-based manual loading and bootstrap details, see [Service Initialization](../030-features/070-service-initialization.md).

## Meta Annotations (`@MetaMandatoryField`,`@Id`)

Entity fields can be annotated with meta annotations to expose structural metadata via the `$meta` expand parameter.  Full details are in [060-meta-annotation-concept.md](../030-features/060-meta-annotation-concept.md).

### `@MetaMandatoryField`

Use `@MetaMandatoryField` (package `de.ebusyness.commons.dataaccess.meta`) to mark any **non-`@Id`** field as mandatory:

```java
@MetaMandatoryField
private String name;

@ManyToOne(fetch = FetchType.LAZY)
@MetaMandatoryField
private CountryEntity countryRef;   // relation field — null check enforced at service layer
```

### Auto-mandatory `@Id` fields

A field annotated with `@Id` is **automatically** treated as an identity field and as a mandatory field — unless it also carries `@GeneratedValue` (in which case the DB assigns the value and the caller must not supply it).

```java
@Id
private String id;           // → identityField AND mandatory (auto rule)

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;             // → identityField ONLY — NOT mandatory (DB-generated)
```

> Do not add `@MetaMandatoryField` to an `@Id` field — the auto-mandatory rule already covers it.
