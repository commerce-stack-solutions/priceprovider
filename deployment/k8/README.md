# Kubernetes Helm Charts & Argo CD Setup (`deployment/k8`)

This directory contains the professional Kubernetes setup for Price Provider applications and infrastructure using **Helm** and **Argo CD** (GitOps).

## Directory Structure

```text
deployment/k8/
├── charts/                           # Microservices Application Helm Charts
│   ├── priceprovider-service/        # Backend Service Helm Chart (Java / Spring Boot)
│   └── priceprovider-app/            # Frontend Management App Helm Chart (Angular)
├── infrastructure/                   # Third-Party Infrastructure Helm Charts
│   ├── postgres/                     # PostgreSQL Database Helm Chart
│   └── keycloak/                     # Keycloak Identity Provider (IAM) Helm Chart
├── environments/                     # Environment umbrella deployment configurations
│   └── local-dev/                    # Umbrella chart & local-dev values override
├── argocd/                           # Argo CD GitOps Application manifests
│   ├── app-of-apps.yaml              # Root App-of-Apps master manifest
│   ├── local-dev-infrastructure.yaml # Argo CD Application for Infrastructure (Postgres & Keycloak)
│   └── local-dev-applications.yaml   # Argo CD Application for Applications (Service & App)
├── setup-helm.sh                     # Bash helper script for local deployment & dependency update
└── setup-helm.bat                    # Windows CMD helper script for local deployment
```

---

## Detailed Helm Chart Reference

### 1. Application Charts (`deployment/k8/charts/`)

#### `priceprovider-service` (Backend Service)
- **Description**: Spring Boot REST microservice handling price management and calculation logic.
- **Port**: Container port `8080`, Service ClusterIP port `80`.
- **Configurable Values (`values.yaml`)**:
  - `replicaCount`: Number of pod replicas (default: `1`).
  - `image.repository` & `image.tag`: Image repository and tag (default: `price-provider-service:0.0.0-SNAPSHOT`).
  - `env.dbJdbcUrl`, `env.dbUsername`, `env.dbPassword`: PostgreSQL database connection credentials.
  - `env.oidcIssuerUri`, `env.jwkSetUri`, `env.oidcClientId`: OIDC/OAuth2 authentication parameters.
  - `env.corsAllowedOrigins`: Allowed CORS origins.
  - `env.initializeEssentialData`, `env.initializeSampleData`: Data initialization toggles.
  - `ingress.enabled`, `ingress.hosts`: Ingress configuration (default host: `service.priceprovider.local`).
  - `resources`: CPU and memory requests/limits.
  - `autoscaling`: Horizontal Pod Autoscaler settings (`enabled`, `minReplicas`, `maxReplicas`, `targetCPUUtilizationPercentage`).

#### `priceprovider-app` (Frontend Management UI)
- **Description**: Angular single-page application for price administration.
- **Port**: Container port `80`, Service ClusterIP port `80`.
- **Configurable Values (`values.yaml`)**:
  - `replicaCount`: Pod replicas (default: `1`).
  - `image.repository` & `image.tag`: Image details (default: `price-manager-app:0.0.0-SNAPSHOT`).
  - `env.baseUrl`: Backend service endpoint (`http://service.priceprovider.local/`).
  - `env.oidcIssuerUri`: Keycloak realm issuer URI (`http://keycloak.priceprovider.local/realms/priceprovider`).
  - `env.oidcRequireHttps`: Require HTTPS flag (`false` for local dev).
  - `ingress.enabled`, `ingress.hosts`: Ingress routing configuration (default host: `app.priceprovider.local`).
  - `resources` & `autoscaling`: Resource requests/limits and HPA settings.

---

### 2. Infrastructure Charts (`deployment/k8/infrastructure/`)

#### `postgres` (Database)
- **Description**: PostgreSQL database server for storing persistent application data.
- **Port**: Container and Service port `5432`.
- **Configurable Values (`values.yaml`)**:
  - `image.repository` & `image.tag`: `postgres:15`.
  - `persistence.enabled`: Persistent Volume Claim toggle (default `true`, size `1Gi`).
  - `env.postgresDb`, `env.postgresUser`, `env.postgresPassword`: Database initialization parameters.

#### `keycloak` (Identity & Access Management)
- **Description**: Keycloak server for authentication and authorization.
- **Port**: Container port `8080`, Service ClusterIP port `80`.
- **Configurable Values (`values.yaml`)**:
  - `image.repository` & `image.tag`: `quay.io/keycloak/keycloak:26.0.0`.
  - `args`: Arguments passed to Keycloak (`start-dev`, `--import-realm`).
  - `env.keycloakAdmin`, `env.keycloakAdminPassword`: Admin credentials.
  - Auto-mounts `realm-export.json` ConfigMap into `/opt/keycloak/data/import` for automatic realm initialization.
  - `ingress.enabled`, `ingress.hosts`: Ingress routing configuration (default host: `keycloak.priceprovider.local`).

---

### 3. Environment Umbrella Chart (`deployment/k8/environments/local-dev`)

The `local-dev` chart acts as an umbrella chart orchestrating all components using Helm subchart dependencies.

- **Dependencies (`Chart.yaml`)**:
  - `postgres` (condition: `postgres.enabled`)
  - `keycloak` (condition: `keycloak.enabled`)
  - `priceprovider-service` (condition: `priceprovider-service.enabled`)
  - `priceprovider-app` (condition: `priceprovider-app.enabled`)

- **Overrides (`values.yaml`)**:
  - Provides local development hostnames, database connection URIs, and credentials for all components in one place.

---

## Prerequisites

- **Kubernetes cluster** (Docker Desktop, Minikube, K3s, or remote cluster).
- **`kubectl`** CLI tool.
- **`helm`** v3 CLI tool.
- **NGINX Ingress Controller** enabled on the cluster.

---

## Access & Hostnames Configuration

Add the following local hosts mapping to your `/etc/hosts` (Linux/macOS) or `C:\Windows\System32\drivers\etc\hosts` (Windows):

```text
127.0.0.1 app.priceprovider.local
127.0.0.1 service.priceprovider.local
127.0.0.1 keycloak.priceprovider.local
```

### Access URLs:
- **Frontend Admin App**: [http://app.priceprovider.local](http://app.priceprovider.local)
- **Backend Service API**: [http://service.priceprovider.local](http://service.priceprovider.local)
- **Keycloak IAM**: [http://keycloak.priceprovider.local](http://keycloak.priceprovider.local)

---

## Deployment Options

### Option 1: Direct Helm Deployment (Local Testing)

To update chart dependencies and deploy the `local-dev` umbrella chart directly using Helm:

#### Linux / macOS:
```bash
./setup-helm.sh
```

#### Windows:
```cmd
setup-helm.bat
```

Or manually via Helm:
```bash
helm dependency update environments/local-dev
helm upgrade --install local-dev environments/local-dev --namespace price-provider --create-namespace
```

---

### Option 2: Argo CD GitOps Deployment

The GitOps setup uses an **App-of-Apps** pattern with separated infrastructure and application layers:

- `local-dev-infrastructure.yaml`: Deploys PostgreSQL and Keycloak into namespace `price-provider`.
- `local-dev-applications.yaml`: Deploys `priceprovider-service` and `priceprovider-app` into namespace `price-provider`.
- `app-of-apps.yaml`: Root Argo CD application orchestrating both applications.

#### Deploying via Argo CD:

1. Apply the root App-of-Apps manifest:
```bash
./setup-helm.sh argocd
```

Or manually with `kubectl`:
```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f argocd/app-of-apps.yaml
```

2. Argo CD will continuously sync state with repository: `https://github.com/commerce-stack-solutions/priceprovider.git` on branch `master`.
