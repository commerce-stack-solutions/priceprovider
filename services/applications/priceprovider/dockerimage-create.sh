#!/bin/bash

# Always execute from this script directory so relative paths are stable
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# Use provided version or default to 0.0.0-SNAPSHOT
VERSION=${1:-0.0.0-SNAPSHOT}
IMAGE_NAME="price-provider-service"

echo "Building Docker image ${IMAGE_NAME}:${VERSION}..."

# Build the Docker image
docker build -t ${IMAGE_NAME}:${VERSION} -f Dockerfile ../../..

echo "Docker image ${IMAGE_NAME}:${VERSION} built successfully."
