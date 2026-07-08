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
APP_APP="price-manager-app"

# ============================================================
#  SECURITY NOTE
#  ACR admin credentials are used for simplicity. For production
#  environments, consider using managed identity instead:
#  https://learn.microsoft.com/azure/container-apps/managed-identity-image-pull
# ============================================================

# ============================================================
#  Check required parameters
# ============================================================
if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
    echo "Usage: 05-create-container-app-app.sh <version> <pps_baseurl>"
    echo "Example: 05-create-container-app-app.sh 1.2.3 \\"
    echo "    https://price-provider-service.nicefield-abc123.westeurope.azurecontainerapps.io/"
    echo ""
    echo "Prerequisites:"
    echo "  - Resource group and Container Apps environment created (01-create-resource-group)"
    echo "  - Container registry created and admin enabled (03-create-container-registry)"
    echo "  - Azure CLI logged in and ACR build permissions available"
    echo "  - Service container app URL known (04-create-container-app-service)"
    exit 1
fi

VERSION="$1"
PPS_BASEURL="$2"

# Ensure app image exists in ACR (build on demand)
APP_IMAGE="${APP_APP}:${VERSION}"
APP_BUILD_CONTEXT="../../app"

echo "Checking ACR image tag \"${APP_IMAGE}\"..."
if ! az acr repository show --name "${ACR_NAME}" --image "${APP_IMAGE}" >/dev/null 2>&1; then
    echo "Image not found. Building and pushing with az acr build..."
    az acr build --registry "${ACR_NAME}" --image "${APP_IMAGE}" "${APP_BUILD_CONTEXT}"
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
#  Create Container App for price-manager-app
# ============================================================
echo "Creating Container App \"${APP_APP}\"..."
echo "  Image      : ${ACR_NAME}.azurecr.io/${APP_APP}:${VERSION}"
echo "  PPS_BASEURL: ${PPS_BASEURL}"
az containerapp create \
    --name "${APP_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --environment "${CONTAINER_APP_ENV}" \
    --image "${ACR_NAME}.azurecr.io/${APP_APP}:${VERSION}" \
    --target-port 80 \
    --ingress external \
    --registry-server "${ACR_NAME}.azurecr.io" \
    --registry-username "${ACR_USERNAME}" \
    --registry-password "${ACR_PASSWORD}" \
    --env-vars "PPS_BASEURL=${PPS_BASEURL}" \
    --min-replicas 0 \
    --max-replicas 1

echo ""
echo "Container App \"${APP_APP}\" created successfully."
echo "  Image: ${ACR_NAME}.azurecr.io/${APP_APP}:${VERSION}"
echo ""
echo "Retrieve the app URL with:"
echo "  az containerapp show --name ${APP_APP} --resource-group ${RESOURCE_GROUP} --query properties.configuration.ingress.fqdn -o tsv"
