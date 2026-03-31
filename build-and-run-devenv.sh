#!/bin/bash
# build-and-run-devenv.sh
# Fast setup: builds dev Docker images (0.0.0-SNAPSHOT) for the service and app, then starts the full stack.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="0.0.0-SNAPSHOT"

# ---------------------------------------------------------------------------
# Welcome screen
# ---------------------------------------------------------------------------
echo ""
echo "             _                           _    __       "
echo "   ___  ____(_)______ ___  _______ _  __(_)__/ /__ ____"
echo "  / _ \/ __/ / __/ -_) _ \/ __/ _ \ |/ / / _  / -_) __/"
echo " / .__/_/ /_/\__/\__/ .__/_/  \___/___/_/\_,_/\__/_/   "
echo "/_/                /_/                                 "
echo ""
echo "  build-and-run-devenv  |  Fast Setup and Run"
echo "  Version: ${VERSION}"
echo ""
echo "================================================================"
echo ""

# ---------------------------------------------------------------------------
# Prerequisites check
# ---------------------------------------------------------------------------
echo "Checking prerequisites..."
echo ""

if ! command -v docker > /dev/null 2>&1; then
    echo "  [MISSING] Docker is not installed or not on PATH."
    echo ""
    echo "  Please install Docker Desktop:"
    echo "    https://www.docker.com/products/docker-desktop/"
    echo ""
    exit 1
fi

if ! docker info > /dev/null 2>&1; then
    echo "  [NOT RUNNING] Docker daemon is not running."
    echo ""
    echo "  Please start Docker Desktop:"
    echo "    https://www.docker.com/products/docker-desktop/"
    echo ""
    exit 1
fi

echo "  [OK] Docker Desktop is running  ($(docker --version))"
echo ""
echo "================================================================"
echo ""

# ---------------------------------------------------------------------------
# Build Docker image: Price Provider Service
# ---------------------------------------------------------------------------
echo "  [1/2] Building Docker image for Price Provider Service..."
echo "        Image: price-provider-service:${VERSION}"
echo ""
(cd "${SCRIPT_DIR}/service" && bash dockerimage-create.sh "${VERSION}")
echo ""

# ---------------------------------------------------------------------------
# Build Docker image: Price Manager App
# ---------------------------------------------------------------------------
echo "  [2/2] Building Docker image for Price Manager App..."
echo "        Image: price-manager-app:${VERSION}"
echo ""
(cd "${SCRIPT_DIR}/app" && bash dockerimage-create.sh "${VERSION}")
echo ""

echo "================================================================"
echo ""

# ---------------------------------------------------------------------------
# Start the stack
# ---------------------------------------------------------------------------
echo "Starting the full stack with docker-compose..."
echo "  (postgres, keycloak, service, app)"
echo ""

export VERSION
cd "${SCRIPT_DIR}"
docker-compose up -d

echo ""
echo "================================================================"
echo ""
echo "  All services are starting up!"
echo ""
echo "  Price Manager App   ->  http://localhost"
echo "  Price Provider API  ->  http://localhost:8080"
echo "  Keycloak (IdP)      ->  http://localhost:8081"
echo ""
echo "  To stream logs:  docker-compose logs -f"
echo "  To stop:         ./docker-compose-down.sh"
echo "           or:     docker-compose down"
echo ""
echo "================================================================"
echo ""
