# Menu Sections and Custom View Registry Guide

The application adapts its navigation **declaratively** through the app-local JSON file:

- `/src/assets/config/menu-registry.json`

That file is loaded during application startup and its content is registered into the shared `RegistryService`.

Use this JSON file for:

- assigning entity types to menu sections
- introducing additional menu sections
- defining entity menu entries, including entities coming from shared libraries such as `corebusiness`
- defining standalone non-entity menu items

Use code-based registry registration only for:

- registering custom list/detail/form components that should replace the generic pages

## 1. Declarative menu configuration in the application

The menu configuration is owned by the Angular application, not by the shared library. To adapt the menu for an application, edit:

- `/src/assets/config/menu-registry.json`

Structure:

```json
{
  "entities": [
    {
      "type": "PriceRow",
      "routePrefix": "pricerows",
      "menuSection": "Commerce Management",
      "icon": "bi bi-card-list"
    }
  ],
  "menuItems": [
    {
      "key": "service-initialization",
      "path": "service-initialization",
      "section": "System & Access Management",
      "label": "Service Initialization",
      "icon": "bi bi-gear",
      "permission": "priceprovider.admin:ServiceInitialization:write",
      "permissionMode": "permission"
    }
  ]
}
```

All entity menu entries that should be visible in the application should be declared here, including shared entities such as `Unit`, `Currency`, `TaxClass`, `Group`, `Organization`, and `Language`.

## 2. Add or move a type to a menu section

To place a type in a menu section, set `menuSection` on its `entities` entry:

```json
{
  "type": "PriceRow",
  "routePrefix": "pricerows",
  "menuSection": "Commerce Management",
  "icon": "bi bi-card-list"
}
```

Changing `menuSection` moves the item in both the sidebar and the home page.

If a type has no `menuSection`, it falls back to **Other Types**.

## 3. Introduce a new menu section

Menu sections are created on demand. Use a new `menuSection` name in the JSON:

```json
{
  "type": "AuditLog",
  "routePrefix": "audit-logs",
  "menuSection": "Monitoring",
  "icon": "bi bi-activity"
}
```

Once the application loads this configuration, the new **Monitoring** section is rendered automatically.

## 4. Add standalone menu items declaratively

Use `menuItems` for entries that are not entity types:

```json
{
  "key": "service-initialization",
  "path": "service-initialization",
  "section": "System & Access Management",
  "label": "Service Initialization",
  "icon": "bi bi-gear",
  "permission": "priceprovider.admin:ServiceInitialization:write",
  "permissionMode": "permission"
}
```

Use this for application-specific pages that should appear together with the entity navigation.

## 5. Route behavior for declarative entity entries

For entity entries, the JSON configuration drives generic navigation:

- `routePrefix` defines the generated route segment
- the generated entity link points to `/:lang/generic/<routePrefix>`

Example:

```json
{
  "type": "AuditLog",
  "routePrefix": "audit-logs",
  "menuSection": "Monitoring"
}
```

This produces navigation to:

- `/:lang/generic/audit-logs`

## 6. Custom view overrides stay code-based

If a type should use custom Angular pages instead of the generic `core` pages, register those views in code:

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

## 7. Typical application workflow

For most application-level menu changes:

1. Edit `/src/assets/config/menu-registry.json`
2. Add or adjust the `entities` / `menuItems` declarations
3. Start the app and verify the sidebar and home page
4. Only add code registration when a type needs custom list/detail/form components
