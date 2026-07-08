@echo off
setlocal

REM Always run from this script directory so relative paths are stable
cd /d "%~dp0"

REM ============================================================
REM  Configuration – adjust to your environment
REM ============================================================
set "ACR_NAME=ppscr"
set "IMAGE_NAME=price-provider-service"

REM ============================================================
REM  Check required parameter: version (three digits, e.g. 1.2.3)
REM ============================================================
if "%~1"=="" (
    echo Usage: 06-create-service-image.bat ^<version^>
    echo Example: 06-create-service-image.bat 1.2.3
    exit /b 1
)
set "VERSION=%~1"

REM ============================================================
REM  Resolve the service source directory
REM  This script lives in deployment/azure-container-apps/
REM  The service Dockerfile is in service/ (two levels up)
REM ============================================================
set "SCRIPT_DIR=%~dp0"
set "SERVICE_DIR=%SCRIPT_DIR%..\..\service"

REM ============================================================
REM  Log in to ACR
REM ============================================================
echo Logging in to Azure Container Registry "%ACR_NAME%"...
call az acr login --name "%ACR_NAME%"
if %errorlevel% neq 0 (
    echo ERROR: ACR login failed.
    exit /b 1
)

REM ============================================================
REM  Build Docker image
REM ============================================================
echo Building Docker image %IMAGE_NAME%:%VERSION%...
call docker build -t "%IMAGE_NAME%:%VERSION%" "%SERVICE_DIR%"
if %errorlevel% neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)

REM ============================================================
REM  Tag image for ACR
REM ============================================================
echo Tagging image for ACR...
call docker tag "%IMAGE_NAME%:%VERSION%" "%ACR_NAME%.azurecr.io/%IMAGE_NAME%:%VERSION%"
if %errorlevel% neq 0 (
    echo ERROR: Docker tag failed.
    exit /b 1
)

REM ============================================================
REM  Push image to ACR
REM ============================================================
echo Pushing image to ACR...
call docker push "%ACR_NAME%.azurecr.io/%IMAGE_NAME%:%VERSION%"
if %errorlevel% neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo Image %ACR_NAME%.azurecr.io/%IMAGE_NAME%:%VERSION% pushed to ACR successfully.

endlocal
