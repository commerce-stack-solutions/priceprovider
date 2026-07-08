@echo off
setlocal

REM Always run from this script directory so relative paths are stable
cd /d "%~dp0"

REM ============================================================
REM  Configuration – adjust to your environment
REM ============================================================
set "RESOURCE_GROUP=pps-rg"
set "ACR_NAME=ppscr"

REM ============================================================
REM  Create Azure Container Registry (Basic SKU – cheapest)
REM ============================================================
echo Creating Azure Container Registry "%ACR_NAME%"...
call az acr create ^
    --resource-group "%RESOURCE_GROUP%" ^
    --name "%ACR_NAME%" ^
    --sku Basic
if %errorlevel% neq 0 (
    echo ERROR: Failed to create container registry.
    exit /b 1
)

REM ============================================================
REM  Enable admin user so container apps can authenticate
REM ============================================================
echo Enabling admin user on registry "%ACR_NAME%"...
call az acr update ^
    --resource-group "%RESOURCE_GROUP%" ^
    --name "%ACR_NAME%" ^
    --admin-enabled true
if %errorlevel% neq 0 (
    echo ERROR: Failed to enable admin user.
    exit /b 1
)

echo.
echo Azure Container Registry created successfully.
echo   Registry  : %ACR_NAME%.azurecr.io
echo   SKU       : Basic
echo   Admin user: enabled
echo.
echo Retrieve registry credentials with:
echo   az acr credential show --name %ACR_NAME%

endlocal
