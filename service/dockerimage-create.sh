#!/bin/bash

# Use provided version or default to 0.0.0-SNAPSHOT
VERSION=${1:-0.0.0-SNAPSHOT}
IMAGE_NAME="price-provider-service"

echo "Building Docker image ${IMAGE_NAME}:${VERSION}..."

# Build the Docker image
docker build -t ${IMAGE_NAME}:${VERSION} .

echo "Docker image ${IMAGE_NAME}:${VERSION} built successfully."