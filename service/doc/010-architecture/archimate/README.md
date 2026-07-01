# ArchiMate Documentation

This directory contains the ArchiMate model for the Price Provider project, providing a structured architectural overview across multiple layers.

## ArchiMate Model File

- **File**: `price-provider.archimate`
- **Format**: Archi tool native XML format (compatible with [Archi](https://www.archimatetool.com/)).

## Architectural Views

The ArchiMate model includes the following architectural views. Mermaid diagrams are provided below as a visual reference for each.

### 1. Business Process View
Focuses on how stakeholders interact with pricing processes and services.

### 2. Application Cooperation View
Shows how the different application components interact to provide the pricing solution.

### 3. Import Processing View
Details the flow of data from external pricing tools into the system.


### 4. Deployment View
Models the cloud-native infrastructure setup in Kubernetes.

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
