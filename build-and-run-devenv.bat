@echo off
REM build-and-run-devenv.bat
REM Fast setup: builds dev Docker images (0.0.0-SNAPSHOT) for the service and app, then starts the full stack.

setlocal ENABLEDELAYEDEXPANSION

set "VERSION=0.0.0-SNAPSHOT"
set "SCRIPT_DIR=%~dp0"
REM Remove trailing backslash
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM ---------------------------------------------------------------------------
REM Welcome screen
REM ---------------------------------------------------------------------------
echo.
echo              _                           _    __
echo    ___  ____(_)______ ___  _______ _  __(_)__/ /__ ____
echo   / _ \/ __/ / __/ -_) _ \/ __/ _ \ ^|/ / / _  / -_) __/
echo  / .__/_/ /_/\__/\__/ .__/_/  \___/___/_/\_,_/\__/_/
echo /_/                /_/
echo.
echo   build-and-run-devenv  ^|  Fast Setup and Run
echo   Version: %VERSION%
echo.
echo ================================================================
echo.

REM ---------------------------------------------------------------------------
REM Prerequisites check
REM ---------------------------------------------------------------------------
echo Checking prerequisites...
echo.

where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo   [MISSING] Docker is not installed or not on PATH.
    echo.
    echo   Please install Docker Desktop:
    echo     https://www.docker.com/products/docker-desktop/
    echo.
    endlocal
    exit /b 1
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo   [NOT RUNNING] Docker daemon is not running.
    echo.
    echo   Please start Docker Desktop:
    echo     https://www.docker.com/products/docker-desktop/
    echo.
    endlocal
    exit /b 1
)

for /f "tokens=*" %%v in ('docker --version') do set "DOCKER_VER=%%v"
echo   [OK] Docker Desktop is running  (%DOCKER_VER%)
echo.
echo ================================================================
echo.

REM ---------------------------------------------------------------------------
REM Build Docker image: Price Provider Service
REM ---------------------------------------------------------------------------
echo   [1/2] Building Docker image for Price Provider Service...
echo         Image: price-provider-service:%VERSION%
echo.

pushd "%SCRIPT_DIR%\service"
call dockerimage-create.bat %VERSION%
if %errorlevel% neq 0 (
    echo ERROR: Failed to build Price Provider Service image.
    popd
    endlocal
    exit /b 1
)
popd
echo.

REM ---------------------------------------------------------------------------
REM Build Docker image: Price Manager App
REM ---------------------------------------------------------------------------
echo   [2/2] Building Docker image for Price Manager App...
echo         Image: price-manager-app:%VERSION%
echo.

pushd "%SCRIPT_DIR%\app"
call dockerimage-create.bat %VERSION%
if %errorlevel% neq 0 (
    echo ERROR: Failed to build Price Manager App image.
    popd
    endlocal
    exit /b 1
)
popd
echo.

echo ================================================================
echo.

REM ---------------------------------------------------------------------------
REM Start the stack
REM ---------------------------------------------------------------------------
echo Starting the full stack with docker-compose...
echo   (postgres, keycloak, service, app)
echo.

set "VERSION=%VERSION%"
pushd "%SCRIPT_DIR%"
docker-compose up -d
if %errorlevel% neq 0 (
    echo ERROR: docker-compose up failed.
    popd
    endlocal
    exit /b 1
)
popd

echo.
echo ================================================================
echo.
echo   All services are starting up!
echo.
echo   Price Manager App   -^>  http://localhost
echo   Price Provider API  -^>  http://localhost:8080
echo   Keycloak (IdP)      -^>  http://localhost:8081
echo.
echo   To stream logs:  docker-compose logs -f
echo   To stop:         docker-compose-down.bat
echo            or:     docker-compose down
echo.
echo ================================================================
echo.

endlocal
