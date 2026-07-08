---
name: angular-components
description: 'Skill for developing Angular components using modern patterns (signals, standalone components, reactive forms) and reusable UI components in the frontend app'
---

# Goal
Develop Angular components following modern best practices including standalone components, signals for reactive state, OnPush change detection, and proper use of reusable UI components for common patterns like localized fields and reference fields.

# Overview
The Angular frontend uses:
- **Standalone components** (default in Angular 20+)
- **Signals** for reactive state management
- **Zoneless change detection** (requires explicit state updates)
- **OnPush change detection strategy**
- **Reactive forms** with form controls
- **Reusable components** for common UI patterns

# Modern Angular Patterns

## 1. Standalone Components

All components use the standalone API (no `NgModule` needed):

```typescript
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-my-component',
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.scss'],
  // standalone: true is the default in Angular 20+ and can be omitted
  imports: [CommonModule, /* other imports */],
  changeDetection: ChangeDetectionStrategy.OnPush  // Always use OnPush
})
export class MyComponent {
  // Component logic
}
```

## 2. Signals for Reactive State

Use signals instead of traditional properties for reactive state:

```typescript
import { Component, signal, computed } from '@angular/core';

@Component({
  selector: 'app-user-list',
  template: `
    <div>
      <h2>Users ({{ count() }})</h2>
      @for (user of users(); track user.id) {
        <div>{{ user.name }}</div>
      }
      <button (click)="addUser()">Add User</button>
    </div>
  `
})
export class UserListComponent {
  // Writable signal
  users = signal<User[]>([]);

  // Computed signal (derived state)
  count = computed(() => this.users().length);

  addUser() {
    // Update signal with .update()
    this.users.update(users => [...users, { id: Date.now(), name: 'New User' }]);
  }

  setUsers(newUsers: User[]) {
    // Set signal value with .set()
    this.users.set(newUsers);
  }
}
```

**Important for Zoneless Change Detection:**
- Use signals for all reactive state
- Angular does NOT automatically detect changes from async operations (HTTP, timers, etc.)
- Update signals when async data arrives to trigger UI updates

```typescript
data = signal<MyType[]>([]);

loadData() {
  this.myService.getData().subscribe(result => {
    this.data.set(result);  // This triggers UI update
  });
}
```

## 3. Input/Output Functions (Not Decorators)

Use `input()` and `output()` functions instead of decorators:

```typescript
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-user-card',
  template: `
    <div class="card" (click)="cardClick.emit(userId())">
      <h3>{{ userName() }}</h3>
      @if (showDetails()) {
        <p>Details...</p>
      }
    </div>
  `
})
export class UserCardComponent {
  // Required input
  userId = input.required<number>();

  // Optional inputs with defaults
  userName = input<string>('');
  showDetails = input<boolean>(false);

  // Outputs
  cardClick = output<number>();
}
```

Usage in parent:
```html
<app-user-card
  [userId]="user.id"
  [userName]="user.name"
  [showDetails]="true"
  (cardClick)="onUserSelected($event)">
</app-user-card>
```

## 4. Service Injection with inject()

Use `inject()` function instead of constructor injection:

```typescript
import { Component, inject } from '@angular/core';
import { MyService } from '../services/my.service';
import { PermissionService } from '../service/permission.service';

@Component({
  selector: 'app-my-component',
  // ...
})
export class MyComponent {
  private myService = inject(MyService);
  permissionService = inject(PermissionService);  // Public for template access

  loadData() {
    this.myService.getData().subscribe(/* ... */);
  }
}
```

# Reusable Components

## 1. Localized String Field Edit Component

For editing multi-language text fields (names, descriptions, etc.).

**Usage:**
```typescript
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LocalizedStringfieldEditComponent } from './components/localized-stringfield-edit/localized-stringfield-edit.component';
import { SessionService } from '../service/session.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [LocalizedStringfieldEditComponent, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private sessionService = inject(SessionService);

  form!: FormGroup;
  nameLanguages = signal<string[]>([]);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  isEditMode = signal(false);

  // Mandatory languages are determined from the session service, not hardcoded.
  // SessionService loads languages from the backend API (/admin/api/languages?q=active:true),
  // where each language has a `mandatory` flag.
  mandatoryLanguages = computed(() =>
    this.sessionService.availableLanguages().filter(l => l.mandatory).map(l => l.isoKey)
  );

  ngOnInit(): void {
    this.initForm();
    // Show all configured languages (mandatory + active) in the form
    this.nameLanguages.set(this.sessionService.availableLanguages().map(l => l.isoKey));
  }

  initForm(): void {
    const formConfig: any = {};
    // Dynamically add form controls for all languages.
    // Validators.required is set only for languages marked as mandatory in the backend.
    this.sessionService.availableLanguages().forEach(lang => {
      formConfig[`name_${lang.isoKey}`] = ['', lang.mandatory ? Validators.required : []];
    });
    this.form = this.fb.group(formConfig);
  }

  removeLanguage(lang: string): void {
    // Cannot remove mandatory languages
    if (this.mandatoryLanguages().includes(lang)) return;
    this.nameLanguages.update(langs => langs.filter(l => l !== lang));
    // Clear the field value when explicitly removed (keep the control for potential re-add)
    this.form.get(`name_${lang}`)?.setValue('', { emitEvent: true });
  }

  addLanguage(lang: string): void {
    if (!this.nameLanguages().includes(lang)) {
      this.nameLanguages.update(langs => [...langs, lang]);
    }
  }
}
```

**Template:**
```html
<form [formGroup]="form">
  <app-localized-stringfield-edit
    [form]="form"
    [fieldPrefix]="'name_'"
    [fieldLabel]="'Product Name'"
    [configuredLanguages]="nameLanguages"
    [fieldErrors]="fieldErrors"
    [isEditMode]="isEditMode()"
    (removeLanguageEvent)="removeLanguage($event)"
    (addLanguageEvent)="addLanguage($event)">
  </app-localized-stringfield-edit>
</form>
```

## 2. Reference Edit Component

For editing single reference fields (foreign keys) with autocomplete:

**Usage:**
```typescript
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ReferenceEditComponent, ReferenceDataSourceResult } from './components/reference-edit/reference-edit.component';
import { SessionService } from '../service/session.service';

@Component({
  selector: 'app-price-form',
  standalone: true,
  imports: [ReferenceEditComponent, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PriceFormComponent {
  private fb = inject(FormBuilder);
  private unitsService = inject(UnitsService);
  private router = inject(Router);
  private sessionService = inject(SessionService);
  lang = computed(() => this.sessionService.language());

  form: FormGroup = this.fb.group({
    unitRef: ['']
  });

  // Paginated data source with backend filtering
  unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    return this.unitsService.getUnits(page, 20, searchTerm).pipe(
      map(response => ({
        options: response.items.map(unit => ({
          value: unit.symbol,
          label: `${unit.symbol} - ${unit.name['en'] || ''}`
        })),
        hasMore: response.$info.paging['total-pages'] > page + 1
      }))
    );
  };

  hasFieldError(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    return !!(control && control.invalid && control.touched);
  }

  onCreateNewUnit(searchTerm: string): void {
    // Navigate to unit creation form with pre-filled search term
    this.router.navigate(['/' + this.lang(), 'units', 'add'], { queryParams: { symbol: searchTerm } });
  }
}
```

**Template:**
```html
<form [formGroup]="form">
  <div class="mb-3">
    <label class="form-label">{{ 'common.fields.unitKey' | transloco }}</label>
    <app-reference-edit
      [inputFormControl]="form.get('unitRef')"
      placeholder="e.g., kg"
      [isInvalid]="hasFieldError('unitRef')"
      [dataSource]="unitsDataSource"
      [allowCreate]="true"
      (createNew)="onCreateNewUnit($event)">
    </app-reference-edit>
    @if (hasFieldError('unitRef')) {
      <div class="invalid-feedback d-block">{{ 'common.messages.requiredField' | transloco }}</div>
    }
  </div>
</form>
```

## 3. Localized String Field View Component

For displaying multi-language fields in read-only/detail views:

```html
<app-localized-stringfield-view
  [values]="entity.name"
  [fieldLabel]="'Name'">
</app-localized-stringfield-view>
```

## 4. Reference Badge CSS Class

For displaying reference values as styled badges:

```html
<!-- As link (include lang() prefix in route) -->
<a [routerLink]="['/' + lang(), 'currencies', key]" class="ref-badge">{{ key }}</a>

<!-- As static text -->
<span class="ref-badge">{{ value }}</span>
```

# Entity Page Pattern

For each entity, implement these pages following existing examples (Currency, Unit, Language):

## 1. List Page (`<entity>s.component`)
- Table with sorting and pagination
- Bulk delete functionality
- Search/filter capabilities
- Links to detail pages using `.ref-badge`

```typescript
@Component({
  selector: 'app-units',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslocoModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UnitsComponent implements OnInit {
  units = signal<Unit[]>([]);
  selectedUnits = signal<Set<string>>(new Set());

  permissionService = inject(PermissionService);
  sessionService = inject(SessionService);
  private unitsService = inject(UnitsService);
  private transloco = inject(TranslocoService);

  lang = computed(() => this.sessionService.language());

  ngOnInit(): void {
    this.loadUnits();
  }

  loadUnits(): void {
    this.unitsService.getUnits().subscribe(response => {
      this.units.set(response.items);
    });
  }

  deleteSelected(): void {
    if (!confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedUnits().size }))) return;

    this.unitsService.bulkDelete(Array.from(this.selectedUnits())).subscribe(() => {
      this.loadUnits();
      this.selectedUnits.set(new Set());
    });
  }
}
```

## 2. Detail Page (`detail/<entity>-detail.component`)
- Card layout with field display
- Uses `localized-stringfield-view` for translations
- Edit and Delete buttons (permission-based)

```html
<div class="card">
  <div class="card-header">
    <h2>{{ 'pages.units.details' | transloco }}</h2>
  </div>
  <div class="card-body">
    <div class="row mb-3">
      <label class="col-sm-3 col-form-label">{{ 'common.fields.symbol' | transloco }}:</label>
      <div class="col-sm-9">
        <span class="ref-badge">{{ unit().symbol }}</span>
      </div>
    </div>

    <app-localized-stringfield-view
      [values]="unit().name"
      [fieldLabel]="'Name'">
    </app-localized-stringfield-view>

    @if (permissionService.hasWritePermission('Unit')) {
      <button (click)="onEdit()" class="btn btn-primary">{{ 'common.actions.edit' | transloco }}</button>
    }
    @if (permissionService.hasDeletePermission('Unit')) {
      <button (click)="onDelete()" class="btn btn-danger">{{ 'common.actions.delete' | transloco }}</button>
    }
  </div>
</div>
```

## 3. Form Page (`form/<entity>-form.component`)
- Handles both add and edit modes
- Uses reactive forms
- Uses `localized-stringfield-edit` and `reference-edit` components
- Validation error display
- Save, Cancel, Delete buttons

```typescript
@Component({
  selector: 'app-unit-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    LocalizedStringfieldEditComponent,
    ReferenceEditComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UnitFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = signal(false);
  // nameLanguages tracks which languages are shown in the form UI
  nameLanguages = signal<string[]>([]);

  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionService = inject(SessionService);
  private unitsService = inject(UnitsService);

  // Mandatory languages loaded from backend via SessionService
  mandatoryLanguages = computed(() =>
    this.sessionService.availableLanguages().filter(l => l.mandatory).map(l => l.isoKey)
  );

  ngOnInit(): void {
    const symbol = this.route.snapshot.paramMap.get('symbol');
    this.isEditMode.set(!!symbol);

    this.initForm();

    if (!this.isEditMode()) {
      // Add mode: show mandatory + active languages
      this.nameLanguages.set(this.sessionService.availableLanguages().map(l => l.isoKey));
    } else {
      this.loadUnit(symbol!);
    }
  }

  initForm(): void {
    const formConfig: any = {
      symbol: [{ value: '', disabled: this.isEditMode() }, Validators.required],
      measure: ['']
    };

    // Dynamically add form controls for all languages.
    // Validators.required is determined by the language's `mandatory` flag from the backend.
    this.sessionService.availableLanguages().forEach(lang => {
      formConfig[`name_${lang.isoKey}`] = ['', lang.mandatory ? Validators.required : []];
    });

    this.form = this.fb.group(formConfig);
  }

  removeLanguage(lang: string): void {
    if (this.mandatoryLanguages().includes(lang)) return;
    this.nameLanguages.update(langs => langs.filter(l => l !== lang));
    // Clear the field value when explicitly removed (keep the control for potential re-add)
    this.form.get(`name_${lang}`)?.setValue('', { emitEvent: true });
  }

  addLanguage(lang: string): void {
    if (!this.nameLanguages().includes(lang)) {
      this.nameLanguages.update(langs => [...langs, lang]);
    }
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const data = this.buildRestEntity();

    if (this.isEditMode()) {
      this.unitsService.update(data.symbol, data).subscribe(() => {
        this.router.navigate(['/' + this.sessionService.language(), 'units', data.symbol]);
      });
    } else {
      this.unitsService.create(data).subscribe(() => {
        this.router.navigate(['/' + this.sessionService.language(), 'units']);
      });
    }
  }

  buildRestEntity(): UnitRestEntity {
    const formValue = this.form.getRawValue();

    // Build localized name object from configured languages
    const name: { [key: string]: string } = {};
    this.nameLanguages().forEach(lang => {
      const value = formValue[`name_${lang}`];
      if (value) {
        name[lang] = value;
      }
    });

    return {
      symbol: formValue.symbol,
      name: name,
      measure: formValue.measure
    };
  }
}
```

## 4. Routes Configuration

Routes are nested under `:lang` prefix. **app.routes.ts:**
```typescript
{
  path: ':lang',
  children: [
    { path: 'units', component: UnitsComponent },
    { path: 'units/add', component: UnitFormComponent },
    { path: 'units/:symbol', component: UnitDetailComponent },
    { path: 'units/:symbol/edit', component: UnitFormComponent }
  ]
}
```

## 5. Navigation Links

**sidebar.component.html:**
```html
@if (permissionService.hasReadPermission('Unit')) {
  <a [routerLink]="['/' + lang(), 'units']" class="list-group-item list-group-item-action py-2 ripple" routerLinkActive="active">
    <i class="bi bi-rulers me-3"></i><span>{{ 'components.sidebar.units' | transloco }}</span>
  </a>
}
```

**home.component.html:**
```html
@if (permissionService.hasReadPermission('Unit')) {
  <div class="col-md-4">
    <div class="card">
      <div class="card-body">
        <h5 class="card-title">{{ 'pages.units.title' | transloco }}</h5>
        <p class="card-text">{{ 'pages.units.description' | transloco }}</p>
        <a [routerLink]="['/' + lang(), 'units']" class="btn btn-primary">{{ 'pages.units.viewAll' | transloco }}</a>
      </div>
    </div>
  </div>
}
```

# Form Handling Best Practices

## 1. Track Original Values for PATCH

When editing, track original values to build JSON Patch operations:

```typescript
originalData = signal<UnitRestEntity | null>(null);

loadUnit(id: string) {
  this.unitsService.getById(id).subscribe(unit => {
    this.originalData.set(unit);
    this.patchFormValues(unit);
  });
}

patchFormValues(unit: UnitRestEntity) {
  this.form.patchValue({
    symbol: unit.symbol,
    measure: unit.measure
  });

  // Load language-specific fields
  Object.entries(unit.name).forEach(([lang, value]) => {
    const controlName = `name_${lang}`;
    if (this.form.contains(controlName)) {
      this.form.get(controlName)?.setValue(value);
    } else {
      this.form.addControl(controlName, this.fb.control(value));
      this.nameLanguages.update(langs => [...langs, lang]);
    }
  });
}
```

## 2. Validation Error Handling

Display backend validation errors:

```typescript
fieldErrors = signal<Map<string, string[]>>(new Map());

onSave(): void {
  this.unitsService.update(data).subscribe({
    next: () => this.router.navigate(['/' + this.lang(), 'units']),
    error: (error) => {
      if (error.status === 400 && error.error.$messages) {
        this.handleValidationErrors(error.error.$messages);
      }
    }
  });
}

handleValidationErrors(messages: Message[]) {
  const errorMap = new Map<string, string[]>();

  messages.forEach(msg => {
    if (msg.fields && msg.fields.length > 0) {
      msg.fields.forEach(field => {
        const errors = errorMap.get(field) || [];
        errors.push(msg.message);
        errorMap.set(field, errors);
      });
    }
  });

  this.fieldErrors.set(errorMap);
}
```

# Template Syntax

## Modern Control Flow

Use `@if`, `@for`, `@switch` instead of `*ngIf`, `*ngFor`, `*ngSwitch`:

```html
<!-- Conditional rendering -->
@if (users().length > 0) {
  <ul>
    @for (user of users(); track user.id) {
      <li>{{ user.name }}</li>
    }
  </ul>
} @else {
  <p>No users found</p>
}

<!-- Switch statement -->
@switch (status()) {
  @case ('loading') {
    <div>Loading...</div>
  }
  @case ('error') {
    <div>Error occurred</div>
  }
  @default {
    <div>Ready</div>
  }
}
```

# Best Practices

## Component Development
1. **Always use ChangeDetectionStrategy.OnPush** for better performance
2. **Use signals for reactive state** instead of traditional properties
3. **`standalone: true` is the default in Angular 20+** and can be omitted; existing components in this codebase include it explicitly
4. **Import pipes and directives** explicitly in standalone components
5. **Use `inject()` for services** instead of constructor injection
6. **Use `input()` and `output()`** instead of `@Input()` and `@Output()` for new components (note: existing reusable components like `LocalizedStringfieldEditComponent` and `ReferenceEditComponent` still use `@Input()`/`@Output()` decorators)

## State Management
1. **Use signals** for all reactive state
2. **Use `computed()`** for derived state
3. **Update signals** when async data arrives (HTTP responses)
4. **Avoid `mutate`** - use `update()` or `set()` instead
5. **Track collections** by unique ID in `@for` loops

## Forms
1. **Use reactive forms** (not template-driven forms)
2. **Track original values** for PATCH operations
3. **Mark fields as touched** on save to show validation errors
4. **Handle backend validation errors** with field-level error display
5. **Use reusable components** for common patterns (localized fields, references)

## Templates
1. **Use native control flow** (`@if`, `@for`, `@switch`)
2. **Avoid complex logic** in templates
3. **Use `async` pipe** for observables (when not using signals)
4. **Track items** in `@for` loops for performance
5. **Check permissions** before showing UI elements

# Relevant Resources

- [Angular Development Guide](../../../app/doc/020-development/010-development-guide.md) - Comprehensive development patterns and best practices
- [Localized String Field Edit Component](../../../app/doc/040-components/localized-stringfield-edit.md) - Detailed component documentation
- [Reference Edit Component](../../../app/doc/040-components/reference-edit.md) - Autocomplete reference field component
- [Security Implementation Guide](../../../app/doc/020-development/020-security-implementation-guide.md) - OIDC and permissions in Angular
- [i18n Guide](../../../app/doc/020-development/030-i18n-guide.md) - Internationalization and translation management
