# Reference List Edit Component

## Overview

The `referencelist-edit` component provides an autocomplete-enabled interface for editing multiple reference values (foreign keys, entity references) in forms. It supports async data loading with pagination, backend search filtering, keyboard navigation, and manages a list of selected references with individual remove capabilities.

## Component Selector

```typescript
<app-referencelist-edit>
```

## Purpose

This component is designed to handle multi-value reference fields (e.g., list of group IDs, tags, categories) that link to other entities. It provides:

- Display of selected references as removable badges
- Autocomplete input with async paginated data source
- **Backend search filtering** with debounced input (300ms delay)
- Load more functionality for large datasets
- Keyboard navigation (Arrow keys, Enter, Escape)
- Optional "Create new..." functionality
- Loading indicator during async operations
- Integration with Angular Reactive Forms via ControlValueAccessor
- Bootstrap-styled UI

## Input Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `label` | `string` | No | `'References'` | Label displayed above the component |
| `placeholder` | `string` | No | `'Add reference...'` | Placeholder text for the input field |
| `dataSource` | `ReferenceDataSource` | No | - | Function that returns paginated options based on search term and page number |
| `allowCreate` | `boolean` | No | `false` | Whether to show "Create new..." option |
| `createLabel` | `string` | No | `'Create new...'` | Label text for the create new option |

## Output Events

| Event | Payload Type | Description |
|-------|-------------|-------------|
| `changed` | `string[]` | Emitted when the list of selected references changes (add or remove) |
| `createNew` | `string` | Emitted when user clicks "Create new..." option. Payload is the current input value. |

## Type Definitions

### ReferenceOption Interface

```typescript
export interface ReferenceOption {
  value: string;  // The actual reference value (e.g., "group-123", "tag-abc")
  label: string;  // Display label (e.g., "Marketing Team", "Important")
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

## ControlValueAccessor

This component implements Angular's `ControlValueAccessor` interface, making it a custom form control that works seamlessly with Angular Reactive Forms:

```typescript
// The component value is always a string array
value: string[] = ['group-1', 'group-2', 'group-3'];
```

## Usage Example

### 1. Basic Usage with FormControl

```typescript
import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ReferenceListEditComponent, ReferenceDataSourceResult } from './components/referencelist-edit/referencelist-edit.component';
import { GroupsService } from './services/groups.service';

@Component({
  selector: 'app-organization-form',
  templateUrl: './organization-form.component.html',
  imports: [ReferenceListEditComponent, ReactiveFormsModule]
})
export class OrganizationFormComponent {
  form: FormGroup;
  
  constructor(
    private fb: FormBuilder,
    private groupsService: GroupsService
  ) {
    this.form = this.fb.group({
      name: [''],
      groupIds: [['group-1', 'group-2']]  // Initialize with existing group IDs
    });
  }

  // Paginated data source function for group autocomplete with backend filtering
  groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    // Build query for backend filtering - search in id OR name
    const query = searchTerm ? `id:*${searchTerm}* OR name:*${searchTerm}*` : undefined;
    
    return this.groupsService.getGroups(page, 20, undefined, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(g => ({
          value: g.groupId,
          label: `${g.groupId} - ${g.name}`
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  onGroupsChanged(groupIds: string[]): void {
    console.log('Selected groups:', groupIds);
  }
}
```

### 2. Template Usage

```html
<form [formGroup]="form">
  <div class="mb-3">
    <app-referencelist-edit
      [label]="'Member Groups'"
      formControlName="groupIds"
      [dataSource]="groupsDataSource"
      placeholder="Add group..."
      (changed)="onGroupsChanged($event)">
    </app-referencelist-edit>
  </div>
</form>
```

### 3. Advanced Usage with Create New

```typescript
import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ModalService } from './services/modal.service';
import { GroupFormComponent } from './components/group-form/group-form.component';

@Component({
  selector: 'app-organization-form',
  templateUrl: './organization-form.component.html'
})
export class OrganizationFormComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private groupsService: GroupsService,
    private modalService: ModalService
  ) {
    this.form = this.fb.group({
      groupIds: [[]]
    });
  }

  groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    return this.groupsService.getGroups(page, 20).pipe(
      map(response => ({
        options: response.items.map(g => ({
          value: g.groupId,
          label: `${g.groupId} - ${g.name}`
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  async onCreateNewGroup(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(GroupFormComponent, {
      title: 'Create New Group',
      size: 'lg',
      data: { initialValue: searchTerm, modalMode: true }
    });

    if (result.success && result.data) {
      // Add the newly created group to the list
      const currentGroups = this.form.get('groupIds')?.value || [];
      this.form.get('groupIds')?.setValue([...currentGroups, result.data.groupId]);
    }
  }
}
```

```html
<app-referencelist-edit
  formControlName="groupIds"
  [dataSource]="groupsDataSource"
  placeholder="Add group..."
  [allowCreate]="true"
  createLabel="Create new group..."
  (createNew)="onCreateNewGroup($event)">
</app-referencelist-edit>
```

## Features

### 1. Backend Search Filtering

- **Automatic search triggering**: As users type in the input field, backend search is triggered automatically
- **Debounced input**: 300ms delay to avoid excessive API calls while typing
- **Query syntax**: Uses Lucene-like syntax (e.g., `field:*searchTerm*`) for backend filtering
- **Real-time results**: Dropdown updates with filtered results from the server
- **Multi-field search**: Supports searching across multiple fields using OR operators (e.g., `field1:*term* OR field2:*term*`)

### 2. Selected References Display

- Selected references displayed as removable badges above the input
- Each badge has a trash icon button for removal
- Empty state shows an empty bordered area

### 3. Autocomplete Dropdown with Pagination

- Dropdown appears when user clicks or focuses on the input
- Displays options with value and label (label only shown if different from value)
- Click or Enter to select an option
- "Load More..." button appears when more pages are available
- Automatically filters out already selected items
- Initial page loaded automatically on first open

### 4. Async Data Loading

- Supports async paginated data sources via Observable
- Shows loading spinner while fetching data
- Handles errors gracefully (returns empty array on error)
- Prevents dropdown from closing while loading more items
- Filters out already selected references from dropdown

### 5. Keyboard Navigation

- **Arrow Down**: Navigate down in the dropdown list, wrapping to top
- **Arrow Up**: Navigate up in the dropdown list, wrapping to bottom
- **Enter**: Select the highlighted option, load more items, or create new
- **Escape**: Close dropdown and blur the input field

### 5. Load More Feature

- "Load More..." button appears when `hasMore` is true
- Loads the next page of results
- Appends new results to existing options (excluding already selected)
- Keyboard navigable with arrow keys
- Prevents blur while loading

### 6. Create New Feature

When `allowCreate` is enabled:
- Shows "Create new..." option when user has entered text
- Displays below the "Load More..." option (if present)
- Emits `createNew` event with the current input value when clicked
- Parent component can open a modal or navigate to creation form

### 7. Remove References

- Each selected reference has a trash icon button
- Click to remove individual references
- Updates form control value and emits `changed` event

### 8. Loading Indicator

- Spinner appears in the dropdown while data is being fetched
- "Loading..." text displayed for accessibility
- Automatically hidden when data arrives

## Dropdown Behavior

### Opening the Dropdown

The dropdown opens when:
1. User focuses on the input field
2. User clicks on the input field

Initial page is loaded automatically when component initializes.

### Closing the Dropdown

The dropdown closes when:
1. User clicks outside the component (document click handler)
2. User presses Escape key (document keydown handler)
3. User selects an option (input is cleared, ready for next selection)
4. User clicks "Create new..."

### Load More Behavior

When "Load More..." is clicked:
- `preventBlurClose` flag is set to prevent dropdown from closing
- Next page number is calculated and loaded
- New options are appended to existing options
- Already selected references are filtered out
- Dropdown remains open for continued browsing

### Auto-filtering Selected Items

The component automatically filters out selected references from the dropdown:
- When loading initial page
- When loading more pages
- Ensures users can't select the same reference twice

## Internal State Management

The component uses Angular signals for reactive state:

```typescript
// Form control for the input field
inputFormControl = new FormControl('');

// Options available in the dropdown (excluding already selected)
availableReferenceOptions = signal<ReferenceOption[]>([]);

// Whether dropdown is visible
showDropdown = signal(false);

// Whether data is being loaded
isLoading = signal(false);

// List of selected reference values
selectedReferencesList = signal<string[]>([]);

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

### Container
- `.referencelist-container` - Main container

### Selected References Display
- `.reference-list` - Container for selected badges
- `.ref-badge` - Individual reference badge
- `.ref-badge-removable` - Adds remove button
- `.ref-badge-clear` - Trash icon button

### Input Field
- `.form-control` - Standard Bootstrap form control styling
- `.reference-input-wrapper` - Wrapper for positioning

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
.referencelist-container {
  // Container for the entire component
}

.reference-list {
  // Display area for selected references
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.5rem;
  border: 1px solid var(--bs-border-color);
  border-radius: 0.25rem;
  min-height: 3rem;
}

.ref-badge {
  // Individual selected reference badge
  padding: 0.25rem 0.5rem;
  background-color: #e7f3ff;
  border: 1px solid #b3d7ff;
  border-radius: 0.25rem;
}

.ref-badge-clear {
  // Remove button for each badge
  border: none;
  background: none;
  cursor: pointer;
  color: var(--bs-secondary);
  
  &:hover {
    color: var(--bs-danger);
  }
}

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
getGroups(page: number, pageSize: number): Observable<GroupList> {
  return this.http.get<GroupList>(`/api/groups?page=${page}&page-size=${pageSize}`);
}

// In component
groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
  return this.groupsService.getGroups(page, 20).pipe(
    map(response => ({
      options: response.items
        .filter(g => !searchTerm || g.groupId.toLowerCase().includes(searchTerm.toLowerCase()))
        .map(g => ({
          value: g.groupId,
          label: `${g.groupId} - ${g.name || ''}`
        })),
      hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
    })),
    catchError(() => of({ options: [], hasMore: false }))
  );
};
```

## Best Practices

### 1. Initialize with Existing Values

When loading an entity for editing, initialize the form control with existing reference IDs:

```typescript
loadOrganization(org: Organization): void {
  this.form.patchValue({
    name: org.name,
    groupIds: org.groupIds || []  // Initialize with existing group IDs
  });
}
```

### 2. Handle the changed Event

Use the `changed` event to react to list modifications:

```typescript
onGroupsChanged(groupIds: string[]): void {
  console.log('Current groups:', groupIds);
  // Update dependent fields or trigger validations
}
```

### 3. Validate Reference Lists

Add validators to ensure the list meets requirements:

```typescript
form = this.fb.group({
  groupIds: [[], [Validators.required, Validators.minLength(1)]]
});
```

### 4. Handle Create New Workflow

Provide a smooth workflow for creating new referenced entities and adding them to the list:

```typescript
async onCreateNewGroup(searchTerm: string): Promise<void> {
  try {
    const newGroup = await this.groupService.createGroup({
      groupId: searchTerm,
      name: searchTerm
    }).toPromise();
    
    // Add to the list
    const currentGroups = this.form.get('groupIds')?.value || [];
    this.form.get('groupIds')?.setValue([...currentGroups, newGroup.groupId]);
    
    this.showSuccessMessage(`Group "${newGroup.groupId}" created and added`);
  } catch (error) {
    this.showErrorMessage('Failed to create group');
  }
}
```

### 5. Implement Pagination

Always implement proper pagination in your data source with correct `hasMore` calculation:

```typescript
groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
  return this.groupsService.getGroups(page, 20).pipe(
    map(response => ({
      options: response.items.map(g => ({ value: g.groupId, label: g.groupId })),
      hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
    })),
    catchError(() => of({ options: [], hasMore: false }))
  );
};
```

## Common Issues and Solutions

### Issue: Selected items reappear in dropdown

**Solution**: The component automatically filters selected items. Ensure your data source returns consistent values:

```typescript
// Values should be stable and match exactly
{ value: 'group-123', label: 'Marketing Team' }
```

### Issue: Can't remove items

**Solution**: Ensure you're not preventing event propagation on the remove button's parent elements.

### Issue: Form value not updating

**Solution**: The component implements ControlValueAccessor correctly. Ensure you're using `formControlName` in the template:

```html
<app-referencelist-edit formControlName="groupIds" ...>
```

### Issue: hasMore not working

**Solution**: Ensure your data source correctly calculates `hasMore` based on pagination info:

```typescript
hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
```

## Related Components

- **[reference-edit](./reference-edit.md)** - For editing single reference values
- `.ref-badge` CSS class - For displaying reference values as badges (read-only)

## Differences from reference-edit

| Feature | reference-edit | referencelist-edit |
|---------|---------------|-------------------|
| Value type | Single string | Array of strings |
| Form integration | Direct FormControl binding | ControlValueAccessor (custom form control) |
| Display | Input only | List of selected + input |
| Clear functionality | Delete key | Individual remove buttons |
| Use case | Single foreign key reference | Multiple references/tags |
| Form binding | `[inputFormControl]="form.get('field')"` | `formControlName="field"` |

## Accessibility

The component includes accessibility features:
- Keyboard navigation (Arrow keys, Enter, Escape)
- Document-level event handlers for consistent behavior
- Loading state announcements
- Proper button types for dropdown items
- Semantic HTML with proper button roles

## Integration with Angular Forms

As a ControlValueAccessor, the component integrates seamlessly with:

### Reactive Forms

```typescript
form = this.fb.group({
  groupIds: [['initial-1', 'initial-2'], Validators.required]
});
```

```html
<app-referencelist-edit formControlName="groupIds" ...>
</app-referencelist-edit>
```

### Getting/Setting Values

```typescript
// Get current value
const groupIds = this.form.get('groupIds')?.value;  // string[]

// Set value programmatically
this.form.get('groupIds')?.setValue(['new-1', 'new-2']);

// Patch value
this.form.patchValue({ groupIds: ['new-1', 'new-2'] });
```

### Validation

```typescript
// Built-in validators
groupIds: [[], [Validators.required, Validators.minLength(1)]]

// Custom validator
function maxReferences(max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string[];
    return value && value.length > max ? { maxReferences: { max, actual: value.length } } : null;
  };
}

groupIds: [[], [Validators.required, maxReferences(5)]]
```

## Lifecycle

The component implements:
- `OnInit` - Loads initial data
- `OnDestroy` - Cleans up subscriptions
- `ControlValueAccessor` - Integrates with Angular forms

## Example: Complete Form Integration

```typescript
@Component({
  selector: 'app-organization-form',
  templateUrl: './organization-form.component.html'
})
export class OrganizationFormComponent implements OnInit {
  form: FormGroup;
  
  constructor(
    private fb: FormBuilder,
    private orgService: OrganizationService,
    private groupsService: GroupsService
  ) {
    this.form = this.fb.group({
      organizationId: ['', Validators.required],
      name: ['', Validators.required],
      groupIds: [[], [Validators.required, Validators.minLength(1)]]
    });
  }

  ngOnInit(): void {
    const orgId = this.route.snapshot.paramMap.get('id');
    if (orgId) {
      this.loadOrganization(orgId);
    }
  }

  loadOrganization(id: string): void {
    this.orgService.getOrganization(id).subscribe(org => {
      this.form.patchValue({
        organizationId: org.organizationId,
        name: org.name,
        groupIds: org.groupIds || []
      });
    });
  }

  groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    return this.groupsService.getGroups(page, 20).pipe(
      map(response => ({
        options: response.items.map(g => ({
          value: g.groupId,
          label: `${g.groupId} - ${g.name}`
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      })),
      catchError(() => of({ options: [], hasMore: false }))
    );
  };

  onSubmit(): void {
    if (this.form.valid) {
      const formValue = this.form.value;
      this.orgService.updateOrganization(formValue).subscribe({
        next: () => this.router.navigate(['/organizations']),
        error: (err) => console.error('Save failed:', err)
      });
    }
  }
}
```

```html
<form [formGroup]="form" (ngSubmit)="onSubmit()">
  <div class="mb-3">
    <label class="form-label">Organization ID</label>
    <input type="text" class="form-control" formControlName="organizationId">
  </div>

  <div class="mb-3">
    <label class="form-label">Name</label>
    <input type="text" class="form-control" formControlName="name">
  </div>

  <div class="mb-3">
    <app-referencelist-edit
      label="Member Groups"
      formControlName="groupIds"
      [dataSource]="groupsDataSource"
      placeholder="Add group..."
      [allowCreate]="true">
    </app-referencelist-edit>
    @if (form.get('groupIds')?.invalid && form.get('groupIds')?.touched) {
      <div class="invalid-feedback d-block">
        At least one group is required.
      </div>
    }
  </div>

  <button type="submit" class="btn btn-primary" [disabled]="form.invalid">
    Save Organization
  </button>
</form>
```
