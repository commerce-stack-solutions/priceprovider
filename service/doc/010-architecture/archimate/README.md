# ArchiMate Documentation

This directory contains the ArchiMate model for the Price Provider project, providing a structured architectural overview across multiple layers.

## ArchiMate Model File

- **File**: `price-provider.archimate`
- **Format**: Archi tool native XML format (compatible with [Archi](https://www.archimatetool.com/)).

> **Note**: To ensure maximum compatibility with the Archi tool across different versions and platforms, the `.archimate` file contains the structural model and relationships. Detailed textual descriptions are maintained in this `README.md`.

## Architectural Analysis

### 1. Motivation Layer
The project addresses significant "pains" in enterprise pricing management:
- **Large Volume of Price Updates**: Handling gigantic amounts of price updates (hundreds of thousands or millions) that can overwhelm traditional ERP/monolith systems.
- **Long Import Times**: Legacy systems often suffer from long-running batch processes; this project aims for high-performance ingestion.
- **Monolith to Microservice Migration**: Customers can use this software as a strategic component to decouple pricing logic from their legacy monoliths.
- **Gains**: A dedicated, optimized pricing solution that scales independently and provides low-latency retrieval.

### 2. Strategy Layer
- **Manage Pricing Strategy**: The core capability to update and publish prices faster, allowing businesses to react almost instantly to market changes and competitor pricing.
- **Agentic Engineering**: A strategic resource where AI-assisted development (like the use of Jules/AI agents) is leveraged for rapid feature implementation and robust architectural design.

### 3. Business Layer
- **Price Management API**: The primary business service exposed to consumers (web shops, mobile apps, kiosks).
- **Update and Publish Prices**: The critical business process that ensures price updates are validated, stored, and made available to the API.

### 4. Application Layer
- **Price Provider Service**: The backend engine (Java 21 / Spring Boot 3.x) that encapsulates the pricing domain logic, persistence, and REST API.
- **Price Manager App**: The administrative Angular application for managing pricing rules and data.
- **Identity Provider (Keycloak)**: A shared application component providing OIDC-compliant security and user management.
- **Price Retrieval Service**: An internal application service specifically tuned for high-speed read access.

### 5. Technology Layer
- **Kubernetes Cluster**: The primary deployment target for the cloud-native basic template.
- **PostgreSQL**: The relational database used for structured pricing data storage.
- **Docker**: The containerization technology used for building portable service images.
- **Nginx Ingress Controller**: Handles host-based routing and traffic management for the cluster.
- **Price Provider Docker Image**: The primary technical artifact delivered by the build process.

## Software Solution Analysis

The Price Provider is designed as a **Specialized Microservice**. Unlike general-purpose e-commerce platforms, it focuses intensely on the **Pricing Domain**.

- **Performance-First**: By separating pricing from the main product monolith, it allows for independent scaling and optimization of database schemas specifically for pricing lookups and bulk imports.
- **Extensibility**: Using "Dynamic Enums" and Interface Driven Design (IDD), the service is built to be a template that developers can adapt to complex pricing rules (e.g., contract pricing, promotion-based logic) without re-architecting the core.
- **Cloud-Native by Design**: The inclusion of K8s manifests, health probes, and standard OIDC integration makes it ready for modern DevOps pipelines.
