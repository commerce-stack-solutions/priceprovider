#!/bin/bash
set -euo pipefail

# Always execute from this script directory so relative paths are stable
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ============================================================
#  Configuration – adjust to your environment
# ============================================================
RESOURCE_GROUP="pps-rg"
ACR_NAME="ppscr"

# ============================================================
#  Create Azure Container Registry (Basic SKU – cheapest)
# ============================================================
echo "Creating Azure Container Registry \"${ACR_NAME}\"..."
az acr create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${ACR_NAME}" \
    --sku Basic

# ============================================================
#  Enable admin user so container apps can authenticate
# ============================================================
echo "Enabling admin user on registry \"${ACR_NAME}\"..."
az acr update \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${ACR_NAME}" \
    --admin-enabled true

echo ""
echo "Azure Container Registry created successfully."
echo "  Registry  : ${ACR_NAME}.azurecr.io"
echo "  SKU       : Basic"
echo "  Admin user: enabled"
echo ""
echo "Retrieve registry credentials with:"
echo "  az acr credential show --name ${ACR_NAME}"
