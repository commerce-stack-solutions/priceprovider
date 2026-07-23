# Dynamic Generic UI Components

To facilitate seamless extensibility and eliminate the overhead of manually creating form templates, list tables, and detail screens for every business entity, the Price Manager includes a dynamic **Generic UI Engine**.

## Architecture & Standalone Components

This system is comprised of three core components located under `app/src/app/pages/`:

### 1. `GenericListComponent`
Generates tabular lists dynamically based on the `$meta` API.
- **Ordered Columns**: Automatically sorts and lists columns starting with the entity identifiers (`identityFields`), followed by reference key fields (`referenceKeyFields`), and then other primitive and relation fields (up to a maximum of 8 columns).
- **Interactive Search Filters**: Exposes search filter drop-downs on column headers for all `String`, `Number`, `Enum`, and `Reference` type fields.
- **Reference & Localized Cells**: Displays single relationship references as clean badges, and automatically falls back to preferred active languages when rendering complex `LocalizedString` properties.
- **Bulk Operations**: Provides checkboxes for multi-select and bulk deletion capabilities.

### 2. `GenericDetailComponent`
Displays all properties of a specific record in structured sections.
- **Categorization**: Groups standard attributes, multi-language translatable fields (using tab selectors), single/collection relationships, and system audit information (using `<app-info-section>`) into dedicated layouts.
- **Read-Only Inspection**: Ideal for reviewing complete entity properties before deciding to edit or delete.

### 3. `GenericFormComponent`
Renders dynamic interactive forms mapped to fields' metadata.
- **Type-Aware Input Fields**: Automatically renders standard inputs for `String`, numeric inputs with step precision adjustments for `Number`, checkboxes for `Boolean`, date-time pickers for `DateTime`, enum dropdown lists for `Enum`, and dynamic localized language tabs for `LocalizedString`.
- **Relational Auto-Complete**: Inspects `Reference` and `Set<Reference>` fields, resolves their `referencedEntity` type (e.g. `Group` for `parentRefs`), and maps them to standard administrative search lookups (such as `/admin/api/groups`) with type-ahead selectors.

---

## Generic Routing

The routes are registered in `app.routes.ts` under a localized dynamic routing hierarchy prefix `/generic/:entityType`:

```typescript
{
  path: 'generic/:entityType',
  children: [
    { path: '', loadComponent: () => import('./pages/generic-list/generic-list.component').then(m => m.GenericListComponent) },
    { path: 'add', loadComponent: () => import('./pages/generic-form/generic-form.component').then(m => m.GenericFormComponent) },
    { path: ':id', loadComponent: () => import('./pages/generic-detail/generic-detail.component').then(m => m.GenericDetailComponent) },
    { path: ':id/edit', loadComponent: () => import('./pages/generic-form/generic-form.component').then(m => m.GenericFormComponent) }
  ]
}
```

## Accessing Generic Interfaces

Partners or developers can easily expose new endpoints without modifying application source code. Simply navigate to the generic views corresponding to the plural entity URL segment:
- `/en/generic/currencies`
- `/en/generic/channels`
- `/en/generic/groups`
- `/en/generic/taxclasses`
