# Localized String Field Edit Component

## Overview

The `localized-stringfield-edit` component provides a comprehensive solution for editing multi-language text fields in forms. It automatically manages language availability, mandatory/optional language states, and provides a user-friendly interface for adding or removing language-specific values.

## Component Selector

```typescript
<app-localized-stringfield-edit>
```

## Purpose

This component is designed to handle localized fields (e.g., product names, descriptions) that need to support multiple languages. It integrates with Angular Reactive Forms and the application's language configuration system to provide:

- Display of mandatory and optional language fields
- Dynamic addition/removal of language-specific fields
- Automatic handling of inactive languages (read-only display)
- Toggle between showing all values or just mandatory ones
- Field-level validation error display

## Input Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `form` | `FormGroup` | Yes | - | The parent form group containing the language-specific form controls |
| `fieldPrefix` | `string` | No | `'field_'` | Prefix for form control names (e.g., `'name_'` generates `name_en`, `name_de`) |
| `fieldLabel` | `string` | No | `'Field'` | Label displayed for the field section |
| `configuredLanguages` | `signal<string[]>` | No | `signal([])` | Signal containing array of configured language codes for this field |
| `fieldErrors` | `signal<Map<string, string[]>>` | No | `signal(new Map())` | Signal containing validation errors by field name |
| `isEditMode` | `boolean` | No | `false` | Whether the form is in edit mode |
| `isModalMode` | `boolean` | No | `false` | Whether the component is used in a modal dialog |

## Output Events

| Event | Payload Type | Description |
|-------|-------------|-------------|
| `removeLanguageEvent` | `string` | Emitted when user removes a language field. Payload is the language code. |
| `addLanguageEvent` | `string` | Emitted when user adds a language field. Payload is the language code. |

## Usage Example

### 1. Component Setup

```typescript
import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LocalizedStringfieldEditComponent } from './components/localized-stringfield-edit/localized-stringfield-edit.component';

@Component({
  selector: 'app-product-form',
  templateUrl: './product-form.component.html',
  imports: [LocalizedStringfieldEditComponent, ReactiveFormsModule]
})
export class ProductFormComponent {
  form: FormGroup;
  nameLanguages = signal<string[]>(['en', 'de']); // Initial languages
  fieldErrors = signal<Map<string, string[]>>(new Map());
  isEditMode = signal(false);

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      name_en: ['', Validators.required],
      name_de: ['', Validators.required],
      name_fr: [''] // Optional language
    });
  }

  removeLanguage(lang: string): void {
    const controlName = `name_${lang}`;
    this.form.removeControl(controlName);
    this.nameLanguages.update(langs => langs.filter(l => l !== lang));
  }

  addLanguage(lang: string): void {
    const controlName = `name_${lang}`;
    this.form.addControl(controlName, this.fb.control(''));
    this.nameLanguages.update(langs => [...langs, lang]);
  }
}
```

### 2. Template Usage

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

## Features

### 1. Mandatory vs. Optional Languages

The component automatically distinguishes between mandatory and optional languages based on the session configuration:

- **Mandatory languages**: Marked with a red asterisk (*), cannot be removed
- **Optional languages**: Can be removed using the trash icon button
- **Inactive languages**: Displayed with reduced opacity and are read-only

### 2. Show/Hide Optional Values

When non-mandatory languages contain values:
- A "View All Localized Values" button appears to show all language fields with values
- Toggle to "Hide Optional Values" to show only mandatory fields

### 3. Add Language Functionality

- Displays a dropdown button "Add Language" when optional languages are available
- Lists all active languages not currently configured for the field
- Automatically tracks recently added languages to keep them visible even if empty

### 4. Validation Error Display

The component displays validation errors in two ways:
1. **Form-level validation**: Shows "This field is required" when a field is touched and invalid
2. **Server-side validation**: Displays errors from the `fieldErrors` signal, matching errors by field name

### 5. Inactive Language Handling

Inactive languages (from session configuration):
- Are displayed with reduced opacity (50%)
- Show "(inactive language)" in the placeholder
- Have their form controls disabled automatically
- Cannot be removed (no trash icon)

## Internal State Management

The component uses Angular signals for reactive state management:

```typescript
// Track whether to show all values or just mandatory
showAllLocalizedValues = signal<boolean>(false);

// Track recently added languages (keeps them visible even if empty)
recentlyAddedLanguages = signal<string[]>([]);

// Computed: languages that are mandatory
mandatoryLanguages = computed(() => { ... });

// Computed: languages currently visible in the UI
visibleLanguages = computed(() => { ... });

// Computed: languages available to add
availableToAdd = computed(() => { ... });

// Computed: whether there are hidden values
hasHiddenLocalizedValues = computed(() => { ... });
```

## Styling

The component uses Bootstrap classes for styling:
- `input-group` and `input-group-text` for language prefix labels
- `form-control` for text inputs
- `btn btn-outline-danger` for remove buttons
- `btn btn-sm btn-outline-secondary` for show/hide buttons
- `btn btn-sm btn-outline-primary` for add language dropdown
- `is-invalid` and `invalid-feedback` for validation errors
- `opacity-50` for inactive languages

## Form Control Naming Convention

Form controls must follow the naming pattern: `{fieldPrefix}{languageCode}`

Examples:
- Field prefix `'name_'` generates: `name_en`, `name_de`, `name_fr`
- Field prefix `'description_'` generates: `description_en`, `description_de`

## Best Practices

### 1. Initialize Form with Existing Data

When loading an entity for editing, initialize form controls for all configured languages:

```typescript
loadProduct(product: Product): void {
  const configuredLangs = Object.keys(product.name || {});
  this.nameLanguages.set(configuredLangs);
  
  configuredLangs.forEach(lang => {
    const controlName = `name_${lang}`;
    if (!this.form.contains(controlName)) {
      this.form.addControl(controlName, this.fb.control(product.name[lang]));
    } else {
      this.form.get(controlName)?.setValue(product.name[lang]);
    }
  });
}
```

### 2. Handle Validation Errors from Backend

Map server-side validation errors to the `fieldErrors` signal:

```typescript
handleError(response: any): void {
  if (response.$messages) {
    const errors = new Map<string, string[]>();
    response.$messages.forEach((msg: any) => {
      if (msg.fields && msg.fields.length > 0) {
        msg.fields.forEach((field: string) => {
          if (!errors.has(field)) {
            errors.set(field, []);
          }
          errors.get(field)!.push(msg.message);
        });
      }
    });
    this.fieldErrors.set(errors);
  }
}
```

### 3. Collect Values for Save

When saving, collect all language values into a Map or object:

```typescript
collectLocalizedValues(fieldPrefix: string): { [key: string]: string } {
  const result: { [key: string]: string } = {};
  Object.keys(this.form.controls).forEach(controlName => {
    if (controlName.startsWith(fieldPrefix)) {
      const lang = controlName.substring(fieldPrefix.length);
      const value = this.form.get(controlName)?.value;
      if (value && value.trim() !== '') {
        result[lang] = value;
      }
    }
  });
  return result;
}

onSubmit(): void {
  const nameValues = this.collectLocalizedValues('name_');
  const product = {
    ...this.form.value,
    name: nameValues
  };
  // Save product...
}
```

### 4. Ensure Mandatory Languages are Required

Add validators for mandatory languages:

```typescript
setupMandatoryLanguages(): void {
  const mandatoryLangs = this.sessionService.availableLanguages()
    .filter(l => l.mandatory)
    .map(l => l.isoKey);
  
  mandatoryLangs.forEach(lang => {
    const controlName = `name_${lang}`;
    const control = this.form.get(controlName);
    if (control) {
      control.setValidators([Validators.required]);
      control.updateValueAndValidity();
    }
  });
}
```

## Integration with Session Service

The component depends on `SessionService` to get language configuration:

```typescript
sessionService = inject(SessionService);

mandatoryLanguages = computed(() => {
  return this.sessionService.availableLanguages()
    .filter(l => l.mandatory)
    .map(l => l.isoKey);
});
```

Ensure your `SessionService` provides an `availableLanguages()` signal that returns language objects with `isoKey`, `active`, and `mandatory` properties.

## Common Issues and Solutions

### Issue: Form controls not updating when language is added

**Solution**: Ensure you're calling `form.addControl()` in the `addLanguage()` handler:

```typescript
addLanguage(lang: string): void {
  const controlName = `${this.fieldPrefix}${lang}`;
  this.form.addControl(controlName, this.fb.control(''));
  this.configuredLanguages.update(langs => [...langs, lang]);
}
```

### Issue: Inactive languages are editable

**Solution**: The component automatically disables inactive language controls. Ensure your `SessionService` correctly identifies inactive languages.

### Issue: Recently added language disappears when empty

**Solution**: The component tracks recently added languages in `recentlyAddedLanguages` signal. This is handled automatically but requires that the language is added through the `addLanguage()` event.

## Related Components

- [`localized-stringfield-view`](../../../src/app/components/localized-stringfield-view/) - For displaying localized fields in read-only mode
- Do NOT use `localized-name-input` (deprecated/unused)
