# Development Guide

This guide provides an overview of the development patterns, conventions, and tooling for the services platform. For the full architectural layering concept, see [Architecture Overview](../010-architecture/010-overview.md). For developer setup (build, run, environment), see [README.md](../../README.md).

## Technology Stack

- Java 25
- Spring Boot 4.x
- Gradle 9.x
- Spring Web, Spring Data JPA
- H2 (development/test), PostgreSQL (production)
- RESTful API design
- SpringDoc OpenAPI 2.8.x (OpenAPI 3.0 spec, Swagger UI available at `/swagger-ui.html`)

## Module Concept

The codebase is split into **platform modules** and **service applications**. Understanding this distinction is essential before making any change.

### Platform Modules (`services/platform/`)

Platform modules are reusable Spring Boot auto-configured library JARs. They are consumed by one or more service applications via Gradle dependencies. Each platform module has its own `build.gradle` and `gradlew`.

| Module | Root Package | Description |
|--------|-------------|-------------|
| `commons` | `io.commercestacksolutions.commons` | Shared utilities, base interfaces, exception types, query engine |
| `coreserviceapp` | `io.commercestacksolutions.coreserviceapp` | `AppRole`, `AppPermission` — RABAC base module |
| `corebusinessentities` | `io.commercestacksolutions.corebusinessentities` | `Channel`, `Country`, `Currency`, `Group`, `Language`, `Organization`, `TaxClass`, `Unit` |
| `cdf-plugin` | `io.commercestacksolutions.codegen` | Gradle build plugin for CDF code generation |

**Key rules for platform modules:**
- Platform modules must **not** import from application-specific packages (e.g. `priceproviderservice.*`)
- Cross-module integration uses shared interfaces/contracts from `commons`
- Platform modules declare only the minimum viable set of fields — consuming applications can extend via the CDF plugin

### Service Applications (`services/applications/`)

Service applications are deployable Spring Boot services. They depend on one or more platform modules and add application-specific domain logic.

| Application | Root Package | Description |
|-------------|-------------|-------------|
| `priceprovider` | `io.commercestacksolutions.priceproviderservice` | Price management service: `PriceRow`, public price API |

**Key rules for service applications:**
- Must declare `@EntityScan` explicitly if any `@EntityScan` annotation is present, covering all platform module `dataaccess` packages
- Application-specific business entities and rules go here, not in platform modules
- Integration tests (`@SpringBootTest`, `@WebMvcTest`) live here, not in platform modules

## Project Structure

```
services/
├── platform/
│   ├── commons/
│   │   ├── src/main/java/io/commercestacksolutions/commons/
│   │   ├── build.gradle
│   │   └── gradlew
│   ├── coreserviceapp/
│   │   ├── src/main/java/io/commercestacksolutions/coreserviceapp/
│   │   ├── build.gradle
│   │   └── settings.gradle
│   ├── corebusinessentities/
│   │   ├── src/main/java/io/commercestacksolutions/corebusinessentities/
│   │   ├── build.gradle
│   │   └── settings.gradle
│   └── cdf-plugin/
│       ├── src/main/java/io/commercestacksolutions/codegen/
│       ├── build.gradle
│       └── settings.gradle
└── applications/
    └── priceprovider/
        ├── src/main/java/io/commercestacksolutions/priceproviderservice/
        │   ├── dataaccess/
        │   ├── facade/
        │   ├── service/
        │   └── web/controller/
        ├── src/main/resources/application.yaml
        ├── postman/pps-postmancollection.json
        ├── build.gradle
        └── gradlew
```

## Where to Put New Code

| What you are building | Where it lives |
|-----------------------|---------------|
| New core reference entity (reusable across services) | `corebusinessentities` |
| New security / RBAC concept | `coreserviceapp` or `commons` |
| Shared utility / base class | `commons` |
| Price-specific entity or business rule | `priceprovider` |
| Application configuration / security wiring | `priceprovider` |
| Integration test requiring `@SpringBootTest` | `priceprovider` test sources |
| Pure unit test for platform code | Platform module test sources |

## Layer-Specific Development Guides

Each layer has its own focused guide with patterns, examples, and best practices:

| Guide | Layer | Key Topics |
|-------|-------|------------|
| [011 – Data Access Layer](011-development-guide-data-access-layer.md) | `dataaccess` | Repositories, JPA Entities, Specifications, Data Initialization |
| [012 – Service Layer](012-development-guide-service-layer.md) | `service` | Business Logic, Validation, IDD |
| [013 – Facade Layer](013-development-guide-facade-layer.md) | `facade` | DTO Mapping, Expansion, Context, PatchMapper |
| [014 – Controller Layer](014-development-guide-controller-layer.md) | `web/controller` | REST Endpoints, OpenAPI, Exception Handling |

## Interface Driven Design (IDD)

All layers follow Interface Driven Design principles:

- **Services**: `{Entity}Service` interface → `{Entity}ServiceImpl` implementation
- **Facades**: `{Entity}FacadeService` interface → `{Entity}FacadeImpl` implementation
- **Inject interfaces**, never concrete implementations

For detailed IDD guidance, see [Service Layer Guide](012-development-guide-service-layer.md) and [Facade Layer Guide](013-development-guide-facade-layer.md).

## Exception Handling

All exceptions are checked, propagated through the call stack, and handled centrally by `ExceptionHandlerAdvice`. See [Controller Layer Guide](014-development-guide-controller-layer.md#exception-handling) for details.

## Testing

See [030-testing.md](030-testing.md) for test strategies and examples.

## Postman & Newman

See [040-postman.md](040-postman.md) for Postman collection usage and Newman CLI automation.
