# Development Guide

This guide provides an overview of the development patterns, conventions, and tooling for the Price Provider Service. For the full architectural layering concept, see [Architecture Overview](../010-architecture/010-overview.md). For developer setup (build, run, environment), see [service/README.md](../../README.md).

## Technology Stack

- Java 17
- Spring Boot 3.x
- Gradle 8.8
- MapStruct for DTO mapping
- Spring Web, Spring Data JPA
- H2 (development/test), PostgreSQL (production)
- RESTful API design
- SpringDoc OpenAPI 3.0 (Swagger UI available at `/swagger-ui.html`)

## Project Structure

```
service/
├── src/
│   └── main/
│       ├── java/
│       │   └── de/
│       │       └── ebusyness/
│       │           ├── commons/
│       │           └── priceproviderservice/
│       │               ├── dataaccess/
│       │               ├── facade/
│       │               ├── service/
│       │               └── web/
│       │                   └── controller/
│       └── resources/
│           └── application.yaml
├── postman/pps-postmancollection.json
├── build.gradle
├── Dockerfile
└── dockerimage-create.sh / dockerimage-create.bat
```

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
