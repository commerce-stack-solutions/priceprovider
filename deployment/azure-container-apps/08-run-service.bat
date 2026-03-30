@echo off
setlocal

REM Always run from this script directory so relative paths are stable
cd /d "%~dp0"

REM ============================================================
REM  Configuration – adjust to your environment
REM ============================================================
set "RESOURCE_GROUP=pps-rg"
set "ACR_NAME=ppscr"
set "SERVICE_APP=price-provider-service"

REM ============================================================
REM  Check required parameter: version (three digits, e.g. 1.2.3)
REM ============================================================
if "%~1"=="" (
    echo Usage: 08-run-service.bat ^<version^>
    echo Example: 08-run-service.bat 1.2.3
    echo.
    echo Prerequisites:
    echo   - Service image must be built and pushed to ACR ^(06-create-service-image^)
    echo   - Container App must already exist ^(04-create-container-app-service^)
    exit /b 1
)
set "VERSION=%~1"

REM ============================================================
REM  Update Container App to the specified image version
REM ============================================================
echo Updating Container App "%SERVICE_APP%" to image version "%VERSION%"...
call az containerapp update ^
    --name "%SERVICE_APP%" ^
    --resource-group "%RESOURCE_GROUP%" ^
    --image "%ACR_NAME%.azurecr.io/%SERVICE_APP%:%VERSION%"
if %errorlevel% neq 0 (
    echo ERROR: Failed to update Container App.
    exit /b 1
)

echo.
echo Container App "%SERVICE_APP%" updated to version "%VERSION%" successfully.
echo.
echo Retrieve the app URL with:
echo   az containerapp show --name %SERVICE_APP% --resource-group %RESOURCE_GROUP% --query properties.configuration.ingress.fqdn -o tsv

endlocal
