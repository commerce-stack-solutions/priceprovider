#!/bin/bash
# docker-compose-up.sh [version] [services...]
# If first parameter contains a dot (e.g. 1.2.3) it is treated as VERSION; remaining params are service names.

set -euo pipefail

# Detect optional version (contains a dot)
VERSION="0.0.0-SNAPSHOT"
if [[ -n "${1:-}" && "${1}" == *.* ]]; then
  VERSION="$1"
  shift
fi
export VERSION

# Environment variables with defaults if not already set
export PPS_DB_USERNAME=${PPS_DB_USERNAME:-postgres}
export PPS_DB_PASSWORD=${PPS_DB_PASSWORD:-postgres}
export PPS_DB_JDBCURL=${PPS_DB_JDBCURL:-jdbc:postgresql://db:5432/priceProviderService}
export PPS_DB_DRIVER=${PPS_DB_DRIVER:-org.postgresql.Driver}
export PPS_JPA_DB_PLATFORM=${PPS_JPA_DB_PLATFORM:-org.hibernate.dialect.PostgreSQLDialect}
export PPS_BASEURL=${PPS_BASEURL:-http://localhost:8080/}

echo "Starting Price Provider services with version ${VERSION}..."
echo "Database: ${PPS_DB_JDBCURL}"
echo "API Base URL: ${PPS_BASEURL}"

# Determine services to start: if none provided, default to all (db, service, app)
if [ $# -eq 0 ]; then
  SELECTED_SERVICES=(db service app)
else
  SELECTED_SERVICES=("$@")
fi

# Function to map service -> image name
service_image() {
  case "$1" in
    db) echo "postgres:16-alpine" ;;
    service) echo "price-provider-service:${VERSION}" ;;
    app) echo "price-manager-app:${VERSION}" ;;
    *) echo "" ;;
  esac
}

# Check that images for the requested services exist locally. Abort if any missing (no pull).
for s in "${SELECTED_SERVICES[@]}"; do
  IMG=$(service_image "$s")
  if [ -z "$IMG" ]; then
    echo "Unknown service: $s"
    echo "Allowed services: db service app"
    exit 1
  fi
  if ! docker image inspect "$IMG" > /dev/null 2>&1; then
    echo "ERROR: Required image '$IMG' not found locally. Aborting because this script will NOT pull images."
    exit 2
  fi
done

# All required images exist locally. Start the requested services without building or pulling.
if [ ${#SELECTED_SERVICES[@]} -eq 3 ] && [ "${SELECTED_SERVICES[0]}" = "db" ] && [ "${SELECTED_SERVICES[1]}" = "service" ] && [ "${SELECTED_SERVICES[2]}" = "app" ]; then
  docker-compose up -d --no-build
else
  docker-compose up -d --no-build "${SELECTED_SERVICES[@]}"
fi

echo
echo "Services started successfully!"

echo ""
echo "To view logs: docker-compose logs -f"
echo "To stop services: docker-compose down"
