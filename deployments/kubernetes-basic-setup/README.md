# Kubernetes Basic Setup for Price Provider

This folder contains Kubernetes manifests and scripts to set up the Price Provider applications and services.

## Overview

The setup includes the following components:

- **Namespace**: `price-provider` - Isolates all resources.
- **PostgreSQL**: Internal database for the backend service.
- **Keycloak**: Identity provider for authentication (publicly available).
- **Backend Service**: Java-based service handling price data (publicly available, auto-scaled).
- **Frontend App**: Angular-based management application (publicly available, auto-scaled).

## Files

- `namespace.yaml`: Creates the `price-provider` namespace.
- `postgres.yaml`: Sets up a PersistentVolumeClaim, a Deployment, and a ClusterIP Service for PostgreSQL.
- `keycloak-config.yaml`: A ConfigMap containing the realm export data to initialize Keycloak.
- `keycloak.yaml`: Sets up Keycloak with a LoadBalancer Service.
- `service.yaml`: Sets up the backend service with a LoadBalancer Service and HorizontalPodAutoscaler.
- `app.yaml`: Sets up the frontend application with a LoadBalancer Service and HorizontalPodAutoscaler.
- `setup.sh`: Bash script to apply all manifests and wait for resources.
- `setup.bat`: Windows batch script to apply all manifests.

## Prerequisites

- `kubectl` configured to point to your Kubernetes cluster.
- A Kubernetes cluster that supports `LoadBalancer` services (e.g., Minikube with `minikube tunnel`, GKE, EKS, AKS).
- Metrics Server installed in the cluster (required for HorizontalPodAutoscaler).

## How to Run

### Using Bash (Linux/macOS)
```bash
./setup.sh
```

### Using Batch (Windows)
```cmd
setup.bat
```

## Accessing the Applications

After running the setup, you can find the external IPs for the services by running:
```bash
kubectl get svc -n price-provider
```

- **Frontend App**: Accessible on port 80 of the `app` service IP.
- **Keycloak**: Accessible on port 8080 of the `keycloak` service IP.
- **Backend Service**: Accessible on port 8080 of the `service` service IP.

## Notes on OIDC Configuration

In this basic setup, the OIDC issuer URI is set to `http://keycloak:8080/realms/priceprovider`. For a production-ready or external-access environment, you might need to:
1. Update the `PPS_OIDC_ISSUER_URI` and other OIDC-related environment variables in `service.yaml` and `app.yaml` to use the actual public DNS or IP of the Keycloak LoadBalancer.
2. Update the `redirectUris` and `webOrigins` in the Keycloak realm configuration (via the UI or by updating `keycloak-config.yaml` before deployment).

## Auto-scaling

Both the `service` and `app` deployments are configured with a `HorizontalPodAutoscaler` that targets 50% CPU utilization. They will scale between 1 and 5 (for service) or 3 (for app) replicas based on load.
