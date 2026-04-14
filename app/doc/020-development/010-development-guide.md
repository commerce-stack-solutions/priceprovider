# Angular Frontend Development Guide

This guide provides detailed implementation patterns, best practices, and examples for developing the Price Manager Angular frontend.

## Table of Contents

1. [Component Development Patterns](#component-development-patterns)
2. [Signals and Reactive State](#signals-and-reactive-state)
3. [Form Handling](#form-handling)
4. [REST API Communication](#rest-api-communication)
5. [`$meta` — Entity Metadata](#meta--entity-metadata)
6. [Error Handling](#error-handling)
7. [Routing and Navigation](#routing-and-navigation)

---

## Component Development Patterns

### Standalone Components

All components use the standalone API (default in Angular 20+):

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-my-component',
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.scss'],
  // NOTE: Do NOT set standalone: true - it's the default
  imports: [CommonModule, /* other imports */],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyComponent {
  // Component logic
}
```

### Using Standalone Pipes and Directives

Because the application uses standalone components, any custom pipes or directives used in a component's template must be explicitly added to that component's `imports` array.

For example, to use the `IsMandatoryPipe` (which powers the `isMandatory` template checks), you must import it directly into the component:

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe'; // Adjust path as needed

@Component({
  selector: 'app-my-form',
  templateUrl: './my-form.component.html',
  // standalone pipes/directives MUST be declared here to use them in the template
  imports: [CommonModule, IsMandatoryPipe]
})
export class MyFormComponent {
  // ...
}
```

### Component Input/Output

Use the `input()` and `output()` functions instead of decorators:

```typescript
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-user-card',
  template: `
    <div class="card" (click)="cardClick.emit(userId())">
      <h3>{{ userName() }}</h3>
    </div>
  `
})
export class UserCardComponent {
  // Inputs
  userId = input.required<number>();
  userName = input<string>('');
  
  // Outputs
  cardClick = output<number>();
}
```

Usage in parent template:

```html
<app-user-card
  [userId]="user.id"
  [userName]="user.name"
  (cardClick)="onUserSelected($event)">
</app-user-card>
```

---

## Signals and Reactive State

### Why Signals?

The app uses **zoneless change detection**, which requires explicit state management. Signals provide reactive state that automatically triggers UI updates.

### Basic Signal Usage

```typescript
import { Component, signal, computed } from '@angular/core';

@Component({
  selector: 'app-counter',
  template: `
    <div>
      <p>Count: {{ count() }}</p>
      <p>Double: {{ doubled() }}</p>
      <button (click)="increment()">Increment</button>
    </div>
  `
})
export class CounterComponent {
  // Signal for mutable state
  count = signal(0);
  
  // Computed signal for derived state
  doubled = computed(() => this.count() * 2);
  
  increment(): void {
    // Update signal value
    this.count.update(current => current + 1);
  }
}
```

### Signal Update Methods

```typescript
// Set to specific value
count.set(10);

// Update based on current value
count.update(current => current + 1);

// For complex objects, create new reference
user.update(current => ({ ...current, name: 'New Name' }));
```

### Signals with Async Data

```typescript
import { Component, signal } from '@angular/core';
import { ProductService } from './services/product.service';

@Component({
  selector: 'app-product-list',
  template: `
    @if (loading()) {
      <div>Loading...</div>
    }
    @for (product of products(); track product.id) {
      <div>{{ product.name }}</div>
    }
  `
})
export class ProductListComponent {
  products = signal<Product[]>([]);
  loading = signal(false);
  
  constructor(private productService: ProductService) {
    this.loadProducts();
  }
  
  loadProducts(): void {
    this.loading.set(true);
    this.productService.getProducts().subscribe({
      next: (data) => {
        this.products.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.loading.set(false);
      }
    });
  }
}
```

---

## Form Handling

### Reactive Forms Setup

```typescript
import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-product-form',
  templateUrl: './product-form.component.html',
  imports: [ReactiveFormsModule]
})
export class ProductFormComponent {
  form: FormGroup;
  fieldErrors = signal<Map<string, string[]>>(new Map());
  
  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      unitRef: ['', Validators.required]
    });
  }
  
  onSubmit(): void {
    if (this.form.valid) {
      // Submit form
    }
  }
}
```

### Tracking Original Values for PATCH

```typescript
import { Component, signal } from '@angular/core';

interface JsonPatchOperation {
  op: 'replace' | 'add' | 'remove';
  path: string;
  value?: any;
}

@Component({
  selector: 'app-edit-form',
  templateUrl: './edit-form.component.html'
})
export class EditFormComponent {
  form: FormGroup;
  originalValues = signal<any>({});
  entityId = signal<number | null>(null);
  
  loadEntity(id: number): void {
    this.service.getEntity(id).subscribe(entity => {
      // Save original values
      this.originalValues.set({ ...entity });
      this.entityId.set(id);
      
      // Populate form
      this.form.patchValue(entity);
    });
  }
  
  buildPatchOperations(): JsonPatchOperation[] {
    const patches: JsonPatchOperation[] = [];
    const formValue = this.form.value;
    const original = this.originalValues();
    
    Object.keys(formValue).forEach(key => {
      if (formValue[key] !== original[key]) {
        patches.push({
          op: 'replace',
          path: `/${key}`,
          value: formValue[key]
        });
      }
    });
    
    return patches;
  }
  
  onSubmit(): void {
    const patches = this.buildPatchOperations();
    const id = this.entityId();
    
    if (id && patches.length > 0) {
      this.service.patchEntity(id, patches).subscribe({
        next: (result) => {
          // Handle success
        },
        error: (err) => {
          // Handle error
        }
      });
    }
  }
}
```

### Form Validation Errors

```typescript
hasFieldError(fieldName: string): boolean {
  const control = this.form.get(fieldName);
  return !!(control && control.invalid && control.touched);
}

getFieldError(fieldName: string): string {
  const control = this.form.get(fieldName);
  if (control?.hasError('required')) {
    return 'This field is required';
  }
  if (control?.hasError('min')) {
    return `Value must be at least ${control.errors?.['min'].min}`;
  }
  return '';
}
```

Template usage:

```html
<input 
  type="text" 
  formControlName="name"
  [class.is-invalid]="hasFieldError('name')">
@if (hasFieldError('name')) {
  <div class="invalid-feedback">
    {{ getFieldError('name') }}
  </div>
}
```

---

## REST API Communication

### Service Structure

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Product {
  id: number;
  name: string;
  price: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private http = inject(HttpClient);
  private baseUrl = '/api/products';
  
  getProducts(page: number = 0, pageSize: number = 10): Observable<Product[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('page-size', pageSize.toString());
    
    return this.http.get<Product[]>(this.baseUrl, { params });
  }
  
  getProduct(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }
  
  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/create`, product);
  }
  
  patchProduct(id: number, patches: JsonPatchOperation[]): Observable<Product> {
    return this.http.patch<Product>(
      `${this.baseUrl}/${id}`,
      patches,
      { headers: { 'Content-Type': 'application/json-patch+json' } }
    );
  }
  
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

### Using Services in Components

```typescript
@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html'
})
export class ProductListComponent {
  products = signal<Product[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  
  private productService = inject(ProductService);
  
  ngOnInit(): void {
    this.loadProducts();
  }
  
  loadProducts(): void {
    this.loading.set(true);
    this.error.set(null);
    
    this.productService.getProducts().subscribe({
      next: (data) => {
        this.products.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load products');
        this.loading.set(false);
        console.error('Error loading products:', err);
      }
    });
  }
}
```

---

## `$meta` — Entity Metadata

The `$meta` expand returns structural metadata about an entity from the REST API. Form components consume it to drive mandatory-field markers and enum selector options without any hardcoding.

For backend implementation details (annotations, registry, service-layer validation) see [service/doc/030-features/060-meta-annotation-concept.md](../../../service/doc/030-features/060-meta-annotation-concept.md).

### API Response Structure

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

| Field            | Description |
|------------------|-------------|
| `identityFields` | Primary key fields (from `@Id` on the JPA entity) |
| `mandatoryFields`| Fields the caller must supply (from `@Id` without `@GeneratedValue`, and `@MandatoryField`) |
| `enumValues`     | All valid values for every enum-typed field (mandatory **and** optional) |

### `MetaInfo` Interface

The shared TypeScript interface lives in `app/src/app/model/meta-info.model.ts`:

```typescript
export interface MetaInfo {
  identityFields?: string[];
  mandatoryFields?: string[];
  enumValues?: { [key: string]: string[] };
}
```

Import it directly in any model or component that needs it:

```typescript
import { MetaInfo } from '../../../model/meta-info.model';
```

### Requesting `$meta` in Services

Each entity service exposes a dedicated `getMeta()` method that calls `GET /admin/api/{entities}/$meta`:

```typescript
// groups.service.ts
getMeta(): Observable<MetaInfo> {
  return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/groups/$meta`);
}
```

Single-entity GET requests automatically include `$meta` via the `$expand` parameter:

```typescript
// groups.service.ts
getGroup(id: string, expand?: string): Observable<Group> {
  let params = new HttpParams();
  if (expand) {
    params = params.set('$expand', expand);
  } else {
    params = params.set('$expand', '$includes,$info,$meta');  // default: always include $meta
  }
  return this.http.get<Group>(`${this.apiUrl}/${id}`, { params });
}
```

### Using `$meta` in Form Components

#### Component Setup Requirements

To successfully use `$meta` and the matching `IsMandatoryPipe` to handle form metadata dynamically, your standalone component requires a few specific declarations:

1. **Import the interfaces and the pipe** (along with any enum representations needed).
2. **Include `IsMandatoryPipe`** inside your `@Component({ imports: [] })` array.
3. **Declare a `signal`** inside your component class to hold the `$meta` response state.

```typescript
import { Component, signal } from '@angular/core';
// 1. Import MetaInfo interface and the IsMandatoryPipe
import { MetaInfo } from '../../../model/meta-info.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';
import { GroupType } from '../../../model/group/group.model';

@Component({
  selector: 'app-group-form',
  templateUrl: './group-form.component.html',
  standalone: true,
  // 2. You MUST provide IsMandatoryPipe here to make it available to the template!
  imports: [/* other imports... */, IsMandatoryPipe]
})
export class GroupFormComponent implements OnInit {
  // 3. Define the signal to hold the $meta response data once the endpoint returns it
  meta = signal<MetaInfo | null>(null);

  // The IsMandatoryPipe automatically handles the checking logic in the template from this meta signal.
  // The component may still optionally implement its own isMandatory() helper if logic requires checking form validators internally.
}
```

#### Loading `$meta` in create mode (no entity ID yet)

```typescript
// Use the dedicated $meta endpoint — no need to load a full list
this.groupsService.getMeta().subscribe({
  next: (metaInfo) => {
    this.meta.set(metaInfo);
    this.loading.set(false);
  },
  error: () => this.loading.set(false)
});
```

#### Loading `$meta` in edit mode (entity already exists)

```typescript
this.groupsService.getGroup(id).subscribe({
  next: (group: Group) => {
    if (group.$meta) {
      this.meta.set(group.$meta);
      if (group.$meta.enumValues?.['groupType']) {
        this.groupTypeValues = group.$meta.enumValues['groupType'];
      }
    }
    this.form.patchValue({ ... });
    this.loading.set(false);
  }
});
```

#### Template

Mandatory `*` markers and enum selector options are both driven by `$meta` — nothing is hardcoded. Use the standalone `IsMandatoryPipe` to check if a field is mandatory based on the metadata.

```html
<!-- Mandatory marker: shown only when $meta says the field is mandatory -->
<label class="form-label">
  {{ 'common.fields.groupType' | transloco }}
  @if ('groupType' | isMandatory: meta()) { <span class="text-danger">*</span> }
</label>

<!-- Enum values come from $meta.enumValues, falling back to the static groupTypeValues array -->
<app-enum-selector
  [values]="groupTypeValues"
  [formControlName]="'groupType'"
  [required]="'groupType' | isMandatory: meta()"
></app-enum-selector>
```

*Note: You must explicitly add `IsMandatoryPipe` to your standalone component's `imports` array.*

## Error Handling

### Backend Validation Errors

When the backend returns validation errors in the `$messages` array:

```typescript
interface Message {
  type: 'ERROR' | 'WARNING' | 'INFO';
  message: string;
  fields?: string[];
}

interface EntityResponse {
  id?: number;
  // ... entity fields
  $messages?: Message[];
}

@Component({
  selector: 'app-entity-form',
  templateUrl: './entity-form.component.html'
})
export class EntityFormComponent {
  form: FormGroup;
  fieldErrors = signal<Map<string, string[]>>(new Map());
  generalErrors = signal<string[]>([]);
  
  handleErrorResponse(response: EntityResponse): void {
    if (response.$messages) {
      const fieldErrors = new Map<string, string[]>();
      const generalErrors: string[] = [];
      
      response.$messages.forEach(msg => {
        if (msg.type === 'ERROR') {
          if (msg.fields && msg.fields.length > 0) {
            // Field-specific error
            msg.fields.forEach(field => {
              if (!fieldErrors.has(field)) {
                fieldErrors.set(field, []);
              }
              fieldErrors.get(field)!.push(msg.message);
            });
          } else {
            // General error
            generalErrors.push(msg.message);
          }
        }
      });
      
      this.fieldErrors.set(fieldErrors);
      this.generalErrors.set(generalErrors);
    }
  }
  
  hasFieldError(fieldName: string): boolean {
    return this.fieldErrors().has(fieldName);
  }
  
  getFieldErrors(fieldName: string): string[] {
    return this.fieldErrors().get(fieldName) || [];
  }
  
  onSubmit(): void {
    this.service.saveEntity(this.form.value).subscribe({
      next: (result) => {
        if (result.$messages?.some(m => m.type === 'ERROR')) {
          this.handleErrorResponse(result);
        } else {
          // Success
          this.router.navigate(['/entities']);
        }
      },
      error: (err) => {
        if (err.error?.$messages) {
          this.handleErrorResponse(err.error);
        } else {
          this.generalErrors.set(['An unexpected error occurred']);
        }
      }
    });
  }
}
```

### Template Error Display

```html
<!-- General errors at top of form -->
@if (generalErrors().length > 0) {
  <div class="alert alert-danger">
    @for (error of generalErrors(); track error) {
      <div>{{ error }}</div>
    }
  </div>
}

<!-- Field-specific errors -->
<div class="mb-3">
  <label class="form-label">Name</label>
  <input 
    type="text" 
    formControlName="name"
    [class.is-invalid]="hasFieldError('name')">
  @if (hasFieldError('name')) {
    <div class="invalid-feedback d-block">
      @for (error of getFieldErrors('name'); track error) {
        <div>{{ error }}</div>
      }
    </div>
  }
</div>
```

---

## Routing and Navigation

### Route Configuration

```typescript
// app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { 
    path: 'products', 
    children: [
      { path: '', component: ProductsComponent },
      { path: 'add', component: ProductFormComponent },
      { path: ':id', component: ProductDetailComponent },
      { path: ':id/edit', component: ProductFormComponent }
    ]
  }
];
```

### Route Parameters

```typescript
import { Component, signal, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html'
})
export class ProductDetailComponent {
  product = signal<Product | null>(null);
  productId = signal<number | null>(null);
  
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  
  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        const productId = parseInt(id, 10);
        this.productId.set(productId);
        this.loadProduct(productId);
      }
    });
  }
  
  loadProduct(id: number): void {
    this.productService.getProduct(id).subscribe({
      next: (product) => {
        this.product.set(product);
      },
      error: (err) => {
        console.error('Failed to load product:', err);
      }
    });
  }
}
```

### Programmatic Navigation

```typescript
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-product-form',
  templateUrl: './product-form.component.html'
})
export class ProductFormComponent {
  private router = inject(Router);
  
  onSaveSuccess(product: Product): void {
    // Navigate to detail page
    this.router.navigate(['/products', product.id]);
  }
  
  onCancel(): void {
    // Navigate back to list
    this.router.navigate(['/products']);
  }
}
```

### Query Parameters

```typescript
// Set query parameters
this.router.navigate(['/products'], {
  queryParams: { page: 2, sort: 'name' }
});

// Read query parameters
this.route.queryParamMap.subscribe(params => {
  const page = params.get('page') || '0';
  const sort = params.get('sort') || 'id';
  this.loadProducts(parseInt(page), sort);
});
```

---

## Best Practices Summary

### Component Development
- Use standalone components (default in Angular 20+)
- Use `input()` and `output()` functions instead of decorators
- Use `ChangeDetectionStrategy.OnPush` for performance
- Use `inject()` instead of constructor injection

### State Management
- Use signals for all reactive state
- Use `computed()` for derived values
- Avoid direct mutation - use `set()` or `update()`
- Always set signals in async callbacks to trigger change detection

### Forms
- Use Reactive Forms for complex forms
- Track original values for PATCH operations
- Build JSON Patch operations from form changes
- Handle both client-side and server-side validation

### API Communication
- Use services with `providedIn: 'root'`
- Use PATCH for updates (not PUT)
- Use POST to `/create` endpoint for new entities
- Handle errors with proper error messages

### Error Handling
- Display field-specific errors below fields
- Display general errors at top of form
- Use Bootstrap's `.is-invalid` and `.invalid-feedback` classes
- Clear errors when user starts editing

### Templates
- Use native control flow (`@if`, `@for`, `@switch`)
- Avoid complex logic in templates
- Use `track` in `@for` loops for performance
- Use `async` pipe for observables when appropriate

