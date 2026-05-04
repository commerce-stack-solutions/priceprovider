# Service Documentation

This directory contains the comprehensive documentation for the Price Provider Service backend.

## Structure

```
service/doc/
├── README.md                          ← You are here
│
├── 010-architecture/
│   ├── 010-overview.md                       Architecture overview, layer description, goals, constraints
│   ├── 020-architectural-decisions.md        ADR index and purpose
│   └── 021-adr001.md                         ADR-001: Database Selection and Scalability
│
├── 020-development/
│   ├── 010-development-guide.md              General development guide (overview, links to layers & setup)
│   ├── 011-development-guide-data-access-layer.md   Repositories, JPA Entities, Data Initialization
│   ├── 012-development-guide-service-layer.md       Business Logic, Transactions, Validation, IDD
│   ├── 013-development-guide-facade-layer.md        DTO Mapping, $expand, PatchMapper, IDD Facade
│   ├── 014-development-guide-controller-layer.md    REST Endpoints, OpenAPI, Exception Handling
│   ├── 020-security.md                       Implemented security concepts
│   ├── 030-testing.md                        Test strategies and examples
│   └── 040-postman.md                        Postman Collection Guide and Newman CLI
│
├── 030-features/
│   ├── 010-query-filtering-usage.md          Query filtering user guide (Lucene-like syntax)
│   ├── 020-query-filtering-implementation.md Query filtering implementation guide
│   ├── 030-bulk-update-pricerows.md          Bulk create/update of price rows
│   ├── 040-public-price-api/
│   │   ├── 010-integration-guide.md          Public Price API - Integration guide for consumers
│   │   └── 020-developer-guide.md            Public Price API - Developer/architecture guide
│   ├── 050-channels-countries/
│   │   ├── 010-business-guide.md             Channel-country consistency business rules
│   │   └── 020-developer-guide.md            Implementation and validation architecture
│   ├── 050-rbac-and-user-guide.md            RBAC model, sample users, and organization concept
│   ├── 060-meta-annotation-concept.md        $meta expand – entity metadata API
│   ├── 070-service-initialization.md         Service initialization and bootstrap mechanism
│   ├── 080-reference-key-and-id-generation.md  @ReferenceKey, @GeneratedId, and exchangeable ID generation
│   ├── 090-permission-selectors.md           Permission Selectors - Technical Implementation Guide
│   └── 091-permission-selectors-user-guide.md  Permission Selectors - Business User Guide
│
└── 040-api-reference/
    ├── 010-general-concept.md                Typical REST call patterns, GET/PUT/PATCH/POST/DELETE
    └── 020-error-codes.md                    Error response format, HTTP status codes, error examples
```

## Table of Contents

### Architecture

- [Architecture Overview](010-architecture/010-overview.md) - Goals, functional features, quality goals, constraints, layer diagram
- [Architectural Decision Records](010-architecture/020-architectural-decisions.md) - ADR index and purpose
- [ADR-001: Database Selection](010-architecture/021-adr001.md) - H2 for development, PostgreSQL / Cloud Spanner for production

### Development

- [Development Guide](020-development/010-development-guide.md) - Technology stack, project structure, entry point to all layer guides
- [Data Access Layer](020-development/011-development-guide-data-access-layer.md) - JPA Entities, Repositories, Specifications, Data Initialization
- [Service Layer](020-development/012-development-guide-service-layer.md) - Business Logic, IDD, Service Validation
- [Facade Layer](020-development/013-development-guide-facade-layer.md) - DTO Mapping, $expand, PatchMapper, IDD Facade
- [Controller Layer](020-development/014-development-guide-controller-layer.md) - REST Endpoints, OpenAPI, Exception Handling, PATCH examples
- [Security Overview](020-development/020-security.md) - OIDC and RBAC architecture overview
- [Security Implementation Guide](020-development/021-security-implementation-guide.md) - Technical deep dive into service security
- [Testing](020-development/030-testing.md) - Unit, integration, and controller test strategies with examples
- [Postman & Newman](020-development/040-postman.md) - Postman collection usage and Newman CLI automation

### Features

- [Query Filtering - Usage](030-features/010-query-filtering-usage.md) - How to use the Lucene-like q parameter
- [Query Filtering - Implementation](030-features/020-query-filtering-implementation.md) - How to add query filtering to new entities
- [Bulk Update of Price Rows](030-features/030-bulk-update-pricerows.md) - Smart field-matching bulk create/update
- [Public Price API - Integration Guide](030-features/040-public-price-api/010-integration-guide.md) - API for external consumers (shop, PIM, ERP)
- [Public Price API - Developer Guide](030-features/040-public-price-api/020-developer-guide.md) - Architecture, strategies, extensibility
- [Channel-Country Consistency - Business Guide](030-features/050-channels-countries/010-business-guide.md) - Business rules for channel-country validation
- [Channel-Country Consistency - Developer Guide](030-features/050-channels-countries/020-developer-guide.md) - Implementation and validation architecture
- [RBAC and User Guide](030-features/050-rbac-and-user-guide.md) - RBAC model, sample users, and organization concept
- [Meta Annotation Concept](030-features/060-meta-annotation-concept.md) - `$meta` expand, `referenceKeyFields`, `@GeneratedId`
- [Service Initialization](030-features/070-service-initialization.md) - Service initialization and bootstrap mechanism
- [Reference Key and ID Generation](030-features/080-reference-key-and-id-generation.md) - `@ReferenceKey`, `@GeneratedId`, and exchangeable UUID generation
- **[Permission Selectors - Technical Guide](030-features/090-permission-selectors.md)** - Implementation, architecture, and developer guide for permission selectors
- **[Permission Selectors - Business User Guide](030-features/091-permission-selectors-user-guide.md)** - Non-technical guide for creating and managing permission-based access control

### API Reference

- [General Concepts](040-api-reference/010-general-concept.md) - Typical REST call patterns, how GET/PUT/PATCH/POST/DELETE work
- [Error Codes](040-api-reference/020-error-codes.md) - Error response format, HTTP status codes, error examples

## Quick Links

| Task | Document |
|------|----------|
| Developer setup (build, run) | [service/README.md](../README.md) |
| Understanding the architecture | [Architecture Overview](010-architecture/010-overview.md) |
| Adding a new entity | [Development Guide](020-development/010-development-guide.md) |
| Implementing a service | [Service Layer Guide](020-development/012-development-guide-service-layer.md) |
| Implementing a mapper | [Facade Layer Guide](020-development/013-development-guide-facade-layer.md) |
| Adding a REST endpoint | [Controller Layer Guide](020-development/014-development-guide-controller-layer.md) |
| Understanding REST error handling | [Error Codes](040-api-reference/020-error-codes.md) |
| Using the query filter API | [Query Filtering Usage](030-features/010-query-filtering-usage.md) |
| Adding query filtering to new entity | [Query Filtering Implementation](030-features/020-query-filtering-implementation.md) |
| Using the public price API | [Public Price API - Integration Guide](030-features/040-public-price-api/010-integration-guide.md) |
| Initial service setup and bootstrap | [Service Initialization](030-features/070-service-initialization.md) |
| Understanding `$meta`, `@ReferenceKey`, `@GeneratedId` | [Reference Key and ID Generation](030-features/080-reference-key-and-id-generation.md) |
| **Creating permission-based access control** | **[Permission Selectors - Business User Guide](030-features/091-permission-selectors-user-guide.md)** |
| **Implementing permission selectors for new entities** | **[Permission Selectors - Technical Guide](030-features/090-permission-selectors.md)** |
| Testing the API with Postman | [Postman Collection Guide](020-development/040-postman.md) |
