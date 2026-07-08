@echo off
setlocal

REM Always run from this script directory so relative paths are stable
cd /d "%~dp0"

REM ============================================================
REM  Configuration – adjust to your environment
REM ============================================================
set "RESOURCE_GROUP=pps-rg"
set "CONTAINER_APP_ENV=pps-container-env"
set "ACR_NAME=ppscr"
set "SERVICE_APP=price-provider-service"

REM ============================================================
REM  SECURITY NOTE
REM  Credentials passed as command-line arguments may be visible
REM  in process listings and saved in command history.
REM  For production use, consider sourcing credentials from
REM  environment variables or a key vault instead.
REM  ACR authentication via admin credentials is sufficient for
REM  development; for production use managed identity instead:
REM    https://learn.microsoft.com/azure/container-apps/managed-identity-image-pull
REM ============================================================

REM ============================================================
REM  Check required parameters
REM ============================================================
if "%~1"=="" (
    echo Usage: 04-create-container-app-service.bat ^<version^> ^<db_jdbcurl^> ^<db_username^> ^<db_password^>
    echo Example: 04-create-container-app-service.bat 1.2.3 "jdbc:postgresql://pps-postgres.postgres.database.azure.com:5432/priceProviderService" ppsadmin MySecureP@ssw0rd
    echo.
    echo Prerequisites:
    echo   - Resource group and Container Apps environment created ^(01-create-resource-group^)
    echo   - Container registry created and admin enabled ^(03-create-container-registry^)
    echo   - Azure CLI logged in and ACR build permissions available
    exit /b 1
)
if "%~2"=="" (
    echo ERROR: db_jdbcurl parameter is required.
    exit /b 1
)
if "%~3"=="" (
    echo ERROR: db_username parameter is required.
    exit /b 1
)
if "%~4"=="" (
    echo ERROR: db_password parameter is required.
    exit /b 1
)

set "VERSION=%~1"
set "DB_JDBCURL=%~2"
set "DB_USERNAME=%~3"
set "DB_PASSWORD=%~4"

REM ============================================================
REM  Ensure service image exists in ACR (build on demand)
REM ============================================================
set "SERVICE_IMAGE=%SERVICE_APP%:%VERSION%"
set "SERVICE_BUILD_CONTEXT=..\..\service"

echo Checking ACR image tag "%SERVICE_IMAGE%"...
call az acr repository show --name "%ACR_NAME%" --image "%SERVICE_IMAGE%" >nul 2>&1
if %errorlevel% neq 0 (
    echo Image not found. Building and pushing with az acr build...
    call az acr build --registry "%ACR_NAME%" --image "%SERVICE_IMAGE%" "%SERVICE_BUILD_CONTEXT%"
    if %errorlevel% neq 0 (
        echo ERROR: Failed to build and push image "%SERVICE_IMAGE%".
        exit /b 1
    )
) else (
    echo Image already exists in ACR.
)

REM ============================================================
REM  Retrieve ACR admin credentials
REM ============================================================
echo Retrieving ACR credentials...
for /f "tokens=*" %%i in ('call az acr credential show --name "%ACR_NAME%" --query username -o tsv') do set "ACR_USERNAME=%%i"
for /f "tokens=*" %%i in ('call az acr credential show --name "%ACR_NAME%" --query passwords[0].value -o tsv') do set "ACR_PASSWORD=%%i"
if "%ACR_USERNAME%"=="" (
    echo ERROR: Could not retrieve ACR credentials. Ensure the registry exists and admin is enabled.
    exit /b 1
)

REM ============================================================
REM  Create Container App for price-provider-service
REM  DB credentials are stored as Container App secrets
REM ============================================================
echo Creating Container App "%SERVICE_APP%"...
echo   Image  : %ACR_NAME%.azurecr.io/%SERVICE_APP%:%VERSION%
call az containerapp create ^
    --name "%SERVICE_APP%" ^
    --resource-group "%RESOURCE_GROUP%" ^
    --environment "%CONTAINER_APP_ENV%" ^
    --image "%ACR_NAME%.azurecr.io/%SERVICE_APP%:%VERSION%" ^
    --target-port 8080 ^
    --ingress external ^
    --registry-server "%ACR_NAME%.azurecr.io" ^
    --registry-username "%ACR_USERNAME%" ^
    --registry-password "%ACR_PASSWORD%" ^
    --secrets "pps-db-jdbcurl=%DB_JDBCURL%" "pps-db-username=%DB_USERNAME%" "pps-db-password=%DB_PASSWORD%" ^
    --env-vars "PPS_DB_JDBCURL=secretref:pps-db-jdbcurl" "PPS_DB_USERNAME=secretref:pps-db-username" "PPS_DB_PASSWORD=secretref:pps-db-password" "PPS_DB_DRIVER=org.postgresql.Driver" "PPS_JPA_DB_PLATFORM=org.hibernate.dialect.PostgreSQLDialect" ^
    --min-replicas 0 ^
    --max-replicas 1
if %errorlevel% neq 0 (
    echo ERROR: Failed to create Container App.
    exit /b 1
)

echo.
echo Container App "%SERVICE_APP%" created successfully.
echo   Image: %ACR_NAME%.azurecr.io/%SERVICE_APP%:%VERSION%
echo.
echo Retrieve the app URL with:
echo   az containerapp show --name %SERVICE_APP% --resource-group %RESOURCE_GROUP% --query properties.configuration.ingress.fqdn -o tsv

endlocal
