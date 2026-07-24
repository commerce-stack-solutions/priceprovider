# $meta Expand – Entity Metadata API

## Overview

All admin entity endpoints support a `$meta` expand parameter that returns structural metadata about the entity: which fields are identity keys, which are mandatory, and what values are valid for enum-typed fields.

```
GET /api/admin/groups/?$expand=$meta
GET /api/admin/groups/GRP-001?$expand=$includes,$info,$meta
```

## Response Structure

```json
{
  "id": "78b50c3e-3694-4c8f-8922-cc4dfec87f3a",
  "path": "ORG-MY-COMPANY/ORG-IT-DEPT",
  "groupType": "ORGANIZATION",
  "$meta": {
    "identityFields": ["id"],
    "mandatoryFields": ["path", "groupType"],
    "referenceKeyFields": ["path"],
    "enumValues": {
      "groupType": ["ORGANIZATION", "PROMOTION"]
    },
    "fields": [
      {
        "name": "id",
        "type": "String",
        "readOnly": true
      },
      {
        "name": "path",
        "type": "String",
        "readOnly": false
      },
      {
        "name": "groupType",
        "type": "Enum",
        "enumValues": ["ORGANIZATION", "PROMOTION"],
        "readOnly": false
      },
      {
        "name": "name",
        "type": "String",
        "readOnly": false
      }
    ]
  }
}
```

| Field                | Description |
|----------------------|-------------|
| `identityFields`     | Fields that serve as primary keys (detected from `@jakarta.persistence.Id`) |
| `mandatoryFields`    | Fields required for create/update (declared with `@MandatoryField`, plus non-generated `@Id` fields) |
| `referenceKeyFields` | The human-readable alternative key field(s) used in JSON references and query filters (see `@ReferenceKey` below). Falls back to `identityFields` when no `@ReferenceKey` is declared. |
| `enumValues`         | All valid string constants for every enum-typed field (mandatory **and** optional) |
| `fields`             | Comprehensive list of `FieldMetadata` describing each field's name, dynamic type class, read-only flag, decimal precision, and enum values to support generic dynamic forms. |

### Field Metadata Attributes

Each object in the `fields` array contains:

- `name`: The exact field name in the entity class.
- `type`: Classified frontend field type mapping:
  - `Number`: For fields assignable to numeric types (e.g. `BigDecimal`, `Double`, `Integer`, `Long`).
  - `Enum`: For enum-typed or `@MetaDynamicEnum`-annotated fields.
  - `LocalizedString`: For dynamic multi-language text fields represented as maps (`Map<String, String>`).
  - `Reference`: For single entity relationship reference keys.
  - `Set<Reference>`: For relationship collection/set reference keys (e.g., `Set<GroupEntity>`).
  - `Boolean`: For `boolean` and `Boolean` fields.
  - `DateTime`: For date and time fields (e.g. `OffsetDateTime`, `Instant`, `LocalDate`).
  - `String`: Fallback text type.
- `readOnly`: Boolean indicating if the field is write-blocked. Automatically true for:
  - Technical fields (`createdAt`, `lastModifiedAt`).
  - Primary key (`@Id`) fields annotated with `@GeneratedValue` or `@GeneratedId`.
- `precision`: Number of decimal places, derived from `@MetaPrecision` or `@Column(scale = ...)`.
- `enumValues`: List of string options if the type is classified as `Enum`.

## How It Works (Backend)

### Annotations

Entity fields are annotated directly:

```java
@Entity
public class GroupEntity {
    @Id
    @GeneratedId          // → identityField ONLY – NOT mandatory (app generates via @PrePersist)
    private String id;    // auto-generated UUID

    @ReferenceKey         // → listed in referenceKeyFields; used for queries and JSON refs
    @MandatoryField
    private String path;  // human-readable key, e.g. "ORG-MY-COMPANY/ORG-IT-DEPT"

    @Enumerated(EnumType.STRING)
    @MandatoryField
    private GroupType groupType;  // → enum values always included in enumValues

    @Enumerated(EnumType.STRING)
    private PriceType priceType;  // optional enum – values still included
}

@Entity
public class PriceRowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // → identityField ONLY – NOT mandatory (DB generates the value)

    @MandatoryField
    private String pricedResourceId;
}
```

#### Auto-mandatory rule for `@Id` fields

A field annotated with `@Id` is **implicitly mandatory** (the caller must supply it) unless it also carries `@GeneratedValue` or `@GeneratedId`. When either of these is present the value is assigned automatically and the client must not (and cannot) provide one.

#### `@GeneratedId`

`@GeneratedId` (`commons.dataaccess.idgenerator`) is the equivalent of `@GeneratedValue` for entities that use `@PrePersist`-based ID generation instead of JPA sequence-based generation (e.g. when targeting Cloud Spanner). See [Reference Key and ID Generation](080-reference-key-and-id-generation.md) for full details.

#### `@ReferenceKey` and `referenceKeyFields`

`@ReferenceKey` (`commons.dataaccess`) marks a field as the human-readable alternative key used in JSON references and Lucene-style query filters. The field name is exposed under `$meta.referenceKeyFields`.  When no `@ReferenceKey` is declared, `referenceKeyFields` falls back to the `identityFields` list.

`SpecificationBuilder` uses `referenceKeyFields` (via reflection) when building `.hasAny` / `.hasAll` predicates on collection fields, so callers can filter by readable keys (e.g. `groupRefs.hasAny:ORG-MY-COMPANY/ORG-IT-DEPT`) rather than UUID values.

See [Reference Key and ID Generation](080-reference-key-and-id-generation.md) for full details and a guide on adding `@ReferenceKey` to new entities.

#### `@MetaPrecision`

`@MetaPrecision` (`commons.dataaccess.meta`) marks a numeric field (like `BigDecimal`, `Double`, etc.) with its decimal scale/precision (e.g. `2`). The builder scans for this annotation and exposes the value under `$meta.fields[x].precision` so the frontend form inputs can enforce exact decimal stepping.

### MetaInfoBuilder

`MetaInfoBuilder.build(EntityClass.class)` scans the class hierarchy via reflection and returns a populated `MetaInfo`:

```java
MetaInfo meta = MetaInfoBuilder.build(GroupEntity.class);
```

### EntityMetaInfoRegistry (startup cache)

To avoid repeated reflection on every request, `MetaInfoRegistryConfig` pre-builds and registers all `MetaInfo` instances once at application startup (`@PostConstruct`). Facades retrieve the cached instance:

```java
result.setMeta(entityMetaInfoRegistry.getMetaInfo(GroupEntity.class));
```

### Supported Entities

| Entity              | Endpoint                           |
|---------------------|------------------------------------|
| `GroupEntity`       | `/api/admin/groups/`               |
| `OrganizationEntity`| `/api/admin/organizations/`        |
| `UnitEntity`        | `/api/admin/units/`                |
| `CurrencyEntity`    | `/api/admin/currencies/`           |
| `LanguageEntity`    | `/api/admin/languages/`            |
| `CountryEntity`     | `/api/admin/countries/`            |
| `ChannelEntity`     | `/api/admin/channels/`             |
| `TaxClassEntity`    | `/api/admin/taxclasses/`           |
| `PriceRowEntity`    | `/api/admin/pricerows/`            |

## How It Works (Frontend)

The Angular frontend requests `$meta` in every single-entity GET call:

```typescript
getGroup(id: string): Observable<Group> {
  return this.http.get<Group>(`${this.apiUrl}/${id}?$expand=$includes,$info,$meta`);
}
```

### Form Components

Form components use `$meta` to:

1. **Populate enum selectors** from `$meta.enumValues` instead of hardcoded arrays
2. **Mark mandatory fields** using the standalone `IsMandatoryPipe`

```html
<label>Group Type @if ('groupType' | isMandatory: meta()) { <span class="text-danger">*</span> }</label>
<app-enum-selector [options]="meta()?.enumValues?.['groupType'] ?? []" ... />
```

Note: Ensure that you import `IsMandatoryPipe` in your component's `@Component({ imports: [...] })` array so it can be used in the template.

In **create mode** (no entity ID yet), the form fetches the list endpoint with `$expand=$meta` to obtain metadata before the entity exists.

### Generic Form Component (`GenericFormComponent`)

To avoid creating custom pages/templates for every entity type, the platform includes a dynamic, metadata-driven generic form component at route paths:

- Add form: `/:lang/generic/:entityType/add`
- Edit form: `/:lang/generic/:entityType/:id/edit`

The `GenericFormComponent` works fully generically:
1. It reads the `:entityType` plural parameter (e.g. `currencies`, `groups`, `taxclasses`).
2. It fetches the metadata (`admin/api/:entityType/$meta`) and dynamically constructs a `FormGroup`.
3. It maps each field in `fields` metadata to its respective compiled control name and dynamically renders the appropriate UI widget (standard textbox, number input, checkbox, enum-selector, localized string editor, or single/multi-reference editor).
4. On submit, it automatically handles building either PATCH operations (updates) or structured payloads (creates) and routes back to the main plural list path.

## Adding $meta Support to a New Entity

1. Annotate entity fields — **do not** add `@MandatoryField` to `@Id` fields:
   ```java
   @Id private String id;           // auto-mandatory (no @GeneratedValue / @GeneratedId)
   @MandatoryField private String name;
   @Enumerated(EnumType.STRING) private MyEnum status; // enum values auto-included
   ```
   For entities with a JPA-generated primary key:
   ```java
   @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id; // identity only, NOT mandatory
   ```
   For entities with an app-generated primary key (e.g. UUID via `@PrePersist`):
   ```java
   @Id @GeneratedId private String id; // identity only, NOT mandatory
   ```
   For entities that have a separate human-readable business key:
   ```java
   @ReferenceKey
   @Column(unique = true, nullable = false)
   private String path;  // appears in $meta.referenceKeyFields; used in hasAny/hasAll queries
   ```

2. Register in `MetaInfoRegistryConfig`:
   ```java
   entityMetaInfoRegistry.register(MyEntity.class, MetaInfoBuilder.build(MyEntity.class));
   ```

3. Inject `EntityMetaInfoRegistry` into the facade and call `setMeta` when `$meta` is in `expand`:
   ```java
   if (expand != null && expand.contains("$meta")) {
       result.setMeta(entityMetaInfoRegistry.getMetaInfo(MyEntity.class));
   }
   ```
