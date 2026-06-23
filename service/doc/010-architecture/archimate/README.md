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

### 6. Strategy & Motivation View
Traces the business drivers through goals and requirements to strategic capabilities.

```mermaid
graph TD
    Volume[Large Volume of Price Updates] -- Association --> Goal[Optimized Import Performance]
    Goal -- Realization --> Perf[Performance < 100ms]
    Goal -- Realization --> Scale[Scalability 5000 req/sec]
    Cloud[Cloud-Native Principle] -- Influence --> Avail[Availability 99.9%]
    Tool[Customer Managed Vendor Tool] -- Association --> Flow[Price Import Flow]
    Cap[Manage Pricing Strategy] -- Realization --> Weekly[Weekly Price Update]
    Res[Agentic Engineering] -- Association --> Cap
```

### 7. Observability View
Shows the monitoring and logging infrastructure.

```mermaid
graph LR
    Service[Price Provider Service] -- Association --> Prometheus[Prometheus]
    Prometheus -- Association --> Grafana[Grafana]
    Service -- Association --> OTEL[OpenTelemetry]
    OTEL -- Association --> Loki[Loki]
```

### 8. Data Model View
Details the core entities and their relationships within the Price Provider service.

```mermaid
classDiagram
    class PriceRow {
        String id
        String pricedResourceId
        BigDecimal priceValue
        BigDecimal minQuantity
        DateTime validFrom
        DateTime validTo
        Boolean taxIncluded
    }
    class Unit {
        String symbol
        Map name
        String measure
        BigDecimal factor
    }
    class Currency {
        String currencyKey
        String symbol
        Map name
    }
    class TaxClass {
        String taxClassId
        BigDecimal taxRate
    }
    class Country {
        String isoKey
        Map name
    }
    class Channel {
        String id
        Enum representationMode
    }
    class Group {
        String id
        String path
        String name
    }
    class Organization {
        Enum organizationType
    }

    PriceRow --> Unit : unitRef
    PriceRow --> Currency : currencyRef
    PriceRow --> TaxClass : taxClassRef
    PriceRow "n" -- "m" Group : groupRefs
    PriceRow "n" -- "m" Channel : channelRefs
    TaxClass --> Country : countryRef
    Channel "n" -- "m" Country : allowedCountryRefs
    Country --> Currency : primaryCurrencyRef
    Organization --|> Group : extends
    Group "1" -- "*" Group : parentRefs/subRefs
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
The service operates on a specialized data model optimized for high-performance pricing lookups.

**Core Entities**:
- **PriceRow**: The central transactional record. It decouples pricing from products, allowing for complex rules based on quantity, time, and customer context.
- **Master Data**: `Unit`, `Currency`, and `Country` provide the normalization required for multi-national operations.
- **Contextual Pricing**: `Channel` and `Group` (with `Organization` as a specialization) allow for targeted pricing strategies (e.g., store-specific, B2B-segment-specific).

### 4. Application Layer & Integration
- **Observability**: Prometheus, Grafana, Loki, and OpenTelemetry provide full stack monitoring.
- **Integration**:
    - **Admin API**: Consumed by the **Customer Pricing Tool** (vendor-managed) for bulk updates.
    - **Public API**: Consumed by the **Shop System** and the **Instore Kiosk**.

### 5. Technology Layer
The Kubernetes-based deployment architecture ensures that the Price Provider remains highly available and scalable. Each component (API, WebApp, DB, Keycloak) runs in isolated Pods with appropriate resource limits and health probes.
