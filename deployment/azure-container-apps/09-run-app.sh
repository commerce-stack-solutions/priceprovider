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
APP_APP="price-manager-app"

# ============================================================
#  Check required parameter: version (three digits, e.g. 1.2.3)
# ============================================================
if [ -z "${1:-}" ]; then
    echo "Usage: 09-run-app.sh <version>"
    echo "Example: 09-run-app.sh 1.2.3"
    echo ""
    echo "Prerequisites:"
    echo "  - App image must be built and pushed to ACR (07-create-app-image)"
    echo "  - Container App must already exist (05-create-container-app-app)"
    exit 1
fi
VERSION="$1"

# ============================================================
#  Update Container App to the specified image version
# ============================================================
echo "Updating Container App \"${APP_APP}\" to image version \"${VERSION}\"..."
az containerapp update \
    --name "${APP_APP}" \
    --resource-group "${RESOURCE_GROUP}" \
    --image "${ACR_NAME}.azurecr.io/${APP_APP}:${VERSION}"

echo ""
echo "Container App \"${APP_APP}\" updated to version \"${VERSION}\" successfully."
echo ""
echo "Retrieve the app URL with:"
echo "  az containerapp show --name ${APP_APP} --resource-group ${RESOURCE_GROUP} --query properties.configuration.ingress.fqdn -o tsv"
