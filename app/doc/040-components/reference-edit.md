# Reference Edit Component

## Overview

The `reference-edit` component provides an autocomplete-enabled input field for editing single reference values (foreign keys, entity references) in forms. It supports async data loading with pagination, backend search filtering, keyboard navigation, and optionally creating new referenced entities on the fly.

## Component Selector

```typescript
<app-reference-edit>
```

## Purpose

This component is designed to handle single-value reference fields (e.g., `unitRef`, `currencyRef`, `taxClassRef`) that link to other entities. It provides:

- Autocomplete dropdown with async paginated data source
- **Backend search filtering** with debounced input (300ms delay)
- Load more functionality for large datasets
- Keyboard navigation (Arrow keys, Enter, Escape, Delete)
- Optional "Create new..." functionality
- Loading indicator during async operations
- Integration with Angular Reactive Forms
- Bootstrap-styled dropdown UI

## Input Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `inputFormControl` | `AbstractControl \| null \| undefined` | Yes | - | The form control to bind to (must be a `FormControl`) |
| `placeholder` | `string` | No | `''` | Placeholder text for the input field |
| `isInvalid` | `boolean` | No | `false` | Whether to display the field as invalid (applies Bootstrap's `is-invalid` class) |
| `dataSource` | `ReferenceDataSource` | No | - | Function that returns paginated options based on search term and page number |
| `allowCreate` | `boolean` | No | `false` | Whether to show "Create new..." option |
| `createLabel` | `string` | No | `'Create new...'` | Label text for the create new option |

## Output Events

| Event | Payload Type | Description |
|-------|-------------|-------------|
| `cleared` | `void` | Emitted when user clears the field using the Delete key |
| `createNew` | `string` | Emitted when user clicks "Create new..." option. Payload is the current input value. |

## Type Definitions

### ReferenceOption Interface

```typescript
export interface ReferenceOption {
  value: string;  // The actual reference value (e.g., "kg", "EUR")
  label: string;  // Display label (e.g., "kilogram", "Euro")
}
```

### ReferenceDataSourceResult Interface

```typescript
export interface ReferenceDataSourceResult {
  options: ReferenceOption[];  // Array of options for the current page
  hasMore: boolean;            // Whether more pages are available
}
```

### ReferenceDataSource Type

```typescript
export type ReferenceDataSource = (searchTerm: string, page: number) => Observable<ReferenceDataSourceResult>;
```

## Usage Example

### 1. Basic Usage with Pagination

```typescript
import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ReferenceEditComponent, ReferenceDataSourceResult } from './components/reference-edit/reference-edit.component';
import { UnitsService } from './services/units.service';

@Component({
  selector: 'app-price-form',
  templateUrl: './price-form.component.html',
  imports: [ReferenceEditComponent, ReactiveFormsModule]
})
export class PriceFormComponent {
  form: FormGroup;
  
  constructor(
    private fb: FormBuilder,
    private unitsService: UnitsService
  ) {
    this.form = this.fb.group({
      unitRef: ['']
    });
  }

  // Paginated data source function for unit autocomplete with backend filtering
  unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    // Build query for backend filtering using Lucene-like syntax
    const query = searchTerm ? `symbol:*${searchTerm}*` : undefined;
    
    return this.unitsService.getUnits(page, 20, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(u => ({
          value: u.symbol,
          label: u.symbol
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  hasFieldError(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    return !!(control && control.invalid && control.touched);
  }
}
```

### 2. Template Usage

```html
<form [formGroup]="form">
  <div class="mb-3">
    <label class="form-label">Unit</label>
    <app-reference-edit
      [inputFormControl]="form.get('unitRef')"
      [dataSource]="unitsDataSource"
      placeholder="e.g., kg"
      [isInvalid]="hasFieldError('unitRef')">
    </app-reference-edit>
    @if (hasFieldError('unitRef')) {
      <div class="invalid-feedback d-block">
        Unit is required.
      </div>
    }
  </div>
</form>
```

### 3. Advanced Usage with Create New

```typescript
import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ModalService } from './services/modal.service';
import { UnitFormComponent } from './components/unit-form/unit-form.component';

@Component({
  selector: 'app-price-form',
  templateUrl: './price-form.component.html'
})
export class PriceFormComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private unitsService: UnitsService,
    private modalService: ModalService
  ) {
    this.form = this.fb.group({
      unitRef: ['']
    });
  }

  unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    return this.unitsService.getUnits(page, 20).pipe(
      map(response => ({
        options: response.items.map(u => ({
          value: u.symbol,
          label: u.symbol
        })),
        hasMore: response.$info.paging.page < response.$info.paging.totalPages - 1
      }))
    );
  };

  async onCreateNewUnit(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(UnitFormComponent, {
      title: 'Create New Unit',
      size: 'lg',
      data: { initialValue: searchTerm, modalMode: true }
    });

    if (result.success && result.data) {
      // Set the newly created unit's symbol in the form
      this.form.get('unitRef')?.setValue(result.data.symbol);
    }
  }
}
```

```html
<app-reference-edit
  [inputFormControl]="form.get('unitRef')"
  [dataSource]="unitsDataSource"
  placeholder="e.g., kg"
  [isInvalid]="hasFieldError('unitRef')"
  [allowCreate]="true"
  createLabel="Create new unit..."
  (createNew)="onCreateNewUnit($event)">
</app-reference-edit>
```

## Features

### 1. Backend Search Filtering

- **Automatic search triggering**: As users type in the input field, backend search is triggered automatically
- **Debounced input**: 300ms delay to avoid excessive API calls while typing
- **Query syntax**: Uses Lucene-like syntax (e.g., `field:*searchTerm*`) for backend filtering
- **Real-time results**: Dropdown updates with filtered results from the server
- **Multi-field search**: Supports searching across multiple fields using OR operators (e.g., `field1:*term* OR field2:*term*`)

### 2. Autocomplete Dropdown with Pagination

- Dropdown appears when user clicks or focuses on the input
- Displays options with value and label (label only shown if different from value)
- Click or Enter to select an option
- "Load More..." button appears when more pages are available
- Automatically loads initial page when dropdown opens

### 3. Async Data Loading

- Supports async paginated data sources via Observable
- Shows loading spinner while fetching data
- Handles errors gracefully (returns empty array on error)
- Prevents dropdown from closing while loading more items

### 4. Keyboard Navigation

- **Arrow Down**: Navigate down in the dropdown list, wrapping to top
- **Arrow Up**: Navigate up in the dropdown list, wrapping to bottom
- **Enter**: Select the highlighted option, load more items, or create new
- **Escape**: Close dropdown and blur the input field
- **Delete**: Clear the current value (emits `cleared` event)

### 4. Load More Feature

- "Load More..." button appears when `hasMore` is true
- Loads the next page of results
- Appends new results to existing options
- Keyboard navigable with arrow keys
- Prevents blur while loading

### 5. Create New Feature

When `allowCreate` is enabled:
- Shows "Create new..." option when user has entered text
- Displays below the "Load More..." option (if present)
- Emits `createNew` event with the current input value when clicked
- Parent component can open a modal or navigate to creation form

### 6. Loading Indicator

- Spinner appears in the dropdown while data is being fetched
- "Loading..." text displayed for accessibility
- Automatically hidden when data arrives

## Dropdown Behavior

### Opening the Dropdown

The dropdown opens when:
1. User focuses on the input field
2. User clicks on the input field

Initial page is loaded automatically on first open.

### Closing the Dropdown

The dropdown closes when:
1. User clicks outside the component (document click handler)
2. User presses Escape key (document keydown handler)
3. User selects an option
4. User clicks "Create new..."

### Load More Behavior

When "Load More..." is clicked:
- `preventBlurClose` flag is set to prevent dropdown from closing
- Next page number is calculated and loaded
- New options are appended to existing options
- Dropdown remains open for continued browsing

## Internal State Management

The component uses Angular signals for reactive state:

```typescript
// Options available in the dropdown
availableReferenceOptions = signal<ReferenceOption[]>([]);

// Whether dropdown is visible
showDropdown = signal(false);

// Whether data is being loaded
isLoading = signal(false);

// Current page number for pagination
currentPage = signal(0);

// Whether more items are available to load
hasMoreItems = signal(false);

// Currently highlighted option index for keyboard navigation
selectedIndex = signal<number>(-1);

// Prevent blur from closing dropdown (used during load more)
private preventBlurClose = false;

// Whether initial data has been loaded
private hasLoadedData = false;
```

## Styling

The component uses Bootstrap classes and custom CSS for styling:

### Input Field
- `.form-control` - Standard Bootstrap form control styling
- `.is-invalid` - Applied when `isInvalid` is true

### Dropdown Container
- `.dropdown-menu` - Bootstrap dropdown menu styling
- `.show` - Makes dropdown visible
- `position-relative` on wrapper for positioning

### Dropdown Items
- `.dropdown-item` - Bootstrap dropdown item styling
- `.highlighted` - Applied to keyboard-selected item
- `.disabled` - Applied to loading indicator

### Special Items
- `.dropdown-item-load-more` - "Load More..." button styling with info color
- `.dropdown-item-create` - "Create new..." button styling with primary color

## CSS Structure

```scss
.reference-input-wrapper {
  position: relative;
  flex: 1;
  min-width: 200px;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
  width: 100%;
}

.dropdown-item {
  padding: 0.5rem 1rem;
  cursor: pointer;
  
  &.highlighted {
    background-color: var(--bs-light);
  }
  
  &.dropdown-item-load-more {
    color: var(--bs-info);
    border-top: 1px solid var(--bs-border-color);
  }
  
  &.dropdown-item-create {
    color: var(--bs-primary);
    border-top: 2px solid var(--bs-primary);
  }
}
```

## Data Source Implementation

### Paginated Service-Based Data Source

```typescript
// In service
getUnits(page: number, pageSize: number): Observable<UnitList> {
  return this.http.get<UnitList>(`/api/units?page=${page}&page-size=${pageSize}`);
}

// In component
unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
  return this.unitsService.getUnits(page, 20).pipe(
    map(response => ({
      options: response.items
        .filter(u => !searchTerm || u.symbol.toLowerCase().includes(searchTerm.toLowerCase()))
        .map(u => ({
          value: u.symbol,
          label: u.symbol
        })),
      hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
    })),
    catchError(() => of({ options: [], hasMore: false }))
  );
};
```

### Client-Side Filtering with Pagination Simulation

```typescript
unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
  const allUnits: ReferenceOption[] = [
    { value: 'kg', label: 'Kilogram' },
    { value: 'g', label: 'Gram' },
    { value: 'l', label: 'Liter' },
    // ... more units
  ];
  
  const filtered = allUnits.filter(u => 
    !searchTerm || 
    u.value.toLowerCase().includes(searchTerm.toLowerCase()) ||
    u.label.toLowerCase().includes(searchTerm.toLowerCase())
  );
  
  const pageSize = 10;
  const start = page * pageSize;
  const end = start + pageSize;
  const pageItems = filtered.slice(start, end);
  
  return of({
    options: pageItems,
    hasMore: end < filtered.length
  });
};
```

## Best Practices

### 1. Use Backend Filtering for Large Datasets

Always implement backend filtering with proper query syntax to improve performance:

```typescript
unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
  // Build query for backend filtering using Lucene-like syntax
  const query = searchTerm ? `symbol:*${searchTerm}*` : undefined;
  
  return this.unitsService.getUnits(page, 20, undefined, undefined, query).pipe(
    map(response => ({
      options: response.items.map(u => ({ value: u.symbol, label: u.symbol })),
      hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
    })),
    catchError(() => of({ options: [], hasMore: false }))
  );
};
```

### 2. Search Multiple Fields with OR Operator

When searching across multiple fields, use the OR operator:

```typescript
currenciesDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
  // Search in both currencyKey and symbol fields
  const query = searchTerm ? `currencyKey:*${searchTerm}* OR symbol:*${searchTerm}*` : undefined;
  
  return this.currenciesService.getCurrencies(page, 20, undefined, undefined, query).pipe(
    map(response => ({
      options: response.items.map(c => ({
        value: c.currencyKey,
        label: `${c.currencyKey}${c.symbol ? ' (' + c.symbol + ')' : ''}`
      })),
      hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
    }))
  );
};
```

### 3. Implement Proper Pagination

Ensure your service methods support the query parameter:

```typescript
// In service
getUnits(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string) {
  let params = new HttpParams()
    .set('page', page.toString())
    .set('page-size', pageSize.toString());
  
  if (query) {
    params = params.set('q', query);
  }
  
  return this.http.get<UnitList>(url, { params });
}
```

### 4. Handle Errors Gracefully

Always include error handling in your data source:

```typescript
dataSource = (term: string, page: number): Observable<ReferenceDataSourceResult> => {
  return this.service.search(term, page).pipe(
    catchError(error => {
      console.error('Error loading options:', error);
      return of({ options: [], hasMore: false });
    })
  );
};
```

### 3. Provide Meaningful Labels

Make labels descriptive to help users identify the correct option:

```typescript
{
  value: 'EUR',
  label: 'EUR (€) - Euro'
}
```

### 4. Clear Dependent Fields

When a reference is cleared, clear dependent fields:

```typescript
onUnitCleared(): void {
  this.form.patchValue({
    quantity: null,
    minQuantity: null
  });
}
```

### 5. Validate Reference Fields

Ensure selected references actually exist using validators.

## Common Issues and Solutions

### Issue: Dropdown not appearing

**Solution**: Ensure `dataSource` function is provided and returns an Observable with the correct structure:

```typescript
<app-reference-edit
  [inputFormControl]="form.get('unitRef')"
  [dataSource]="unitsDataSource">  <!-- Must be provided -->
</app-reference-edit>
```

### Issue: hasMore not working

**Solution**: Ensure your data source correctly calculates `hasMore`:

```typescript
hasMore: response.$info.paging.page < response.$info.paging.totalPages - 1
```

### Issue: Loading indicator stuck

**Solution**: Ensure your data source Observable completes or errors:

```typescript
dataSource = (term: string, page: number): Observable<ReferenceDataSourceResult> => {
  return this.service.search(term, page).pipe(
    catchError(() => of({ options: [], hasMore: false })) // Return proper structure
  );
};
```

## Related Components

- **[referencelist-edit](./referencelist-edit.md)** - For editing multiple reference values (array/list)
- `.ref-badge` CSS class - For displaying reference values as badges (read-only)

## Differences from referencelist-edit

| Feature | reference-edit | referencelist-edit |
|---------|---------------|-------------------|
| Value type | Single string | Array of strings |
| Form integration | Direct FormControl binding | ControlValueAccessor (custom form control) |
| Display | Input only | List of selected + input |
| Clear functionality | Delete key | Individual remove buttons |
| Use case | Single foreign key reference | Multiple references/tags |

## Accessibility

The component includes accessibility features:
- Keyboard navigation (Arrow keys, Enter, Escape, Delete)
- Document-level event handlers for consistent behavior
- Loading state announcements
- Proper button types for dropdown items

## Migration from Old Version

If migrating from the old `reference-edit` component:

1. **Update data source signature** from `(searchTerm: string) => Observable<ReferenceOption[]>` to `(searchTerm: string, page: number) => Observable<ReferenceDataSourceResult>`
2. **Update data source return value** to include `hasMore` property
3. **Remove custom styling** - component now uses Bootstrap's standard dropdown styling
4. **Update property name** - `control` is now `inputFormControl`
5. **Remove inline clear button** - use Delete key instead

Example migration:

```typescript
// Old
dataSource = (term: string): Observable<ReferenceOption[]> => {
  return this.service.getAll().pipe(
    map(items => items.filter(i => i.name.includes(term)))
  );
};

// New
dataSource = (term: string, page: number): Observable<ReferenceDataSourceResult> => {
  return this.service.getAll(page, 20).pipe(
    map(response => ({
      options: response.items
        .filter(i => !term || i.name.includes(term))
        .map(i => ({ value: i.id, label: i.name })),
      hasMore: response.$info.paging.page < response.$info.paging.totalPages - 1
    }))
  );
};
```
