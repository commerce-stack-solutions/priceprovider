@echo off
REM docker-compose-up.bat [version] [services...]
REM If first parameter contains a dot (e.g. 1.2.3) it is treated as VERSION; remaining params are service names.

REM Enable delayed expansion for safety in complex cases
setlocal ENABLEDELAYEDEXPANSION

REM Default version
set "VERSION=0.0.0-SNAPSHOT"

REM If first arg exists and contains a dot, treat it as version and shift it out
if not "%~1"=="" (
    echo %~1 | findstr "\." >nul && (
        set "VERSION=%~1"
        shift
    )
)

REM Environment variables with defaults if not already defined in environment
if not defined PPS_DB_USERNAME set "PPS_DB_USERNAME=postgres"
if not defined PPS_DB_PASSWORD set "PPS_DB_PASSWORD=postgres"
if not defined PPS_DB_JDBCURL set "PPS_DB_JDBCURL=jdbc:postgresql://db:5432/priceProviderService"
if not defined PPS_DB_DRIVER set "PPS_DB_DRIVER=org.postgresql.Driver"
if not defined PPS_JPA_DB_PLATFORM set "PPS_JPA_DB_PLATFORM=org.hibernate.dialect.PostgreSQLDialect"
if not defined PPS_BASEURL set "PPS_BASEURL=http://localhost:8080/"

REM Make VERSION available to docker-compose (child process will see it)
set "VERSION=%VERSION%"

echo Starting Price Provider services with version %VERSION%...
echo Database: %PPS_DB_JDBCURL%
echo API Base URL: %PPS_BASEURL%

REM Determine services to start: if none provided, default to all (db, service, app)
if "%*"=="" (
    set "SELECTED_SERVICES=db service app"
) else (
    set "SELECTED_SERVICES=%*"
)

REM Check that images for the requested services exist locally. If any image is missing, abort (no pull).
for %%s in (%SELECTED_SERVICES%) do (
    set "IMG="
    if /I "%%~s"=="db" set "IMG=postgres:16-alpine"
    if /I "%%~s"=="service" set "IMG=price-provider-service:%VERSION%"
    if /I "%%~s"=="app" set "IMG=price-manager-app:%VERSION%"
    if "!IMG!"=="" (
        echo Unknown service: %%~s
        echo Allowed services: db service app
        endlocal
        exit /b 1
    )
    REM Check local image existence using docker image inspect (returns non-zero if not found)
    docker image inspect "!IMG!" >nul 2>&1 || (
        echo ERROR: Required image '!IMG!' not found locally. Aborting because this script will NOT pull images.
        endlocal
        exit /b 1
    )
)

REM All required images exist locally. Start the requested services without building or pulling.
if "%SELECTED_SERVICES%"=="db service app" (
    docker-compose up -d --no-build
) else (
    docker-compose up -d --no-build %SELECTED_SERVICES%
)

echo.
echo Services started successfully!

endlocal
