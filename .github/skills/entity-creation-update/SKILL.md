---
name: entity-creation-update
description: 'Skill for creating or updating a domain entity in the backend service and the frontend app'
---

# Goal
- Create a fully working full-stack domain entity, from persistence to frontend visualization, or update an existing entity with a new field or relation.
- To achieve this goal, several phases need to be completed to implement the changes.

# Phase 1 - Domain Entity Setup - Data Access Layer
- start with the data model (entity) setup / data model changes in the backend service
  - make sure it implements AuditableEntity and fields: OffsetDateTime createdAt, OffsetDateTime lastModifiedAt exist
- if required introduce repository / update repository methods / query 
- if you introduce or extend entities with enum also introduce a (NEW ENTITY)TypeEnumConstraint based on de.ebusyness.commons.dataaccess.dbupdate.AbstractEnumConstraintUpdater

## Relevant Resources during this phase
- see [011-development-guide-data-access-layer.md](../../../service/doc/020-development/011-development-guide-data-access-layer.md)

# Phase 2 - Update Service Layer
- introduce or extend an Entity Service that makes use of an interface
  - e.g. public interface MyEntityService extends EntityService<MyEntity>
  - start with a minimal file first when creating new Entity Service
- introduce validators for the Entity on Service Layer as typically required
  - e.g. MyEntityMandatoryFieldsRule, MyEntitReferenceExistsRule, MyEntityAvoidCyclicDependencyRule, MyEntityLocalizedFieldRule

- generate new or update intialdata (only if explicitly required) and sampledata with Setup Dataloader (make sure its referenced in application.yaml or application-dev.yaml)
- create or update unit tests and integration tests

## Relevant Resources during this phase
- see [012-development-guide-service-layer.md](../../../service/doc/020-development/012-development-guide-service-layer.md)

# Phase 3 - Facade Layer, Controller Layer (REST API)
In the next step you plan implement changes to the REST API by using facade layer, RestEntity and Mappers to convert similar to the existing entities

expected API calls for an entity :
- GET /api/admin/(entity-name-plural)/ - get list of entities
- GET /api/admin/(entity-name-plural)/{id} - get entity by id
- DELETE /api/admin/(entity-name-plural)/{id} - delete Entity by id
- PUT /api/admin/(entity-name-plural)/{id} - create or recreate entity (idempotent)
- PATCH /api/admin/(entity-name-plural)/{id} - patch Entity by id (JSONPatch)
- POST /api/admin/(entity-name-plural)/create - create Entity
- POST /api/admin/(entity-name-plural)/bulk-delete - bulk delete the specified entities (max. 100)
- POST /api/admin/(entity-name-plural)/bulk-create-or-update - bulk create or update the specified entities (max. 100), check if smart matching and update by id is required (no natural business key availble, only technical key)

- implement / use ValidationRule for PatchValidator (patch handling) - e.g. for id ImmutableFieldsRule, LocalizedFieldValidationRule, MandatoryFieldsRule
- implement required service methods, required repository methods and JPQ queries
  - use checked exceptions and pass them trough to web / controller level and handle in ExceptionHandlerAdvice where ever possible
- implement $expand feature for GET calls ($meta, $include, $info, $messages)

- create or update web integration tests (controller based tests) (Happy Path and Angry Path Requests)
- update the postmancollection accordingly (Happy Path and Angry Path Requests with validations)

## Relevant Resources during this phase
- see [013-development-guide-facade-layer.md](../../../service/doc/020-development/013-development-guide-facade-layer.md), [014-development-guide-controller-layer.md](../../../service/doc/020-development/014-development-guide-controller-layer.md)

# Phase 4 - Extend REST API with Query Capabilities
make sure the query capabilities work for a new entity too 
- GET /api/admin/(entity-name-plural)/ - get list of entities

- create or update web integration tests (controller based tests) (Happy Path and Angry Path Requests)
- update the postmancollection accordingly (Happy Path and Angry Path Requests) - and please note if you test them run the spring application with the dev profile (this ensures the sample and test data is loaded accordingly)

## Relevant Resources during this phase
- see [020-query-filtering-implementation.md](../../../service/doc/030-features/020-query-filtering-implementation.md)

# Phase 5 - Frontend / App for new Types Group and Organization
introduce updates of the frontend app to support new Types or new field

pages that are required
- list view with:
  - paging
  - sorting
  - filtering
  - delete action button for selected items (bulk-delete)
  - add button
- Detail view 
- Add New Form / Edit Form, with Save, Cancel and Delete Button
- Routes for every page / view (make sure routes exist)
- Link to the listview on menu sidebar and home page

- make use of existing components if possible
  - if you create new form field components make them generally usable 
- make use $meta as expand parameter information on edit form pages / enum selector component

## Relevant Resources during this phase
- see [development-guide.md](../../../app/docs/development-guide.md)

## Translations
Texts in templates (labels, form field names, button labels, Action names, Statuses, page names,etc..) need to be inserted via transloco translation keys. (introduce new translation keys only if really required)
Make use of the [SKILL.md](../translation/SKILL.md)
### Relevant Resources during this phase
- see [i18n-guide.md](../../../app/docs/i18n-guide.md)

