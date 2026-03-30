# Azure Container Apps – Deployment Guide (BETA)
This folder contains Azure CLI scripts to build, set up, and run the **Price Provider Service** stack on [Azure Container Apps](https://learn.microsoft.com/azure/container-apps/).

**Warning:** this is a work in progress and may contain inaccuracies and untested scripts. It is just a starter script collection to get started setting up the Price Provider Service stack on Azure Container Apps. Use with caution.

The stack consists of:

| Component | Azure resource |
|-----------|---------------|
| PostgreSQL database | Azure Database for PostgreSQL Flexible Server |
| Backend API (`price-provider-service`) | Azure Container App |
| Frontend (`price-manager-app`) | Azure Container App |
| Docker image registry | Azure Container Registry (ACR) |

---

## Prerequisites

| Tool | Minimum version | Install guide |
|------|----------------|---------------|
| [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) | 2.57+ | `az version` |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | 24+ | `docker --version` |

Log in to Azure and select your subscription before running any script:

```bash
az login
az account set --subscription "<your-subscription-id>"
```

---

## Script overview

| # | Script | Purpose | Parameters |
|---|--------|---------|-----------|
| 01 | `01-create-resource-group` | Create resource group + Container Apps environment | – |
| 02 | `02-create-postgres-db` | Create PostgreSQL Flexible Server (Burstable B1ms, cheapest) | `<db_admin_password>` |
| 03 | `03-create-container-registry` | Create Azure Container Registry (Basic SKU) | – |
| 04 | `04-create-container-app-service` | Build service image on demand (if needed) + create container app for the backend | `<version> <db_jdbcurl> <db_username> <db_password>` |
| 05 | `05-create-container-app-app` | Build app image on demand (if needed) + create container app for the frontend | `<version> <pps_baseurl>` |
| 06 | `06-create-service-image` | Build backend Docker image locally and push to ACR | `<version>` |
| 07 | `07-create-app-image` | Build frontend Docker image locally and push to ACR | `<version>` |
| 08 | `08-run-service` | Update the running backend container app to a new image version | `<version>` |
| 09 | `09-run-app` | Update the running frontend container app to a new image version | `<version>` |

Each script comes as a `.bat` (Windows) and a `.sh` (Linux / macOS) pair. All examples below use the `.sh` form; replace `.sh` with `.bat` on Windows.

> **Version number format**: all scripts expect a three-digit semver version such as `1.2.3`.

---

## Configuration

Default resource names are defined at the top of every script. Change them once if you need different names:

| Variable | Default value | Description |
|----------|--------------|-------------|
| `RESOURCE_GROUP` | `pps-rg` | Azure resource group |
| `LOCATION` | `westeurope` | Azure region |
| `CONTAINER_APP_ENV` | `pps-container-env` | Container Apps managed environment |
| `ACR_NAME` | `ppscr` | Container Registry name (must be globally unique) |
| `DB_SERVER_NAME` | `pps-postgres` | PostgreSQL server name (must be globally unique) |
| `DB_NAME` | `priceProviderService` | PostgreSQL database name |
| `DB_ADMIN_USER` | `ppsadmin` | PostgreSQL admin username |
| `SERVICE_APP` | `price-provider-service` | Backend container app name |
| `APP_APP` | `price-manager-app` | Frontend container app name |

---

## Initial setup (run once)

### Step 1 – Create resource group and Container Apps environment

```bash
./01-create-resource-group.sh
```

### Step 2 – Create the PostgreSQL database

```bash
./02-create-postgres-db.sh "MySecureP@ssw0rd"
```

After the server is created, note the JDBC URL printed at the end – you will need it in step 5:

```
jdbc:postgresql://pps-postgres.postgres.database.azure.com:5432/priceProviderService
```

Password requirements: 8–128 characters, must include uppercase, lowercase, digits, and a special character.

### Step 3 – Create the Container Registry

```bash
./03-create-container-registry.sh
```

---

## Build and push Docker images

Scripts 06 and 07 build a Docker image locally (requires Docker Desktop) and push it to ACR. Alternatively, scripts 04 and 05 include an **on-demand image build** step using `az acr build` — if the image tag does not yet exist in ACR, the source is uploaded and built directly in the cloud without needing Docker locally.

### Option A – Build locally then push (scripts 06/07)

Both image scripts resolve the source directories automatically (`../../service/` and `../../app/` relative to this folder), so they can be run from any working directory.

#### Build and push the backend image

```bash
./06-create-service-image.sh 1.2.3
```

#### Build and push the frontend image

```bash
./07-create-app-image.sh 1.2.3
```

### Option B – Build in ACR on demand (automatic in scripts 04/05)

When running scripts 04 or 05, if the image for the given version is not already present in ACR, the script automatically triggers `az acr build` to build and push the image from the source directory. No local Docker installation required.

---

## Create the container apps (run once)

### Step 4 – Create the backend container app

DB credentials are stored as **Container App secrets** and never appear as plain environment variables. If the image for `<version>` is not yet in ACR, the script automatically builds it via `az acr build`.

```bash
./04-create-container-app-service.sh 1.2.3 \
  "jdbc:postgresql://pps-postgres.postgres.database.azure.com:5432/priceProviderService" \
  ppsadmin \
  "MySecureP@ssw0rd"
```

Retrieve the backend URL after creation:

```bash
az containerapp show \
  --name price-provider-service \
  --resource-group pps-rg \
  --query properties.configuration.ingress.fqdn -o tsv
```

The URL will look like:  
`https://price-provider-service.<random-hash>.westeurope.azurecontainerapps.io`

### Step 5 – Create the frontend container app

Pass the backend URL (from step 4) as `PPS_BASEURL`. Make sure it ends with a trailing slash `/`. If the image for `<version>` is not yet in ACR, the script automatically builds it via `az acr build`.

```bash
./05-create-container-app-app.sh 1.2.3 \
  "https://price-provider-service.<random-hash>.westeurope.azurecontainerapps.io/"
```

Retrieve the frontend URL:

```bash
az containerapp show \
  --name price-manager-app \
  --resource-group pps-rg \
  --query properties.configuration.ingress.fqdn -o tsv
```

---

## Deploying a new version

After the container apps exist, build new images (either locally or via `az acr build`) and then update the running apps:

```bash
# Option A – build locally
./06-create-service-image.sh 1.3.0
./07-create-app-image.sh 1.3.0

# Deploy new image to running container apps
./08-run-service.sh 1.3.0
./09-run-app.sh 1.3.0
```

> **Tip**: if you don't need a local Docker build, skip scripts 06/07 and let scripts 08/09 pull the image tag you previously pushed, or use `az acr build` manually before running 08/09.

---

## Complete workflow summary

```
# ── One-time infrastructure ────────────────────────────────────────────
01-create-resource-group.sh
02-create-postgres-db.sh        <db_password>
03-create-container-registry.sh

# ── First deployment ───────────────────────────────────────────────────
# Option A: build images locally first, then create apps
06-create-service-image.sh      <version>
07-create-app-image.sh          <version>
04-create-container-app-service.sh  <version> <jdbcurl> <db_user> <db_pw>
05-create-container-app-app.sh      <version> <pps_baseurl>

# Option B: create apps directly; 04/05 build images on demand via az acr build
04-create-container-app-service.sh  <version> <jdbcurl> <db_user> <db_pw>
05-create-container-app-app.sh      <version> <pps_baseurl>

# ── Subsequent deployments (new version) ───────────────────────────────
06-create-service-image.sh      <new-version>   # or az acr build manually
07-create-app-image.sh          <new-version>   # or az acr build manually
08-run-service.sh               <new-version>
09-run-app.sh                   <new-version>
```

---

## Environment variables reference

| Variable | Used in | Description |
|----------|---------|-------------|
| `PPS_DB_JDBCURL` | backend | JDBC connection URL for PostgreSQL |
| `PPS_DB_USERNAME` | backend | PostgreSQL username (stored as a secret) |
| `PPS_DB_PASSWORD` | backend | PostgreSQL password (stored as a secret) |
| `PPS_DB_DRIVER` | backend | JDBC driver class (`org.postgresql.Driver`) |
| `PPS_JPA_DB_PLATFORM` | backend | Hibernate dialect (`org.hibernate.dialect.PostgreSQLDialect`) |
| `PPS_BASEURL` | frontend | Base URL of the backend API (must end with `/`) |

---

## Security notes

- DB credentials are stored as **Container App secrets** and referenced via `secretref:` – they are never stored as plain environment variables.
- ACR admin credentials are used for registry authentication. For production environments, consider using a **managed identity** instead: [Managed identity image pull](https://learn.microsoft.com/azure/container-apps/managed-identity-image-pull).
- Avoid passing passwords as command-line arguments in production; they may appear in shell history and process listings. Consider reading them from a file or Azure Key Vault.
