# App Documentation

This directory contains the comprehensive documentation for the Price Provider App (Angular frontend).

## Structure

```
app/doc/
├── README.md                          ← You are here
├── 010-architecture/                  Architecture and ADRs
├── 020-development/                   Development guides and security
└── 040-components/                    Reusable UI component documentation
```

## Table of Contents

### Architecture

- [ADR 001: Transloco for Internationalization](010-architecture/001-adr-transloco.md) - Decisions regarding i18n implementation.

### Development

- [Development Guide](020-development/010-development-guide.md) - Comprehensive development patterns and best practices for Angular development.
- [Security Implementation Guide](020-development/020-security-implementation-guide.md) - OIDC, permissions, and organization context in the Angular app.
- [Internationalization Guide](020-development/030-i18n-guide.md) - How to handle translations and multi-language support.

### Component Documentation

- [Localized String Field Edit Component](040-components/localized-stringfield-edit.md) - Multi-language field editing component.
- [Reference Edit Component](040-components/reference-edit.md) - Autocomplete reference field component.
- [Reference List Edit Component](040-components/referencelist-edit.md) - Multiple reference selection component.

## Quick Links

### Development Patterns

- **Component Development**: See [Development Guide - Component Development Patterns](020-development/010-development-guide.md#component-development-patterns)
- **Signals and State Management**: See [Development Guide - Signals and Reactive State](020-development/010-development-guide.md#signals-and-reactive-state)
- **Form Handling**: See [Development Guide - Form Handling](020-development/010-development-guide.md#form-handling)
- **REST API Communication**: See [Development Guide - REST API Communication](020-development/010-development-guide.md#rest-api-communication)
- **Error Handling**: See [Development Guide - Error Handling](020-development/010-development-guide.md#error-handling)
- **Routing and Navigation**: See [Development Guide - Routing and Navigation](020-development/010-development-guide.md#routing-and-navigation)
