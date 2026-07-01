# ADR 001: Use Transloco for Internationalization

## Status
Accepted

## Context
The Price Manager application needs to support multiple languages (19 languages as defined in `service/src/main/resources/initialize/essential/languages.json`). Users must be able to switch languages at runtime without requiring a page reload or separate builds per language.

### Requirements
- Runtime language switching (single build for all languages)
- Support for 19 languages: de, en, es, fr, pt, nl, da, sv, no, fi, zh, ja, sl, cs, pl, hr, et, lv, lt
- Feature-scoped translation files to keep the codebase organized
- Ability to avoid duplication with shared translation keys
- Compatible with Angular 22+ and zoneless change detection
- Compatible with standalone components architecture
- TypeScript type safety for translation keys
- Small bundle size and good performance

## Decision Drivers
- **Developer Experience**: Easy to integrate, maintain, and use
- **Performance**: Minimal runtime overhead and efficient bundle size
- **Compatibility**: Works with Angular 22+, standalone components, and zoneless change detection
- **Community Support**: Active maintenance and good documentation
- **Features**: Runtime language switching, lazy loading, type safety

## Options Considered

### 1. @jsverse/transloco (formerly @ngneat/transloco)
**Pros:**
- Built specifically for Angular with modern architecture in mind
- Excellent support for standalone components and Angular 22+
- Runtime language switching out of the box (single build)
- Lazy loading of translation files by feature/scope
- TypeScript support with type-safe translation keys
- Small bundle size (~2KB gzipped core)
- Active development and well-maintained (moved to @jsverse organization)
- Rich plugin ecosystem (transloco-locale, transloco-persist-lang, etc.)
- Multiple storage strategies (in-memory, localStorage, etc.)
- Declarative API with structural directives and pipes
- Good performance with zoneless change detection
- Excellent documentation and examples

**Cons:**
- Smaller community compared to ngx-translate (but growing rapidly)
- Relatively newer (2019) compared to ngx-translate

**Evaluation:**
- **Trustworthiness**: High - Maintained by well-known Angular community members, moved to @jsverse organization showing long-term commitment
- **GitHub**: ~2.6k stars, regular releases, active issue resolution
- **NPM**: ~100k+ weekly downloads, up-to-date packages
- **License**: MIT (permissive)

### 2. @angular/localize
**Pros:**
- Official Angular internationalization solution
- Full ICU message format support
- AOT compilation of translations

**Cons:**
- **Requires separate builds per language** - Does NOT meet our requirement for runtime switching
- More complex setup and workflow
- Larger bundle sizes (includes all translation at compile time)
- Less flexible for dynamic content

**Verdict**: Does not meet runtime language switching requirement.

### 3. @ngx-translate/core
**Pros:**
- Very popular and widely used (established in 2016)
- Large community and extensive third-party resources
- Runtime language switching support

**Cons:**
- Not actively maintained (last major update was years ago)
- No official support for Angular 16+ standalone components
- Not optimized for modern Angular (signals, zoneless)
- Larger bundle size compared to Transloco
- Less TypeScript type safety
- Community has largely moved to Transloco for new projects

**Evaluation:**
- **Trustworthiness**: Medium - While historically popular, maintenance has slowed significantly
- Development appears stalled with minimal updates

### 4. i18next + angular-i18next
**Pros:**
- Framework-agnostic (can be used in non-Angular projects)
- Very mature and feature-rich
- Large ecosystem of plugins

**Cons:**
- Not designed specifically for Angular
- More boilerplate required for Angular integration
- Larger bundle size
- Less idiomatic Angular usage
- Overkill for our use case

## Decision
We will use **@jsverse/transloco** for the following reasons:

1. **Runtime Language Switching**: Meets our core requirement for single build with runtime language switching
2. **Modern Angular Compatibility**: Excellent support for Angular 22+, standalone components, and zoneless change detection
3. **Performance**: Small bundle size and efficient runtime performance
4. **Developer Experience**: 
   - Clean, declarative API with pipes and structural directives
   - TypeScript type safety for translation keys
   - Easy scoping and lazy loading of translations
5. **Active Maintenance**: Regular updates and responsive to Angular ecosystem changes
6. **Best Practice Alignment**: Recommended by Angular experts and widely adopted for modern Angular applications
7. **Feature Set**: Rich features including lazy loading, plugins, multiple storage strategies, and scope support
8. **Community Momentum**: Growing adoption, especially for new Angular projects

## Implementation Guidelines

### File Structure
```
app/src/assets/i18n/
├── en/
│   ├── common.json          # Shared keys: actions, fields, statuses
│   ├── pages.json           # Page-level translations
│   ├── components.json      # Component-level translations
│   └── validation.json      # Validation messages
├── de/
│   ├── common.json
│   ├── pages.json
│   ├── components.json
│   └── validation.json
└── [other languages following same structure]
```

### Translation Key Organization
- `common.*` - Shared translations (actions, fields, statuses, labels)
  - `common.actions.*` - Button labels (save, cancel, delete, edit, add, etc.)
  - `common.fields.*` - Common field names (name, description, active, etc.)
  - `common.statuses.*` - Status labels
- `pages.*` - Page-specific translations
  - `pages.languages.*` - Language management page
  - `pages.currencies.*` - Currency management page
  - etc.
- `components.*` - Component-specific translations
  - `components.sidebar.*` - Sidebar navigation
  - `components.pagination.*` - Pagination controls
  - etc.
- `validation.*` - Validation error messages

### Configuration
- Default language: English (en)
- Fallback language: English (en)
- Available languages: All 19 languages from languages.json
- Lazy loading: Enabled for better performance
- Production mode: Pre-load default language, lazy load others

## Consequences

### Positive
- Single build serves all languages, reducing deployment complexity
- Users can switch languages instantly without page reload
- Maintainable translation files organized by feature
- Type-safe translations reduce runtime errors
- Small bundle size impact
- Modern Angular best practices

### Negative
- Team needs to learn Transloco API (minimal learning curve)
- Translation files need to be maintained for all 19 languages
- Initial setup effort required

### Neutral
- Moving away from hardcoded strings requires refactoring existing components
- Translation completeness needs to be monitored (can use Transloco's built-in tooling)

## References
- [Transloco Documentation](https://jsverse.github.io/transloco/)
- [Transloco GitHub Repository](https://github.com/jsverse/transloco)
- [Angular i18n Official Guide](https://angular.dev/guide/i18n)
- [Transloco vs ngx-translate comparison](https://github.com/jsverse/transloco/blob/master/docs/docs/comparison.mdx)
