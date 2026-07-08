#!/bin/bash
set -euo pipefail

# Always execute from this script directory so relative paths are stable
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ============================================================
#  Configuration – adjust to your environment
# ============================================================
RESOURCE_GROUP="pps-rg"
LOCATION="westeurope"
CONTAINER_APP_ENV="pps-container-env"

# ============================================================
#  Create resource group
# ============================================================
echo "Creating resource group \"${RESOURCE_GROUP}\" in \"${LOCATION}\"..."
az group create --name "${RESOURCE_GROUP}" --location "${LOCATION}"

# ============================================================
#  Create Container Apps environment
# ============================================================
echo "Creating Container Apps environment \"${CONTAINER_APP_ENV}\"..."
az containerapp env create \
    --name "${CONTAINER_APP_ENV}" \
    --resource-group "${RESOURCE_GROUP}" \
    --location "${LOCATION}"

echo ""
echo "Resource group and Container Apps environment created successfully."
echo "  Resource Group : ${RESOURCE_GROUP}"
echo "  Location       : ${LOCATION}"
echo "  CA Environment : ${CONTAINER_APP_ENV}"
