@echo off
setlocal

REM Always run from this script directory so relative paths are stable
cd /d "%~dp0"

REM ============================================================
REM  Configuration – adjust to your environment
REM ============================================================
set "RESOURCE_GROUP=pps-rg"
set "LOCATION=westeurope"
set "DB_SERVER_NAME=pps-postgres"
set "DB_NAME=priceProviderService"
set "DB_ADMIN_USER=ppsadmin"

REM ============================================================
REM  Check required parameter: admin password
REM ============================================================
if "%~1"=="" (
    echo Usage: 02-create-postgres-db.bat ^<db_admin_password^>
    echo Example: 02-create-postgres-db.bat MySecureP@ssw0rd
    echo.
    echo The password must meet Azure requirements:
    echo   - 8 to 128 characters
    echo   - Contains characters from at least three of the following categories:
    echo     English uppercase letters, English lowercase letters, digits, non-alphanumeric characters
    exit /b 1
)
set "DB_ADMIN_PASSWORD=%~1"

REM ============================================================
REM  Create PostgreSQL Flexible Server (cheapest: Burstable B1ms)
REM ============================================================
echo Creating PostgreSQL Flexible Server "%DB_SERVER_NAME%"...
echo   SKU     : Standard_B1ms ^(Burstable, 1 vCore, 2 GiB RAM^)
echo   Storage : 32 GiB
echo   Version : 16
call az postgres flexible-server create ^
    --resource-group "%RESOURCE_GROUP%" ^
    --name "%DB_SERVER_NAME%" ^
    --location "%LOCATION%" ^
    --admin-user "%DB_ADMIN_USER%" ^
    --admin-password "%DB_ADMIN_PASSWORD%" ^
    --sku-name Standard_B1ms ^
    --tier Burstable ^
    --storage-size 32 ^
    --version 16 ^
    --yes
if %errorlevel% neq 0 (
    echo ERROR: Failed to create PostgreSQL Flexible Server.
    exit /b 1
)

REM ============================================================
REM  Create application database
REM ============================================================
echo Creating database "%DB_NAME%"...
call az postgres flexible-server db create ^
    --resource-group "%RESOURCE_GROUP%" ^
    --server-name "%DB_SERVER_NAME%" ^
    --database-name "%DB_NAME%"
if %errorlevel% neq 0 (
    echo ERROR: Failed to create database.
    exit /b 1
)

REM ============================================================
REM  Allow Azure services to connect (0.0.0.0 magic address)
REM ============================================================
echo Allowing Azure services to connect to the database server...
call az postgres flexible-server firewall-rule create ^
    --resource-group "%RESOURCE_GROUP%" ^
    --name "%DB_SERVER_NAME%" ^
    --rule-name AllowAzureServices ^
    --start-ip-address 0.0.0.0 ^
    --end-ip-address 0.0.0.0
if %errorlevel% neq 0 (
    echo ERROR: Failed to create firewall rule.
    exit /b 1
)

echo.
echo PostgreSQL Flexible Server created successfully.
echo   Server  : %DB_SERVER_NAME%.postgres.database.azure.com
echo   Database: %DB_NAME%
echo   User    : %DB_ADMIN_USER%
echo.
echo JDBC URL for use in container app scripts:
echo   jdbc:postgresql://%DB_SERVER_NAME%.postgres.database.azure.com:5432/%DB_NAME%

endlocal
