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
DB_SERVER_NAME="pps-postgres"
DB_NAME="priceProviderService"
DB_ADMIN_USER="ppsadmin"

# ============================================================
#  Check required parameter: admin password
# ============================================================
if [ -z "${1:-}" ]; then
    echo "Usage: 02-create-postgres-db.sh <db_admin_password>"
    echo "Example: 02-create-postgres-db.sh MySecureP@ssw0rd"
    echo ""
    echo "The password must meet Azure requirements:"
    echo "  - 8 to 128 characters"
    echo "  - Contains characters from at least three of the following categories:"
    echo "    English uppercase letters, English lowercase letters, digits, non-alphanumeric characters"
    exit 1
fi
DB_ADMIN_PASSWORD="$1"

# ============================================================
#  Create PostgreSQL Flexible Server (cheapest: Burstable B1ms)
# ============================================================
echo "Creating PostgreSQL Flexible Server \"${DB_SERVER_NAME}\"..."
echo "  SKU     : Standard_B1ms (Burstable, 1 vCore, 2 GiB RAM)"
echo "  Storage : 32 GiB"
echo "  Version : 16"
az postgres flexible-server create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${DB_SERVER_NAME}" \
    --location "${LOCATION}" \
    --admin-user "${DB_ADMIN_USER}" \
    --admin-password "${DB_ADMIN_PASSWORD}" \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --storage-size 32 \
    --version 16 \
    --yes

# ============================================================
#  Create application database
# ============================================================
echo "Creating database \"${DB_NAME}\"..."
az postgres flexible-server db create \
    --resource-group "${RESOURCE_GROUP}" \
    --server-name "${DB_SERVER_NAME}" \
    --database-name "${DB_NAME}"

# ============================================================
#  Allow Azure services to connect (0.0.0.0 magic address)
# ============================================================
echo "Allowing Azure services to connect to the database server..."
az postgres flexible-server firewall-rule create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${DB_SERVER_NAME}" \
    --rule-name AllowAzureServices \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0

echo ""
echo "PostgreSQL Flexible Server created successfully."
echo "  Server  : ${DB_SERVER_NAME}.postgres.database.azure.com"
echo "  Database: ${DB_NAME}"
echo "  User    : ${DB_ADMIN_USER}"
echo ""
echo "JDBC URL for use in container app scripts:"
echo "  jdbc:postgresql://${DB_SERVER_NAME}.postgres.database.azure.com:5432/${DB_NAME}"
