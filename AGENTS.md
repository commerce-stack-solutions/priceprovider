# AGENTS.md
This repository contains two sub-projects:

- subfolder `service/` – the priceprovider service is a Java / Spring Boot backend using Gradle, see [AGENTS.md](service/AGENTS.md) for project specific architecture, conventions, and development guidelines
- subfolder `app/` – the pricemanager app is an Angular frontend using Node.js and Bootstrap, see [AGENTS.md](app/AGENTS.md) for project specific architecture, conventions, and development guidelines

Each project follows modern best practices and is structured for scalability, maintainability, and developer productivity.

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
