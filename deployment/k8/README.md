# Kubernetes Helm Charts & Argo CD Setup (`deployment/k8`)

This directory contains the professional Kubernetes setup for Price Provider applications and infrastructure using **Helm** and **Argo CD** (GitOps).

The setup is split into:

- **application charts** for deployable Price Provider workloads
- **infrastructure charts** for optional local supporting services
- a **local-dev umbrella chart** that wires the charts together
- **Argo CD applications** that separate always-on app deployment from opt-in infrastructure deployment

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
│   └── local-dev/                    # Umbrella chart, shared Gateway, and local-dev values
├── argocd/                           # Argo CD GitOps Application manifests
│   ├── app-of-apps.yaml              # Root App-of-Apps manifest for the application layer
│   ├── local-dev-infrastructure.yaml # Optional Argo CD Application for Postgres & Keycloak
│   └── local-dev-applications.yaml   # Argo CD Application for Gateway + Service + App
├── setup-helm.sh                     # Bash helper script for local deployment & dependency update
└── setup-helm.bat                    # Windows CMD helper script for local deployment
```

---

## Architecture Overview

### Layer responsibilities

| Layer | Location | Responsibility |
| --- | --- | --- |
| Application charts | `deployment/k8/charts/` | Deploy the Price Provider service and frontend app |
| Infrastructure charts | `deployment/k8/infrastructure/` | Deploy local PostgreSQL and Keycloak when external services are not available |
| Environment chart | `deployment/k8/environments/local-dev/` | Combines dependencies, shared Gateway config, and environment-specific values |
| GitOps manifests | `deployment/k8/argocd/` | Defines Argo CD Applications for app layer and optional infrastructure layer |

### Routing model

This setup uses **Gateway API** instead of Kubernetes `Ingress`:

- the **Gateway** is defined once at the environment level in `environments/local-dev/templates/gateway.yaml`
- each routable chart owns only its own **HTTPRoute**
- the umbrella environment values connect the routes to the shared Gateway through `httpRoute.parentRefs`

This keeps cross-cutting ingress/gateway ownership out of individual application charts while still letting each chart describe its own hostname and path matching.

---

## Prerequisites

- **Kubernetes cluster** (Docker Desktop, Minikube, K3s, or remote cluster).
- **`kubectl`** CLI tool.
- **`helm`** v3 CLI tool.
- **Gateway API CRDs** installed on the cluster.
- A **Gateway API-compatible controller** with a `GatewayClass` configured in `environments/local-dev/values.yaml` (`gateway.className`). Replace the default placeholder (`replace-me`) before deploying.

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

## Helm Chart Reference

### `charts/priceprovider-service`

Backend Spring Boot application chart.

**Rendered resources**
- `Deployment`
- `Service`
- `HorizontalPodAutoscaler` when autoscaling is enabled
- `HTTPRoute` when `httpRoute.enabled=true`

**Main values**
- `image.*`: backend image repository, tag, and pull policy
- `service.*`: service exposure and target container port (`8080`)
- `httpRoute.*`: Gateway API hostname/path routing
- `env.*`: datasource, OIDC, CORS, and data initialization settings
- `resources.*`, `autoscaling.*`, `readinessProbe`, `livenessProbe`

### `charts/priceprovider-app`

Frontend Angular management UI chart.

**Rendered resources**
- `Deployment`
- `Service`
- `HorizontalPodAutoscaler` when autoscaling is enabled
- `HTTPRoute` when `httpRoute.enabled=true`

**Main values**
- `image.*`: frontend image repository, tag, and pull policy
- `service.*`: service exposure and target container port (`80`)
- `httpRoute.*`: Gateway API hostname/path routing
- `env.*`: backend base URL and OIDC browser settings
- `resources.*`, `autoscaling.*`, `readinessProbe`, `livenessProbe`

### `infrastructure/postgres`

Optional PostgreSQL chart used mainly for local or isolated environments.

**Rendered resources**
- `Deployment`
- `Service`
- `PersistentVolumeClaim` when persistence is enabled

**Main values**
- `image.*`: PostgreSQL image configuration
- `service.*`: service exposure and target port (`5432`)
- `persistence.*`: PVC toggle, storage class, access mode, and size
- `env.*`: database name, username, and password
- `resources.*`

### `infrastructure/keycloak`

Optional Keycloak chart used mainly for local or isolated environments.

**Rendered resources**
- `ConfigMap`
- `Deployment`
- `Service`
- `HTTPRoute` when `httpRoute.enabled=true`

**Main values**
- `image.*`: Keycloak image configuration
- `service.*`: service exposure and target container port (`8080`)
- `httpRoute.*`: Gateway API hostname/path routing
- `env.*`: admin bootstrap credentials
- `args`: runtime arguments such as `start-dev` and realm import
- `resources.*`, `readinessProbe`

### `environments/local-dev`

Umbrella chart that assembles the local developer environment.

**Responsibilities**
- declares chart dependencies for app and infrastructure charts
- creates the shared `Gateway`
- provides environment-specific defaults for hostnames, routes, service wiring, and bootstrap configuration
- allows selective enabling/disabling of infrastructure and application components

**Key values**
- `gateway.*`: shared Gateway name, class, and listeners
- `postgres.enabled`: toggles bundled PostgreSQL
- `keycloak.enabled`: toggles bundled Keycloak
- `priceprovider-service.enabled`: toggles backend deployment
- `priceprovider-app.enabled`: toggles frontend deployment

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

### Useful targeted variants

Applications only:
```bash
helm template local-dev-applications environments/local-dev \
  --set gateway.enabled=true \
  --set postgres.enabled=false \
  --set keycloak.enabled=false \
  --set priceprovider-service.enabled=true \
  --set priceprovider-app.enabled=true
```

Optional infrastructure only:
```bash
helm template local-dev-infrastructure environments/local-dev \
  --set gateway.enabled=false \
  --set postgres.enabled=true \
  --set keycloak.enabled=true \
  --set priceprovider-service.enabled=false \
  --set priceprovider-app.enabled=false
```

---

### Option 2: Argo CD GitOps Deployment

The GitOps setup uses an **App-of-Apps** pattern with separated application and optional infrastructure layers:

- `local-dev-applications.yaml`: Deploys the shared `Gateway` plus `priceprovider-service` and `priceprovider-app` into namespace `price-provider`.
- `local-dev-infrastructure.yaml`: Optionally deploys PostgreSQL and Keycloak into namespace `price-provider` for environments without external database or identity services.
- `app-of-apps.yaml`: Root Argo CD application that tracks the application layer only.

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

2. If the target environment also needs bundled infrastructure, apply the optional infrastructure application separately:
```bash
kubectl apply -f argocd/local-dev-infrastructure.yaml
```

3. Argo CD will continuously sync state with repository: `https://github.com/commerce-stack-solutions/priceprovider.git` on branch `master`.

### Argo CD manifest responsibilities

#### `argocd/app-of-apps.yaml`
- root application
- tracks only `local-dev-applications.yaml`
- keeps the core app layer active by default

#### `argocd/local-dev-applications.yaml`
- deploys the `local-dev` umbrella chart with:
  - shared Gateway enabled
  - `priceprovider-service` enabled
  - `priceprovider-app` enabled
  - `postgres` disabled
  - `keycloak` disabled

#### `argocd/local-dev-infrastructure.yaml`
- deploys the same umbrella chart with:
  - Gateway disabled
  - `postgres` enabled
  - `keycloak` enabled
  - application charts disabled

This split allows environments with managed database and identity services to run the application layer without forcing local infrastructure components.

---

## Validation Workflow

From `deployment/k8/environments/local-dev`:

```bash
helm dependency build
helm lint .
helm template local-dev .
```

For split validation, also render the applications-only and infrastructure-only variants shown above. This is the preferred targeted validation flow after changing Helm charts, Gateway routing, or Argo CD values in `deployment/k8`.
