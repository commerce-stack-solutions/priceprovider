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
SERVICE_APP="price-provider-service"

# ============================================================
#  Check required parameter: version (three digits, e.g. 1.2.3)
# ============================================================
if [ -z "${1:-}" ]; then
    echo "Usage: 08-run-service.sh <version>"
    echo "Example: 08-run-service.sh 1.2.3"
    echo ""
    echo "Prerequisites:"
    echo "  - Service image must be built and pushed to ACR (06-create-service-image)"
    echo "  - Container App must already exist (04-create-container-app-service)"
    exit 1
fi
VERSION="$1"

# ============================================================
#  Update Container App to the specified image version
# ============================================================
echo "Updating Container App \"${SERVICE_APP}\" to image version \"${VERSION}\"..."
az containerapp update \
    --name "${SERVICE_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --image "${ACR_NAME}.azurecr.io/${SERVICE_APP}:${VERSION}"

echo ""
echo "Container App \"${SERVICE_APP}\" updated to version \"${VERSION}\" successfully."
echo ""
echo "Retrieve the app URL with:"
echo "  az containerapp show --name ${SERVICE_APP} --resource-group ${RESOURCE_GROUP} --query properties.configuration.ingress.fqdn -o tsv"
