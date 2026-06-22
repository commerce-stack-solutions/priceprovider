# ArchiMate Documentation

This directory contains the ArchiMate model for the Price Provider project, providing a structured architectural overview across multiple layers.

## ArchiMate Model File

- **File**: `price-provider.archimate`
- **Format**: Archi tool native XML format (compatible with [Archi](https://www.archimatetool.com/)).

## Architectural Analysis

### 1. Motivation Layer & NFRs
The architecture is driven by explicit Non-Functional Requirements (NFRs) and Stakeholder needs:

| Area | Goal / Requirement | Description |
|------|--------------------|-------------|
| **Availability** | 99.9% | Ensured by Kubernetes orchestration and pod redundancy. |
| **Scalability** | 5000 req/sec | Horizontal scaling of the API Pods. |
| **Performance** | < 100ms | Optimized database queries and read-side caching. |
| **Security** | OAuth2 / OIDC | Identity management via Keycloak and JWT-based authorization. |
| **Reliability** | At-least-once | Guaranteed import processing via robust service logic. |
| **Auditability** | Change History | Full tracking of price changes via auditable entities. |

**Key Actors**:
- **Pricing Manager**: Responsible for weekly price updates.
- **Product Manager**: Oversees segment-specific pricing strategy.
- **Shop System**: Automated consumer of the Public Price API.
- **Administrator**: Manages the infrastructure and identity provider.

### 2. Strategy Layer
- **Weekly Price Update**: A business process initiated by Pricing Managers of respective product segments.
- **Price Import Flow**: The sequence of events where pricing data is ingested from external systems (like the Customer's Pricing Tool).
- **Agentic Engineering**: Continuous improvement and adaptation of the microservice using AI-driven engineering practices.

### 3. Business Layer & Data Model
The service divides its business interfaces into two primary domains:
- **Public Price API**: For anonymous or logged-in buyers (Webshop, Kiosk).
- **Protected Price Management API (Admin)**: For price updates and management, consumed by internal tools.

**Data Architecture**:
The core of the service is its data model, optimized for pricing:
- **PriceRow**: The central entity containing `priceValue`, `minQuantity`, and validity periods.
- **Unit & Currency**: Master data for normalizing prices.
- **TaxClass**: Defines VAT and tax rules.
- **Channel & Group**: Allows for contextual pricing (e.g., B2B vs B2C, Web vs Store).

### 4. Application Layer & Integration
- **Price Provider Service**: The Spring Boot backend.
- **Instore Kiosk (Flutter)**: A showcase application for instore price checks, integrating with the Price Provider for pricing and a PIM for product info.
- **Pricing Tool (External)**: A customer-owned tool managed by a third-party vendor that integrates with the Admin API for automated updates.

**Observability Stack**:
- **Prometheus**: Collects metrics from the service.
- **Grafana**: Visualizes performance and health dashboards.
- **Loki & OpenTelemetry**: For centralized logging and distributed tracing.

### 5. Technology & Deployment Layer
The deployment follows a cloud-native pattern in a Kubernetes environment:

**Deployment View**:
- **API Pod**: Runs the Price Provider Service.
- **WebApp Pod**: Runs the Price Manager Angular App.
- **Database Pod**: Runs PostgreSQL.
- **Keycloak Pod**: Runs the Identity Provider.

**Infrastructure**:
- **Load Balancer & Ingress**: Routes traffic from domain names (e.g., `app.priceprovider.local`).
- **Persistent Volumes**: Options for storage redundancy (Locally Redundant recommended for dev, Zone Redundant for production).
- **Network Integration**: Secure connection to the Customer's Kubernetes Cluster where the external Pricing Tool resides.

## Software Solution Analysis
The Price Provider acts as a **Specialized Pricing Hub**. By decoupling pricing from the ERP/Monolith, it provides the agility needed for modern retail:
- **Agility**: Rapidly update prices via the Admin API without full catalog deployments.
- **Scalability**: Handle high-traffic "buy" events independently of the catalog or checkout services.
- **Integrity**: Centralized pricing truth across Webshops, Mobile Apps, and Instore Kiosks.
