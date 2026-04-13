# @ReferenceKey, @GeneratedId, and Exchangeable ID Generation

## Overview

This document describes two complementary annotation-driven mechanisms introduced to cleanly separate **technical identifiers** (auto-generated UUIDs) from **human-readable business keys** used in JSON payloads, query filters, and navigation links:

| Concern | Annotation | Where |
|---------|-----------|-------|
| Mark a field as the human-readable key for refs/queries | `@ReferenceKey` | `commons.dataaccess` |
| Mark a `@Id` field as auto-generated (not client-supplied) | `@GeneratedId` | `commons.dataaccess.idgenerator` |
| Pluggable ID generation strategy | `IdGenerator` / `UUIDStringIdGenerator` | `commons.dataaccess.idgenerator` |

---

## @ReferenceKey

### Purpose

`@ReferenceKey` marks a field as the **human-readable alternative key** for an entity.  
This field is used in:

1. **Query filtering** (`SpecificationBuilder` `.hasAny` / `.hasAll`) – collection membership filters match on the `@ReferenceKey` field value rather than the technical `@Id`.
2. **`$meta.referenceKeyFields`** – clients can discover which field to use when constructing references (see [Meta Annotation Concept](060-meta-annotation-concept.md)).

### Example – GroupEntity

```java
@Entity
public class GroupEntity {

    @Id
    @GeneratedId                        // auto-generated UUID – never sent in JSON refs
    private String id;

    @ReferenceKey                       // human-readable key – used in JSON refs and queries
    @Column(unique = true, nullable = false)
    private String path;                // e.g. "ORG-MY-COMPANY/ORG-IT-DEPT"
}
```

With this annotation in place, the Lucene query:

```
groupRefs.hasAny:ORG-MY-COMPANY/ORG-IT-DEPT
```

is resolved against the `path` field of the joined `GroupEntity` rows, not against the UUID `id`.

### Effect on SpecificationBuilder (hasAny / hasAll)

`SpecificationBuilder` uses `QueryReflectionUtil.findFilterKeyAttributeName()` to look for a `@ReferenceKey` field on the collection element type.  If one is found it is used as the match attribute; otherwise the `@Id` field is used as fallback.

```
Collection field  →  element type has @ReferenceKey?
                       YES → match on @ReferenceKey field (e.g. path)
                       NO  → match on @Id field (e.g. id)
```

**Class:** `io.commercestacksolutions.commons.query.SpecificationBuilder`  
**Helper:** `io.commercestacksolutions.commons.query.QueryReflectionUtil.findFilterKeyAttributeName()`

### Effect on $meta

`MetaInfoBuilder` scans for `@ReferenceKey` fields and populates `$meta.referenceKeyFields`.  
When no `@ReferenceKey` is declared the list falls back to the `@Id` field(s).

```json
{
  "$meta": {
    "identityFields": ["id"],
    "mandatoryFields": ["path", "groupType"],
    "referenceKeyFields": ["path"],
    "enumValues": { "groupType": ["ORGANIZATION", "PROMOTION"] }
  }
}
```

See [Meta Annotation Concept – referenceKeyFields](060-meta-annotation-concept.md#referencekey-and-referenceKeyfields) for the full response contract.

### When to Add @ReferenceKey

Add `@ReferenceKey` to any entity that has:
- A technical `@Id` that is **not** the natural business key (UUID, sequence number, …), **and**
- A separate unique natural key that callers use in JSON payloads and Lucene queries.

**Current usages:**

| Entity | @ReferenceKey field | Technical @Id |
|--------|---------------------|---------------|
| `GroupEntity` | `path` | `id` (String UUID) |
| `OrganizationEntity` | `path` | `id` (String UUID, inherited) |

**Likely future candidates:**

| Entity | Candidate field | Notes |
|--------|-----------------|-------|
| `AppPermissionEntity` | `name` | used as business key in AppRole permission lists |
| `AppRoleEntity` | `name` | used in JWT claims and role assignments |
| `CurrencyEntity` | `currencyKey` | used as currency reference in price rows |
| `UnitEntity` | `symbol` | used as unit reference in price rows |
| `TaxClassEntity` | `taxClassId` | used as tax class reference in price rows |

---

## @GeneratedId

### Purpose

`@GeneratedId` signals to `MetaInfoBuilder` that the `@Id` field value is **assigned automatically** (e.g. via `@PrePersist` with an `IdGenerator` bean) and therefore:

- **IS** included in `$meta.identityFields`
- Is **NOT** included in `$meta.mandatoryFields` (the client must not supply it)

This is the equivalent of `@GeneratedValue` for entities that cannot use JPA sequence-based generation (e.g. because the production database is Cloud Spanner, which does not support sequences).

### Example

```java
@Entity
public class GroupEntity {

    @Id
    @GeneratedId        // tells MetaInfoBuilder: auto-generated, not mandatory for callers
    private String id;
}
```

Without `@GeneratedId` (and without `@GeneratedValue`), `MetaInfoBuilder` would include `id` in `mandatoryFields`, forcing the client to supply a value.

### Auto-mandatory rule summary

| Annotations on @Id field | identityField | mandatoryField |
|--------------------------|---------------|----------------|
| `@Id` only | ✓ | ✓ (client must supply) |
| `@Id @GeneratedValue` | ✓ | ✗ (DB assigns value) |
| `@Id @GeneratedId` | ✓ | ✗ (app assigns via @PrePersist) |

---

## Exchangeable ID Generation

### IdGenerator Interface

```java
// commons.dataaccess.idgenerator
public interface IdGenerator {
    String generate();
}
```

Entities that use `@GeneratedId` call the `IdGeneratorProvider` Spring bean from a `@PrePersist` method to obtain the next ID value:

```java
@PrePersist
void prePersist() {
    if (this.id == null) {
        this.id = idGeneratorProvider.getIdGenerator().generate();
    }
}
```

### UUIDStringIdGenerator (Default)

The default implementation is `UUIDStringIdGenerator`, registered as the primary Spring bean:

```java
@Primary
@Component
public class UUIDStringIdGenerator implements IdGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
```

To swap the generation strategy (e.g. for a partition-prefixed key on Cloud Spanner), implement `IdGenerator`, annotate it `@Primary`, and the entire application picks it up without touching entity code.

### Idempotent PUT semantics

`GroupFacadeImpl` and `OrganizationFacadeImpl` honour client-provided IDs for new-entity creation (idempotent PUT).  When the entity does not yet exist, the UUID from the URL path is used as the entity `id` directly instead of generating a new one:

```java
if (existing == null) {
    entity = mapper.toEntity(restEntity);
    entity.setId(id);            // use the client-supplied UUID from the path parameter
    service.save(entity);
}
```

This allows a client to create an entity at a deterministic URL and replay the same PUT without creating duplicates.

---

## Adding @ReferenceKey to a New Entity

1. Annotate the natural-key field:
   ```java
   @ReferenceKey
   @Column(unique = true, nullable = false)
   private String path;
   ```

2. No registration step needed — `MetaInfoBuilder.build()` detects the annotation via reflection automatically.

3. Verify that the collection filters in the query DSL now work with the natural key:
   ```
   GET /admin/api/pricerows?q=groupRefs.hasAny:MY-GROUP-PATH
   ```

4. If the entity also has a generated technical `@Id`, add `@GeneratedId`:
   ```java
   @Id
   @GeneratedId
   private String id;
   ```

5. Verify the `$meta` response includes the field in `referenceKeyFields`:
   ```
   GET /admin/api/groups/?$expand=$meta
   ```
   Expected:
   ```json
   {
     "$meta": {
       "identityFields": ["id"],
       "mandatoryFields": ["path", "groupType"],
       "referenceKeyFields": ["path"]
     }
   }
   ```
