# Menu Sections and Custom View Registry Guide

The frontend now uses the `RegistryService` from the `core` library as the single extension point for:

- assigning entity types to menu sections
- introducing additional menu sections
- registering custom pages that replace the generic list, detail, and form views

Register these mappings during application bootstrap, for example from an `APP_INITIALIZER` or another startup hook in the app layer.

## 1. Add a type to a menu section

Use `registerMenuSection()` for a single type or `registerMenuSectionAssignments()` for the JSON-style mapping from the issue.

```ts
registry.registerMenuSection('PriceRow', 'Commerce Management');

registry.registerMenuSectionAssignments({
  PriceRow: 'Commerce Management',
  AppRole: 'System & Access Management'
});
```

If a type has no menu-section assignment, it is automatically rendered in the **Other Types** section.

## 2. Introduce a new menu section

Menu sections are created on demand. Registering a new section name is enough.

```ts
registry.registerRoutePrefix('AuditLog', 'audit-logs');
registry.registerMenuSection('AuditLog', 'Monitoring');
```

Once the type is registered, the sidebar and home page render the new **Monitoring** section automatically.

If the type only uses the generic pages, the generated menu entry points to the generic route automatically:

- `/:lang/generic/audit-logs`

## 3. Register custom pages for a type

Use `registerCustomView()` to replace the generic pages for a type.

```ts
registry.registerCustomView('PriceRow', {
  list: PriceRowsComponent,
  detail: PricerowDetailComponent,
  form: PricerowFormComponent
});
```

The unified generic routes resolve those registrations automatically:

- `/:lang/generic/:entityType`
- `/:lang/generic/:entityType/add`
- `/:lang/generic/:entityType/:id`
- `/:lang/generic/:entityType/:id/edit`

If no custom view is registered, the app falls back to the generic `core` pages.

## 4. Registering a new type end-to-end

For a new type that should appear in navigation and optionally use custom pages:

```ts
registry.registerRoutePrefix('AuditLog', 'audit-logs');
registry.registerMenuSection('AuditLog', 'Monitoring');

registry.registerCustomView('AuditLog', {
  list: AuditLogsComponent,
  detail: AuditLogDetailComponent,
  form: AuditLogFormComponent
});
```

That is the full setup needed for:

- menu placement
- route resolution
- generic fallback replacement

## 5. Suggested application-level setup

An application can keep its existing dedicated top-level routes and still use registry-based generic routes for new types. A minimal bootstrap registration looks like this:

```ts
registry.registerCustomView('PriceRow', {
  list: PriceRowsComponent,
  detail: PricerowDetailComponent,
  form: PricerowFormComponent
});
```
