# AGENTS.md
This repository contains two sub-projects:

- subfolder `services/applications/priceprovider/` – the priceprovider service is a Java / Spring Boot backend using Gradle, see [AGENTS.md](services/applications/priceprovider/AGENTS.md) for project specific architecture, conventions, and development guidelines
- subfolder `app/` – the pricemanager app is an Angular frontend using Node.js and Bootstrap, see [AGENTS.md](app/AGENTS.md) for project specific architecture, conventions, and development guidelines

Each project follows modern best practices and is structured for scalability, maintainability, and developer productivity.

## Module and Application Overview

This repository is organized into a clean, decoupled architecture separating the frontend client application from backend service and reusable platform modules:

### 1. Frontend Client Application
- **`app/`** (Price Manager App): A standalone, modern web application built with Angular 22, TypeScript 6, and Bootstrap/Tailwind CSS. Consumes the backend API to provide a comprehensive management interface for pricing operations. Contains an end-to-end test suite implemented with Playwright following the Page Object Model (POM) pattern.

### 2. Backend Modules & Service Applications (`services/`)
Structured as a multi-module platform-to-application design to promote code reusability, modularity, and future support for multiple separated services:

- **Platform Layer (`platform/`)**: Reusable components, plugins, and libraries that form the foundational stack for all application services:
  - **`platform/cdf-plugin/`**: A custom Gradle plugin providing Common Definition Format (CDF) code generation. Generates JPA entities and schemas from build-time definition files.
  - **`platform/commons/`**: Shared core library containing foundational utility classes, exception definitions, abstract mapper base classes, and request/response interceptors.
  - **`platform/corebusinessentities/`**: Encapsulates core business master data (e.g., `Unit`, `Currency`, `TaxClass`, `Group`, `Organization`, and `Language`) including their persistence mappings, business validations, mapper classes, and REST endpoint controllers.
  - **`platform/coreserviceapp/`**: Standard security module implementing application-level authentication, authorization (roles & permissions config), and endpoint security.

- **Application Layer (`applications/`)**: Deployable service applications that bundle reusable platform capabilities with specific business logic:
  - **`applications/priceprovider/`**: A fully functional, standalone Spring Boot backend service. It implements specific pricing domain models (like `PriceRowEntity`), handles bulk create/update operations, secures endpoints, and exposes administrative and public pricing APIs. Dependending on `platform/` modules, it compiles and runs as an independent microservice.

## AI Agent Skills

To ensure consistency and quality in complex tasks, several specialized "skills" have been defined for AI agents. These are located in the `.github/skills/` directory and should be consulted when performing relevant tasks:

- [**Entity Creation & Update**](.github/skills/entity-creation-update/SKILL.md): Comprehensive guide for implementing new domain entities or updating existing ones across all layers (persistence, service, facade, REST API, and frontend).
- [**Query Filtering**](.github/skills/query-filtering/SKILL.md): Adding Lucene-like query filtering support to entity endpoints in the backend service.
- [**Bulk Operations**](.github/skills/bulk-operations/SKILL.md): Implementing bulk create-or-update operations with smart field matching or natural key matching.
- [**Security & RBAC**](.github/skills/security-rbac/SKILL.md): Implementing role-based access control, JWT authentication, and organization-scoped data access.
- [**Angular Components**](.github/skills/angular-components/SKILL.md): Developing modern Angular components using signals, standalone components, and reusable UI patterns.
- [**Postman Collection Tester**](.github/skills/postman-collection-tester/SKILL.md): Instructions for running and maintaining integration tests using Newman and the project's Postman collection.
- [**Translation**](.github/skills/translation/SKILL.md): Guidelines and tasks for managing multi-language support in both the backend and frontend.

## Contribution Guidelines

- Do not invent, refactor, or optimize beyond the scope of your task.
- Always work with the existing codebase and reuse established patterns.
- Follow project-specific conventions and examples as documented.
- Consistency and alignment with the defined architecture take priority over personal preferences.
