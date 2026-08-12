#!/bin/bash

# Use provided version or default to 0.0.0-SNAPSHOT
VERSION=${1:-0.0.0-SNAPSHOT}
IMAGE_NAME="price-manager-app"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${SCRIPT_DIR}/../.."
APP_DOCKERFILE="${SCRIPT_DIR}/Dockerfile"

echo "Building Docker image ${IMAGE_NAME}:${VERSION}..."

# Build the Docker image
docker build -f "${APP_DOCKERFILE}" -t "${IMAGE_NAME}:${VERSION}" "${REPO_ROOT}"

echo "Docker image ${IMAGE_NAME}:${VERSION} built successfully."
