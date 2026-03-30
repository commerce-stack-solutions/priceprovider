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
  "id": "GRP-001",
  "name": "Sample Group",
  "groupType": "PROMOTION",
  "$meta": {
    "identityFields": ["id"],
    "mandatoryFields": ["id", "name", "groupType"],
    "enumValues": {
      "groupType": ["ORGANIZATION", "PROMOTION"]
    }
  }
}
```

| Field             | Description |
|-------------------|-------------|
| `identityFields`  | Fields that serve as primary keys (detected from `@jakarta.persistence.Id`) |
| `mandatoryFields` | Fields required for create/update (declared with `@MetaMandatoryField`) |
| `enumValues`      | All valid string constants for every enum-typed field (mandatory **and** optional) |

## How It Works (Backend)

### Annotations

Entity fields are annotated directly:

```java
@Entity
public class GroupEntity {
    @Id                   // → identityField AND auto-mandatory (no @GeneratedValue)
    private String id;

    @MetaMandatoryField
    private String name;

    @Enumerated(EnumType.STRING)
    @MetaMandatoryField
    private GroupType groupType;  // → enum values always included in enumValues

    @Enumerated(EnumType.STRING)
    private PriceType priceType;  // optional enum – values still included
}

@Entity
public class PriceRowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // → identityField ONLY – NOT mandatory (DB generates the value)

    @MetaMandatoryField
    private String pricedResourceId;
}
```

#### Auto-mandatory rule for `@Id` fields

A field annotated with `@Id` is **implicitly mandatory** (the caller must supply it) unless it also carries `@GeneratedValue`. When `@GeneratedValue` is present the database assigns the value, so the client must not (and cannot) provide one.

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

## Adding $meta Support to a New Entity

1. Annotate entity fields — **do not** add `@MetaMandatoryField` to `@Id` fields:
   ```java
   @Id private String id;           // auto-mandatory (no @GeneratedValue)
   @MetaMandatoryField private String name;
   @Enumerated(EnumType.STRING) private MyEnum status; // enum values auto-included
   ```
   For entities with a generated primary key:
   ```java
   @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id; // identity only, NOT mandatory
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
