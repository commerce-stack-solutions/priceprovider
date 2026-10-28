# ADR-003: Module and Extension Mechanisms

| Field     | Value                                       |
|-----------|---------------------------------------------|
| Status    | Accepted                                    |
| Date      | 2026-07                                     |
| Deciders  | Platform Team                               |

## Context

The initial `priceprovider` service was a single-module Spring Boot application containing all domain entities (`AppRole`, `AppPermission`, `Language`, `Currency`, `Country`, `Channel`, `TaxClass`, `Unit`, `Group`, `Organization`, `PriceRow`). As requirements evolved toward a **platform** that can host multiple independent microservices sharing a common foundation, this monolithic layout became a limiting factor:

- Each new service would have to re-implement core business concepts from scratch (roles, permissions, common reference data)
- There was no structured way to share code between services without copy-paste or tight coupling
- Consuming applications could not selectively adopt parts of the platform

Key challenges that had to be resolved:

1. **Common base** – Several important aspects are already in place (Interface-Driven Design, Dependency Injection) but the entities themselves are still application-specific. Sharing entities across services means a common persistence model is needed.
2. **Entity extension** – Each service may have different bounded-context requirements. A pure shared entity approach would force all services to accept the same fields; but bounded contexts require their own dedicated data models. Applications must be able to extend common entities (add fields, relations) without forking the platform.
3. **Security / authentication cross-cutting concerns** – `AppRole` and `AppPermission` are used by every RABAC-secured service; they belong in the platform, not in any single application.

## Decision

### Split into Platform Modules

The backend is reorganised into the following Gradle modules under `services/platform/`:

| Module              | Package                                               | Responsibility |
|---------------------|-------------------------------------------------------|----------------|
| `commons`           | `io.commercestacksolutions.commons`                   | Shared utilities, base interfaces, exception types, query engine, permission selector infrastructure |
| `coreserviceapp`    | `io.commercestacksolutions.coreserviceapp`            | `AppRole` + `AppPermission` — full CRUD stack. Required by any RABAC-secured service application. |
| `corebusinessentities` | `io.commercestacksolutions.corebusinessentities`   | Seven core reference-data entities: `Channel`, `Country`, `Currency`, `Group`, `Language`, `Organization`, `TaxClass`, `Unit`. Depends on `coreserviceapp`. |
| `cdf-plugin`        | `io.commercestacksolutions.codegen`                   | Gradle build plugin for CDF-based code generation (see below). |

Each platform module registers itself via Spring Boot auto-configuration (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), so a consuming service just declares the Gradle dependency.

`@EnableJpaRepositories` is deliberately omitted from auto-configurations to avoid backing off the consuming application's `JpaRepositoriesAutoConfiguration`.

### Entity Extension via the CDF Build Plugin

The `corebusinessentities` module declares the **minimum viable** set of fields on each entity — enough to bootstrap a service quickly. A consuming application that needs additional fields or relations provides a **Class Definition File (CDF)** — a JSON descriptor that is merged with the base class at build time by the `cdf-plugin` Gradle plugin.

**Example CDF flow:**

```
Platform:   LanguageEntity { isoKey, active, mandatory, name }
App CDF:    { "extends": "LanguageEntity", "fields": [{ "name": "region", "type": "String" }] }
Generated:  AppLanguageEntity extends LanguageEntity { String region; ... }
```

This approach:
- Keeps generated code under source control (no runtime reflection magic)
- Gives each application a genuinely independent data model
- Preserves platform upgrade paths: re-running the generator after a platform update merges new base fields automatically

### Check on Security / Authentication Extraction

`AppRole` and `AppPermission` have been extracted into `coreserviceapp`. The authentication and authorization infrastructure (`EntityAuthorizationService`, `AuthorizationContext`, permission selectors) lives in `commons`. This covers the cross-cutting security concern for all consuming services.

Further security concerns (OAuth2 resource server configuration, JWT claims extraction, `PermissionSecurityService` bean for Spring Security `@PreAuthorize`) remain application-specific because they depend on the concrete token issuer and security scheme of each service.

## Consequences

### Benefits

- Multiple service applications can be built on the same platform foundation with minimal boilerplate
- `coreserviceapp` and `corebusinessentities` provide production-ready CRUD APIs that are auto-configured out of the box
- The CDF plugin provides a type-safe, build-time entity extension mechanism; no reflection, no Hibernate polymorphism tricks
- Platform modules are independently versioned and testable

### Trade-offs

- Developers must understand the module boundaries; contributions to shared entities need backward-compatibility consideration
- The CDF code-generation step adds build-time complexity; generated source files must be committed and kept in sync
- `@EntityScan` must be explicitly configured in the application's `@SpringBootApplication` to cover all module `dataaccess` packages when an `@EntityScan` annotation is present (Spring's default package scanning is disabled the moment any explicit `@EntityScan` is used)

## Alternatives Considered

| Alternative | Reason rejected |
|-------------|-----------------|
| Keep everything in `priceprovider` | Does not support multi-service reuse; each new service must copy-paste |
| Single fat `commons` module with all entities | Creates tight coupling; every service pulls in all entities even if not needed |
| JPA `@MappedSuperclass` inheritance for extension | Works for simple field additions but does not support relation changes or annotation merging without forking |
| Spring Data `Projections` for selective field exposure | Solves the read side only; does not help with write-model extensions |

## Related

- [Architecture Overview](010-overview.md) — Extension Architecture section
- [ADR-001](021-adr001.md) — Database Selection and Scalability
- [ADR-002](022-adr002.md) — Previous architectural decisions
- [Development Guide](../020-development/010-development-guide.md) — Platform vs application module development
