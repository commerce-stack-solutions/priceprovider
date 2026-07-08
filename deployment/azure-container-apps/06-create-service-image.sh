#!/bin/bash
set -euo pipefail

# Always execute from this script directory so relative paths are stable
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ============================================================
#  Configuration – adjust to your environment
# ============================================================
ACR_NAME="ppscr"
IMAGE_NAME="price-provider-service"

# ============================================================
#  Check required parameter: version (three digits, e.g. 1.2.3)
# ============================================================
if [ -z "${1:-}" ]; then
    echo "Usage: 06-create-service-image.sh <version>"
    echo "Example: 06-create-service-image.sh 1.2.3"
    exit 1
fi
VERSION="$1"

# ============================================================
#  Resolve the service source directory
#  This script lives in deployment/azure-container-apps/
#  The service Dockerfile is in service/ (two levels up)
# ============================================================
SERVICE_DIR="${SCRIPT_DIR}/../../service"

# ============================================================
#  Log in to ACR
# ============================================================
echo "Logging in to Azure Container Registry \"${ACR_NAME}\"..."
az acr login --name "${ACR_NAME}"

# ============================================================
#  Build Docker image
# ============================================================
echo "Building Docker image ${IMAGE_NAME}:${VERSION}..."
docker build -t "${IMAGE_NAME}:${VERSION}" "${SERVICE_DIR}"

# ============================================================
#  Tag image for ACR
# ============================================================
echo "Tagging image for ACR..."
docker tag "${IMAGE_NAME}:${VERSION}" "${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${VERSION}"

# ============================================================
#  Push image to ACR
# ============================================================
echo "Pushing image to ACR..."
docker push "${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${VERSION}"

echo ""
echo "Image ${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${VERSION} pushed to ACR successfully."
