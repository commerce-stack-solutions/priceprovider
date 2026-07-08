# Internationalization (i18n) Guide

## Overview

The Price Manager application supports multiple languages using [Transloco](https://jsverse.github.io/transloco/), a powerful internationalization library for Angular applications. This guide explains how to use translations throughout the application.

## Supported Languages

The application supports 19 languages as defined in `service/src/main/resources/initialize/essential/languages.json`:

- **de** - German (Deutsch)
- **en** - English (default)
- **es** - Spanish (Español)
- **fr** - French (Français)
- **pt** - Portuguese (Português)
- **nl** - Dutch (Nederlands)
- **da** - Danish (Dansk)
- **sv** - Swedish (Svenska)
- **no** - Norwegian (Norsk)
- **fi** - Finnish (Suomi)
- **zh** - Chinese (中文)
- **ja** - Japanese (日本語)
- **sl** - Slovenian (Slovenščina)
- **cs** - Czech (Čeština)
- **pl** - Polish (Polski)
- **hr** - Croatian (Hrvatski)
- **et** - Estonian (Eesti)
- **lv** - Latvian (Latviešu)
- **lt** - Lithuanian (Lietuvių)

## Translation Files Structure

Translation files are organized in `src/assets/i18n/` with the following structure:

```
src/assets/i18n/
├── en/
│   ├── common.json          # Shared translations (actions, fields, statuses)
│   ├── pages.json           # Page-specific translations
│   └── components.json      # Component-specific translations
├── de/
│   ├── common.json
│   ├── pages.json
│   └── components.json
└── [other languages with same structure]
```

### Translation Key Organization

Translation keys follow a hierarchical structure to avoid duplication and maintain consistency:

#### common.json - Shared Translations
- `common.actions.*` - Action labels (save, cancel, delete, edit, add, etc.)
- `common.fields.*` - Common field names (name, description, active, etc.)
- `common.statuses.*` - Status labels (loading, success, error, etc.)
- `common.messages.*` - Common messages (confirmations, errors, etc.)
- `common.pagination.*` - Pagination controls
- `common.navigation.*` - Navigation elements

#### pages.json - Page-Specific Translations
- `pages.home.*` - Home page translations
- `pages.languages.*` - Language management page
- `pages.currencies.*` - Currency management page
- `pages.units.*` - Unit management page
- `pages.taxclasses.*` - Tax class management page
- `pages.groups.*` - Group management page
- `pages.organizations.*` - Organization management page
- `pages.pricerows.*` - Price row management page

#### components.json - Component-Specific Translations
- `components.sidebar.*` - Sidebar navigation
- `components.languageSwitcher.*` - Language switcher component
- `components.pagination.*` - Pagination component

## Usage in Components

### 1. Import TranslocoModule

```typescript
import { Component } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-example',
  templateUrl: './example.component.html',
  imports: [TranslocoModule]
})
export class ExampleComponent {
}
```

### 2. Use Translation Pipe in Templates

The `transloco` pipe is the primary way to use translations in templates:

```html
<!-- Simple translation -->
<h1>{{ 'pages.example.title' | transloco }}</h1>

<!-- Button with action label -->
<button>{{ 'common.actions.save' | transloco }}</button>

<!-- Field label -->
<label>{{ 'common.fields.name' | transloco }}</label>
```

### 3. Translations with Parameters

For dynamic content, use parameters:

```html
<!-- Translation with parameter -->
<p>{{ 'pages.languages.deleteConfirm' | transloco: { count: selectedLanguages().size } }}</p>
```

The corresponding translation key in the JSON file:
```json
{
  "pages": {
    "languages": {
      "deleteConfirm": "Delete {{count}} selected language(s)?"
    }
  }
}
```

### 4. Using TranslocoService Programmatically

For translations in TypeScript code, inject the `TranslocoService`:

```typescript
import { Component, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-example',
  templateUrl: './example.component.html'
})
export class ExampleComponent {
  private translocoService = inject(TranslocoService);

  showMessage() {
    const message = this.translocoService.translate('common.messages.saveSuccess');
    alert(message);
  }

  // With parameters
  showDeleteConfirm(count: number) {
    const message = this.translocoService.translate(
      'pages.languages.deleteConfirm',
      { count }
    );
    return confirm(message);
  }
}
```

### 5. Structural Directive (Alternative)

For conditional rendering based on translations:

```html
<ng-container *transloco="let t">
  <h1>{{ t('pages.example.title') }}</h1>
  <p>{{ t('pages.example.description') }}</p>
</ng-container>
```

## Language Switching

### Current Implementation

Language selection is managed through the existing `SessionService` which:
1. Loads available languages from the backend API
2. Stores user's language preference in `localStorage`
3. Automatically syncs with Transloco when language changes

The header component provides a session configuration panel where users can select their preferred language.

### Accessing Current Language

```typescript
import { inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

export class ExampleComponent {
  private translocoService = inject(TranslocoService);

  getCurrentLanguage() {
    return this.translocoService.getActiveLang();
  }

  getAvailableLanguages() {
    return this.translocoService.getAvailableLangs();
  }
}
```

## Best Practices

### 1. Use Appropriate Namespaces
- Use `common.*` for reusable translations across the app
- Use `pages.*` for page-specific content
- Use `components.*` for component-specific content

### 2. Consistent Naming
- Use camelCase for keys (e.g., `addLanguage`, not `add_language`)
- Be descriptive but concise
- Group related keys together

### 3. Avoid Hardcoded Strings
- All user-facing text should come from translation files
- This includes error messages, labels, buttons, and placeholders

### 4. Translation Keys in Code
```typescript
// Good - clear and maintainable
'common.actions.save'
'pages.languages.title'
'components.sidebar.currencies'

// Bad - unclear or too long
'save'
'language_management_page_title'
'sidebarCurrenciesNavigationLinkText'
```

### 5. Reuse Common Translations
Instead of duplicating "Save", "Cancel", etc. in every page's translations, use the shared keys:

```html
<!-- Good -->
<button>{{ 'common.actions.save' | transloco }}</button>
<button>{{ 'common.actions.cancel' | transloco }}</button>

<!-- Bad - don't duplicate -->
<button>{{ 'pages.example.save' | transloco }}</button>
<button>{{ 'pages.example.cancel' | transloco }}</button>
```

### 6. Lazy Loading (Future Enhancement)
For large applications, consider using Transloco's scope feature to lazy load translations by feature module.

## Adding New Translations

### Step 1: Add to English Translation File
First, add the key to the appropriate English JSON file (`en/common.json`, `en/pages.json`, or `en/components.json`):

```json
{
  "pages": {
    "newPage": {
      "title": "New Page Title",
      "description": "This is a new page"
    }
  }
}
```

### Step 2: Add to Other Language Files
Add the corresponding translations to other language files (at minimum `de/` for German):

```json
{
  "pages": {
    "newPage": {
      "title": "Neuer Seitentitel",
      "description": "Dies ist eine neue Seite"
    }
  }
}
```

### Step 3: Use in Templates
```html
<h1>{{ 'pages.newPage.title' | transloco }}</h1>
<p>{{ 'pages.newPage.description' | transloco }}</p>
```

## Testing Translations

### Manual Testing
1. Start the development server: `npm start`
2. Open the application in a browser
3. Click the gear icon in the header
4. Select a different language from the dropdown
5. Click "Apply"
6. Verify that all text updates to the selected language

### Missing Translation Keys
Transloco will show the key name if a translation is missing. For example, if `pages.example.title` is not found, it will display `pages.example.title` in the UI.

To debug missing translations:
1. Check browser console for Transloco warnings
2. Verify the key exists in the translation file
3. Ensure the translation file is properly formatted JSON
4. Clear browser cache if needed

## Troubleshooting

### Translations Not Loading
- Verify translation files exist in `src/assets/i18n/[lang]/`
- Check browser network tab for 404 errors on JSON files
- Ensure `provideHttpClient()` is in `app.config.ts`

### Language Not Switching
- Check `SessionService` is properly setting the language
- Verify `TranslocoService.setActiveLang()` is being called
- Check localStorage for `app-language` value

### Build Errors
- Ensure all JSON files are valid (no trailing commas, proper quotes)
- Verify TranslocoModule is imported in components using translations

## References

- [Transloco Official Documentation](https://jsverse.github.io/transloco/)
- [Transloco GitHub Repository](https://github.com/jsverse/transloco)
- [ADR 001: Use Transloco for Internationalization](../010-architecture/001-adr-transloco.md)
