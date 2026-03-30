#!/bin/bash
set -euo pipefail

# Always execute from this script directory so relative build contexts are stable
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ============================================================
#  Configuration – adjust to your environment
# ============================================================
RESOURCE_GROUP="pps-rg"
CONTAINER_APP_ENV="pps-container-env"
ACR_NAME="ppscr"
SERVICE_APP="price-provider-service"

# ============================================================
#  SECURITY NOTE
#  Credentials passed as command-line arguments may be visible
#  in process listings and saved in shell history.
#  For production use, consider sourcing credentials from
#  environment variables or a key vault instead.
#  ACR authentication via admin credentials is sufficient for
#  development; for production use managed identity instead:
#  https://learn.microsoft.com/azure/container-apps/managed-identity-image-pull
# ============================================================

# ============================================================
#  Check required parameters
# ============================================================
if [ -z "${1:-}" ] || [ -z "${2:-}" ] || [ -z "${3:-}" ] || [ -z "${4:-}" ]; then
    echo "Usage: 04-create-container-app-service.sh <version> <db_jdbcurl> <db_username> <db_password>"
    echo "Example: 04-create-container-app-service.sh 1.2.3 \\"
    echo "    \"jdbc:postgresql://pps-postgres.postgres.database.azure.com:5432/priceProviderService\" \\"
    echo "    ppsadmin MySecureP@ssw0rd"
    echo ""
    echo "Prerequisites:"
    echo "  - Resource group and Container Apps environment created (01-create-resource-group)"
    echo "  - Container registry created and admin enabled (03-create-container-registry)"
    echo "  - Azure CLI logged in and ACR build permissions available"
    exit 1
fi

VERSION="$1"
DB_JDBCURL="$2"
DB_USERNAME="$3"
DB_PASSWORD="$4"

# Ensure service image exists in ACR (build on demand)
SERVICE_IMAGE="${SERVICE_APP}:${VERSION}"
SERVICE_BUILD_CONTEXT="../../service"

echo "Checking ACR image tag \"${SERVICE_IMAGE}\"..."
if ! az acr repository show --name "${ACR_NAME}" --image "${SERVICE_IMAGE}" >/dev/null 2>&1; then
    echo "Image not found. Building and pushing with az acr build..."
    az acr build --registry "${ACR_NAME}" --image "${SERVICE_IMAGE}" "${SERVICE_BUILD_CONTEXT}"
else
    echo "Image already exists in ACR."
fi

# ============================================================
#  Retrieve ACR admin credentials
# ============================================================
echo "Retrieving ACR credentials..."
ACR_USERNAME=$(az acr credential show --name "${ACR_NAME}" --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name "${ACR_NAME}" --query "passwords[0].value" -o tsv)

# ============================================================
#  Create Container App for price-provider-service
#  DB credentials are stored as Container App secrets
# ============================================================
echo "Creating Container App \"${SERVICE_APP}\"..."
echo "  Image  : ${ACR_NAME}.azurecr.io/${SERVICE_APP}:${VERSION}"
az containerapp create \
    --name "${SERVICE_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --environment "${CONTAINER_APP_ENV}" \
    --image "${ACR_NAME}.azurecr.io/${SERVICE_APP}:${VERSION}" \
    --target-port 8080 \
    --ingress external \
    --registry-server "${ACR_NAME}.azurecr.io" \
    --registry-username "${ACR_USERNAME}" \
    --registry-password "${ACR_PASSWORD}" \
    --secrets "pps-db-jdbcurl=${DB_JDBCURL}" "pps-db-username=${DB_USERNAME}" "pps-db-password=${DB_PASSWORD}" \
    --env-vars "PPS_DB_JDBCURL=secretref:pps-db-jdbcurl" "PPS_DB_USERNAME=secretref:pps-db-username" "PPS_DB_PASSWORD=secretref:pps-db-password" "PPS_DB_DRIVER=org.postgresql.Driver" "PPS_JPA_DB_PLATFORM=org.hibernate.dialect.PostgreSQLDialect" \
    --min-replicas 0 \
    --max-replicas 1

echo ""
echo "Container App \"${SERVICE_APP}\" created successfully."
echo "  Image: ${ACR_NAME}.azurecr.io/${SERVICE_APP}:${VERSION}"
echo ""
echo "Retrieve the app URL with:"
echo "  az containerapp show --name ${SERVICE_APP} --resource-group ${RESOURCE_GROUP} --query properties.configuration.ingress.fqdn -o tsv"
