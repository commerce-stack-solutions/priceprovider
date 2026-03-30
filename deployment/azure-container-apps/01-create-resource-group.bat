@echo off
setlocal

REM Always run from this script directory so relative paths are stable
cd /d "%~dp0"

REM ============================================================
REM  Configuration – adjust to your environment
REM ============================================================
set "RESOURCE_GROUP=pps-rg"
set "LOCATION=westeurope"
set "CONTAINER_APP_ENV=pps-container-env"

REM ============================================================
REM  Create resource group
REM ============================================================
echo Creating resource group "%RESOURCE_GROUP%" in "%LOCATION%"...
call az group create --name "%RESOURCE_GROUP%" --location "%LOCATION%"
if %errorlevel% neq 0 (
    echo ERROR: Failed to create resource group.
    exit /b 1
)
REM ============================================================
REM  Create Container Apps environment
REM ============================================================
echo Creating Container Apps environment "%CONTAINER_APP_ENV%"...
call az containerapp env create ^
    --name "%CONTAINER_APP_ENV%" ^
    --resource-group "%RESOURCE_GROUP%" ^
    --location "%LOCATION%"
if %errorlevel% neq 0 (
    echo ERROR: Failed to create Container Apps environment.
    exit /b 1
)


echo.
echo Resource group and Container Apps environment created successfully.
echo   Resource Group : %RESOURCE_GROUP%
echo   Location       : %LOCATION%
echo   CA Environment : %CONTAINER_APP_ENV%

endlocal
