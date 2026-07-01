# Angular Frontend

The PriceProvider Angular frontend is structured around pages, each composed of modular, reusable components.

## AI Agent Skills

The following skills are particularly relevant when working on the frontend:
- [**Entity Creation & Update (Frontend Phase)**](../.github/skills/entity-creation-update/SKILL.md#phase-5---frontend--app-for-new-types-group-and-organization): Phase 5 provides guidance on implementing list views, detail views, and forms.
- [**Angular Components**](../.github/skills/angular-components/SKILL.md): Developing modern Angular components using signals, standalone components, and reusable UI patterns.
- [**Security & RBAC (Frontend)**](../.github/skills/security-rbac/SKILL.md#frontend-implementation): Implementing permission-based UI access control and OIDC authentication.
- [**Translation**](../.github/skills/translation/SKILL.md): Managing i18n translation keys in the Angular application.

## Documentation

For detailed development guides, component documentation, and implementation examples, see:
- [Development Guide](docs/development-guide.md) - Comprehensive development patterns and best practices
- [Component Documentation](docs/components/) - Detailed documentation for reusable components

## Technology Stack

- Angular CLI: 22.0.5
- Node.js: 22.22.3+
- TypeScript: 6.0.3
- RxJS
- Standalone Components + Signals API
- Bootstrap (incl. Bootstrap Icons)
- SCSS for styling

## Project Structure

```
app/                                   # Angular frontend
├── public/                            # static assets (images, ...)
├── src/
│   ├── app/
│   │   ├── components/<entity-name>/  # components folder with entity related modules (lazy-loaded components)
│   │   ├── pages/<entity-name>/       # page components folder with page / entity related folders
│   │   ├── service/<entity-name>/     # Shared components, directives, pipes
│   │   ├── shared/                    # Shared components, directives, pipes, interceptors
│   │   ├── model/                     # models, guards
│   │   ├── environments/              # environment config
│   │   │   └── environment.ts
│   │   └── app.config.ts              # Application-wide configuration
|   └── index.html
├── angular.json
├── package.json
├── Dockerfile
└── dockerimage-create.sh / dockerimage-create.bat
```

## Angular / TypeScript Best Practices

- Enable strict type checking
- Prefer type inference when obvious
- Avoid `any`; use `unknown` if necessary
- Avoid `@HostBinding` and `@HostListener`; use `host` metadata
- Use signals and `computed()` for state and derived values
- Lazy-load feature routes
- Use `NgOptimizedImage` for static images (not for base64)

### Zoneless Change Detection and Signals

Angular 16+ introduced zoneless change detection, which removes the need for Zone.js with a bunch of benefits:
- Improved performance (no Zone.js overhead)
- More predictable, explicit change detection
- Smaller bundle size

**Important:** In the price manager app zoneless change detection is activated, Angular does NOT automatically detect changes from async operations (e.g., HTTP requests, timers). You must use signals (prefferd) for reactive state, or manually trigger change detection.

### Example: Using Signals for Reactive State

```ts
import { Component, signal } from '@angular/core';

@Component({
	selector: 'app-demo',
	template: `
		<div>
			<button (click)="increment()">Increment</button>
			<span>Value: {{ value() }}</span>
		</div>
	`
})
export class DemoComponent {
	value = signal(0);

	increment() {
		this.value.update(v => v + 1);
	}
}
```

**Usage:**
- Use `signal()` to declare reactive state.
- Access the value with `value()` in templates and code.
- Update state with `.set()` or `.update()`.

**For async data (e.g., HTTP):**

```ts
data = signal<MyType[]>([]);

loadData() {
	this.myService.getData().subscribe(result => {
		this.data.set(result);
	});
}
```

This ensures the UI updates automatically when the signal changes, even with zoneless change detection enabled.


## Component Guidelines
- Use standalone components (default in Angular 20+)
- Must NOT set `standalone: true` inside Angular decorators. It's the default.
- Small, focused components
- Use `input()` / `output()` functions (not decorators)
- Use `ChangeDetectionStrategy.OnPush`
- Prefer inline templates for small components
- Use reactive forms
- Avoid `ngClass` and `ngStyle`; use `[class]` and `[style]`

## State Management

- Use signals for local state
- Use `computed()` for derived state
- Avoid `mutate`; use `update()` or `set()`

## Template Guidelines

- Use native control flow (`@if`, `@for`, `@switch`)
- Avoid complex logic in templates
- Use `async` pipe for observables

## Services

- Single-responsibility services
- Use `providedIn: 'root'`
- Use `inject()` instead of constructor injection

### REST API Communication

**Update Operations - Use PATCH**:
When updating existing entities, use PATCH requests with JSON Patch operations (RFC 6902).

**Create Operations - Use POST**:
When creating new entities, use POST to `/create` endpoint (ID generated server-side).

**Form Change Tracking**:
Track original values and build JSON Patch operations from form changes.

For detailed implementation examples including change tracking and PATCH operation building, see [Development Guide - REST API Communication](docs/development-guide.md#rest-api-communication).

## Pages and Components with Routes and URL Parameters

- Structure the frontend using page-level components that represent domain entities, such as list views and detail views.
- Use Angular routing to connect pages with meaningful URLs, including dynamic segments and query parameters.
- Reflect component and page state (e.g., selected filters, pagination, sorting) via URL parameters to support deep linking, bookmarking, and consistent navigation.

## Reusable Components

The frontend provides several reusable components for common UI patterns. These components follow Angular best practices and are designed for consistency across the application.

### Core Components

#### `reference-edit` Component
Autocomplete-enabled input for editing reference fields (foreign keys, entity references).

**Features:**
- Async data loading with filtering
- Clear button for removing values
- Optional "Create new..." functionality
- Loading indicators

**Quick Example:**
```html
<app-reference-edit
  [control]="form.get('unitRef')"
  [dataSource]="unitsDataSource"
  placeholder="e.g., kg"
  [isInvalid]="hasFieldError('unitRef')"
  [allowCreate]="true"
  (createNew)="onCreateNewUnit($event)">
</app-reference-edit>
```

**[View Full Documentation](docs/components/reference-edit.md)**

#### `localized-stringfield-edit` Component
Comprehensive solution for editing multi-language text fields in forms.

**Features:**
- Automatic management of mandatory/optional languages
- Dynamic addition/removal of language fields
- Inactive language handling (read-only)
- Toggle between showing all values or just mandatory ones
- Field-level validation error display

**Quick Example:**
```html
<app-localized-stringfield-edit
  [form]="form"
  [fieldPrefix]="'name_'"
  [fieldLabel]="'Name'"
  [configuredLanguages]="nameLanguages"
  [fieldErrors]="fieldErrors"
  [isEditMode]="isEditMode()"
  (removeLanguageEvent)="removeLanguage($event)"
  (addLanguageEvent)="addLanguage($event)">
</app-localized-stringfield-edit>
```

**[View Full Documentation](docs/components/localized-stringfield-edit.md)**

#### `localized-stringfield-view` Component
Display component for multi-language fields in read-only/detail views.

**Quick Example:**
```html
<app-localized-stringfield-view 
  [values]="entity.name" 
  [fieldLabel]="'Name'">
</app-localized-stringfield-view>
```

### Display Components

#### `.ref-badge` CSS Class
For displaying reference values (IDs, keys, symbols) as styled badges.

**Examples:**
```html
<!-- Link -->
<a [routerLink]="['/currencies', key]" class="ref-badge">{{ key }}</a>
<!-- Static -->
<span class="ref-badge">{{ value }}</span>
```

**Note**: Do not use `localized-name-input` (deprecated/unused). Use `localized-stringfield-edit` instead.

## Entity Page Pattern

For each entity, implement these pages following existing examples (Currency, Unit, Language):

1. **List page** (`<entity>s.component`) - Table with paging, sorting, bulk delete, `.ref-badge` links to detail
2. **Detail page** (`detail/<entity>-detail.component`) - Card with fields, uses `localized-stringfield-view` for translations
3. **Form page** (`form/<entity>-form.component`) - Handles add/edit, uses `localized-stringfield-edit` and `reference-edit` components
4. **Routes** in `app.routes.ts`:
   ```typescript
   { path: 'entities', component: EntitiesComponent },
   { path: 'entities/add', component: EntityFormComponent },
   { path: 'entities/:id', component: EntityDetailComponent },
   { path: 'entities/:id/edit', component: EntityFormComponent }
   ```
5. **Navigation** - Add links to `sidebar.component.html` and `home.html`

## Error Handling

### Validation Errors with Field References

When displaying validation errors from the backend:

1. **Error messages with field references** - Apply `.is-invalid` CSS class to form control and display error message below field using `.invalid-feedback` class
2. **General error messages** - Display at the top of the form/page using Bootstrap alert components
3. **Error message format from backend**:
   ```typescript
   {
     "$messages": [
       {
         "type": "ERROR",
         "message": "Error description",
         "fields": ["fieldName1", "fieldName2"]  // Optional: affected fields
       }
     ]
   }
   ```

**Important**: Error messages from backend should NOT contain HTTP status codes, URLs, or technical details.

For detailed error handling patterns and complete examples, see [Development Guide - Error Handling](docs/development-guide.md#error-handling).
