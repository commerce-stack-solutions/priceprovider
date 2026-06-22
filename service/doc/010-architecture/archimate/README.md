# ArchiMate Documentation

This directory contains the ArchiMate model for the Price Provider project, providing a structured architectural overview across multiple layers.

## ArchiMate Model File

- **File**: `price-provider.archimate`
- **Format**: Archi tool native XML format (compatible with [Archi](https://www.archimatetool.com/)).

## Architectural Analysis

### 1. Motivation Layer
The project addresses significant "pains" in enterprise pricing management:
- **Pains**: Handling gigantic amounts of price updates and long-running import times in legacy monoliths.
- **Drivers**: The need for high-performance data ingestion and low-latency price retrieval.
- **Gains**: A dedicated, microservice-based pricing solution optimized for high-traffic and rapid updates.

### 2. Strategy Layer
- **Capabilities**: **Manage Pricing Strategy** - specifically focusing on the agility to update and publish prices faster to react to market dynamics.
- **Resources**: **Agentic Engineering** - leveraging AI-assisted development and operation to maintain and extend the system efficiently.
- **Strategic Direction**: Serving as a template for customers to migrate pricing functionality out of monoliths into a modern microservice architecture.

### 3. Business Layer
- **Business Services**: **Price Management API** - provides the primary value proposition to internal and external consumers.
- **Business Processes**: **Update and Publish Prices** - the core workflow ensuring price consistency and availability.

### 4. Application Layer
- **Software Developed**:
  - **Price Provider Service**: A Java 21 / Spring Boot 3.x backend. It implements a layered architecture (Controller, Facade, Service, Data Access) and follows Interface Driven Design (IDD).
  - **Price Manager App**: An Angular-based management interface for administrative tasks.
- **Integrated Applications**:
  - **Identity Provider (Keycloak)**: Manages OIDC-based authentication and authorization across the stack.

### 5. Technology Layer
- **Software Stack**:
  - **Backend**: Java, Spring Boot, Spring Data JPA, Hibernate.
  - **Frontend**: Angular, TypeScript.
  - **Database**: PostgreSQL for persistent storage.
  - **Infrastructure**: Docker for containerization and Kubernetes for orchestration.
  - **Ingress**: Nginx Ingress Controller for routing and host-based access.
- **Deployment**: The solution is provided as a "basic extendable template" for cloud-native environments, including Helm charts and Kubernetes manifests.

## Software Solution Analysis

The Price Provider is designed as a **Specialized Microservice**. Unlike general-purpose e-commerce platforms, it focuses intensely on the **Pricing Domain**.

- **Performance-First**: By separating pricing from the main product monolith, it allows for independent scaling and optimization of database schemas specifically for pricing lookups and bulk imports.
- **Extensibility**: Using "Dynamic Enums" and IDD, the service is built to be a template that developers can adapt to complex pricing rules (e.g., contract pricing, promotion-based logic) without re-architecting the core.
- **Cloud-Native by Design**: The inclusion of K8s manifests, health probes, and standard OIDC integration makes it ready for modern DevOps pipelines.
