@echo off
REM build-and-run-devenv.bat
REM Fast setup: builds dev Docker images (0.0.0-SNAPSHOT) for the service and app, then starts the full stack.
REM If Node.js / npm is available, example frontends are also installed and started.

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

REM Check for Node.js / npm (optional)
set "NODE_AVAILABLE=false"
where node >nul 2>&1
if %errorlevel% equ 0 (
    where npm >nul 2>&1
    if !errorlevel! equ 0 (
        set "NODE_AVAILABLE=true"
        for /f "tokens=*" %%n in ('node --version') do set "NODE_VER=%%n"
        echo   [OK] Node.js is available  (!NODE_VER!)  – example frontends will be started
    )
)
if "!NODE_AVAILABLE!"=="false" (
    echo   [--] Node.js / npm not found – example frontends will be skipped
    echo        Install from https://nodejs.org/ to also run the example frontends
)

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
echo Starting the full stack with docker compose...
echo   (postgres, keycloak, service, app)
echo.

REM Ensure the example frontend origins are allowed by the service (CORS).
if not defined PPS_CORS_ALLOWED_ORIGINS (
    set "PPS_CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:4200,http://localhost:3000,http://localhost:3001,http://localhost:8081"
)

set "VERSION=%VERSION%"
pushd "%SCRIPT_DIR%"
docker compose up -d
if %errorlevel% neq 0 (
    echo ERROR: docker compose up failed.
    popd
    endlocal
    exit /b 1
)
popd

REM ---------------------------------------------------------------------------
REM Start example frontends (only when Node.js / npm are available)
REM ---------------------------------------------------------------------------
if "!NODE_AVAILABLE!"=="true" (
    echo.
    echo ================================================================
    echo.
    echo Starting example frontends...
    echo.

    for %%f in (shopfrontend rentalfrontend) do (
        echo   Installing dependencies for %%f...
        pushd "%SCRIPT_DIR%\examples\%%f"
        call npm install --silent
        echo   Starting %%f in the background...
        start "%%f" /min cmd /c "npm start > %TEMP%\%%f.log 2>&1"
        echo   [OK] %%f started  (log: %TEMP%\%%f.log)
        popd
    )
)

echo.
echo ================================================================
echo.
echo   All services are starting up!
echo.
echo   Price Manager App      -^>  http://localhost
echo   Price Provider API     -^>  http://localhost:8080
echo   Keycloak (IdP)         -^>  http://localhost:8081
if "!NODE_AVAILABLE!"=="true" (
    echo   Shop Frontend (demo^)   -^>  http://localhost:3000
    echo   Rental Frontend (demo^) -^>  http://localhost:3001
)
echo.
echo   Default Users (Keycloak realm: priceprovider^):
echo   Admin users:
echo     admin-user       / admin123       (Admin    - read/write/delete^)
echo     contributor-user / contributor123 (Contributor - read/write^)
echo     reader-user      / reader123      (Reader   - read-only^)
echo   Shop frontend customers:
echo     customer-city-council / customer123 (Org: City Council^)
echo     customer-city-health  / customer123 (Org: City Health^)
echo     customer-techcorp     / customer123 (Org: TechCorp EU^)
echo   Rental frontend customers:
echo     rental-builder-pro / rental123 (Org: Builder Pro^)
echo     rental-green-land  / rental123 (Org: Green Land^)
echo.
echo   Documentation:
echo   Project README         -^>  %SCRIPT_DIR%\README.md
echo   Service README         -^>  %SCRIPT_DIR%\service\README.md
echo   App README             -^>  %SCRIPT_DIR%\app\README.md
echo   Shop Frontend README   -^>  %SCRIPT_DIR%\examples\shopfrontend\README.md
echo   Rental Frontend README -^>  %SCRIPT_DIR%\examples\rentalfrontend\README.md
echo.
echo   To stream logs:  docker compose logs -f
echo   To stop:         docker compose down
echo.
echo ================================================================
echo.

endlocal
