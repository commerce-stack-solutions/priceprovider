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
set "APP_APP=price-manager-app"

REM ============================================================
REM  SECURITY NOTE
REM  ACR admin credentials are used for simplicity. For production
REM  environments, consider using managed identity instead:
REM    https://learn.microsoft.com/azure/container-apps/managed-identity-image-pull
REM ============================================================

REM ============================================================
REM  Check required parameters
REM ============================================================
if "%~1"=="" (
    echo Usage: 05-create-container-app-app.bat ^<version^> ^<pps_baseurl^>
    echo Example: 05-create-container-app-app.bat 1.2.3 https://price-provider-service.nicefield-abc123.westeurope.azurecontainerapps.io/
    echo.
    echo Prerequisites:
    echo   - Resource group and Container Apps environment created ^(01-create-resource-group^)
    echo   - Container registry created and admin enabled ^(03-create-container-registry^)
    echo   - Azure CLI logged in and ACR build permissions available
    echo   - Service container app URL known ^(04-create-container-app-service^)
    exit /b 1
)
if "%~2"=="" (
    echo ERROR: pps_baseurl parameter is required.
    exit /b 1
)

set "VERSION=%~1"
set "PPS_BASEURL=%~2"

REM ============================================================
REM  Ensure app image exists in ACR (build on demand)
REM ============================================================
set "APP_IMAGE=%APP_APP%:%VERSION%"
set "APP_BUILD_CONTEXT=..\..\app"

echo Checking ACR image tag "%APP_IMAGE%"...
call az acr repository show --name "%ACR_NAME%" --image "%APP_IMAGE%" >nul 2>&1
if %errorlevel% neq 0 (
    echo Image not found. Building and pushing with az acr build...
    call az acr build --registry "%ACR_NAME%" --image "%APP_IMAGE%" "%APP_BUILD_CONTEXT%"
    if %errorlevel% neq 0 (
        echo ERROR: Failed to build and push image "%APP_IMAGE%".
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
REM  Create Container App for price-manager-app
REM ============================================================
echo Creating Container App "%APP_APP%"...
echo   Image      : %ACR_NAME%.azurecr.io/%APP_APP%:%VERSION%
echo   PPS_BASEURL: %PPS_BASEURL%
call az containerapp create ^
    --name "%APP_APP%" ^
    --resource-group "%RESOURCE_GROUP%" ^
    --environment "%CONTAINER_APP_ENV%" ^
    --image "%ACR_NAME%.azurecr.io/%APP_APP%:%VERSION%" ^
    --target-port 80 ^
    --ingress external ^
    --registry-server "%ACR_NAME%.azurecr.io" ^
    --registry-username "%ACR_USERNAME%" ^
    --registry-password "%ACR_PASSWORD%" ^
    --env-vars "PPS_BASEURL=%PPS_BASEURL%" ^
    --min-replicas 0 ^
    --max-replicas 1
if %errorlevel% neq 0 (
    echo ERROR: Failed to create Container App.
    exit /b 1
)

echo.
echo Container App "%APP_APP%" created successfully.
echo   Image: %ACR_NAME%.azurecr.io/%APP_APP%:%VERSION%
echo.
echo Retrieve the app URL with:
echo   az containerapp show --name %APP_APP% --resource-group %RESOURCE_GROUP% --query properties.configuration.ingress.fqdn -o tsv

endlocal
