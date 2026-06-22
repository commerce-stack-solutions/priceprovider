# ArchiMate Documentation

This directory contains the ArchiMate model for the Price Provider project, providing a structured architectural overview across multiple layers.

## ArchiMate Model File

- **File**: `price-provider.archimate`
- **Format**: Archi tool native XML format (compatible with [Archi](https://www.archimatetool.com/)).

## Architectural Views

The ArchiMate model includes the following architectural views. Mermaid diagrams are provided below as a visual reference for each.

### 1. Business Process View
Focuses on how stakeholders interact with pricing processes and services.

```mermaid
graph TD
    PM[Pricing Manager] --> WPU[Weekly Price Update Process]
    PrM[Product Manager] --> WPU
    WPU --> UP[Update and Publish Prices Process]
    UP --> AdminAPI[Protected Price Management API]
    Shop[Shop System] --> PublicAPI[Public Price API]
```

### 2. Application Cooperation View
Shows how the different application components interact to provide the pricing solution.

```mermaid
graph LR
    Kiosk[Instore Kiosk Flutter] -- Get Price --> PublicAPI[REST API Public]
    Shop[Webshop] -- Get Price --> PublicAPI
    PublicAPI --- Service[Price Provider Service]
    Manager[Price Manager App] -- Manage --> AdminAPI[REST API Admin]
    AdminAPI --- Service
    Service -- Authenticate --> Keycloak[Identity Provider Keycloak]
    Manager -- Authenticate --> Keycloak
```

### 3. Import Processing View
Details the flow of data from external pricing tools into the system.

```mermaid
graph LR
    ExtTool[Pricing Tool External] -- Push Updates --> AdminAPI[REST API Admin]
    AdminAPI --> Service[Price Provider Service]
    Service -- Write --> DB[(PostgreSQL)]
    DB --- PriceRow[PriceRow Data]
```

### 4. Deployment View
Models the cloud-native infrastructure setup in Kubernetes.

```mermaid
graph TD
    subgraph K8s[Kubernetes Cluster]
        LB[Load Balancer] --> Ingress[Nginx Ingress]
        Ingress --> ServicePod[Priceprovider API Pod]
        Ingress --> WebPod[WebApp Pod]
        Ingress --> IDPPod[Keycloak Pod]
        ServicePod --> DBPod[Database Pod]
        DBPod --- PV[Persistent Volume]
    end
```

### 5. Security View
Highlights the authentication and authorization mechanisms.

```mermaid
graph TD
    User[Pricing Manager] -- Login --> ManagerApp[Price Manager App]
    ManagerApp -- OIDC Auth --> Keycloak[Keycloak]
    Keycloak -- JWT Token --> ManagerApp
    ManagerApp -- API Call + JWT --> AdminAPI[Admin REST API]
    AdminAPI -- Validate Token --> Keycloak
    AdminAPI --> Backend[Price Provider Service]
```

## Architectural Analysis

### 1. Motivation Layer & NFRs
The architecture is driven by explicit Non-Functional Requirements (NFRs):

| Area | Goal / Requirement | Description |
|------|--------------------|-------------|
| **Availability** | 99.9% | Ensured by Kubernetes orchestration and pod redundancy. |
| **Scalability** | 5000 req/sec | Horizontal scaling of the API Pods. |
| **Performance** | < 100ms | Optimized database queries and read-side caching. |
| **Security** | OAuth2 / OIDC | Identity management via Keycloak and JWT-based authorization. |
| **Reliability** | At-least-once | Guaranteed import processing via robust service logic. |
| **Auditability** | Change History | Full tracking of price changes via auditable entities. |

### 2. Strategy Layer
- **Manage Pricing Strategy**: Agile price updates to react to market dynamics.
- **Weekly Price Update**: Business process for scheduled price adjustments by product segment.
- **Price Import Flow**: Automated ingestion from customer-owned vendor tools.

### 3. Business Layer & Data Model
- **Public Price API**: For consumers (Webshop, Kiosk).
- **Admin API**: Protected interface for management and ingestion.
- **Data Model**: Optimized entities including `PriceRow`, `TaxClass`, `Currency`, `Unit`, `Channel`, and `Group`.

### 4. Application Layer & Integration
- **Observability**: Prometheus, Grafana, Loki, and OpenTelemetry provide full stack monitoring.
- **Integration**: Seamless connection to external PIM systems and vendor-managed Pricing Tools.

### 5. Technology Layer
The deployment is a "basic extendable template" for K8s, featuring automated ingress, load balancing, and redundant storage options.
