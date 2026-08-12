@echo off
REM Use provided version or default to 0.0.0-SNAPSHOT
if [%1]==[] (
    set VERSION=0.0.0-SNAPSHOT
) else (
    set VERSION=%1
)

set IMAGE_NAME="price-manager-app"
set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%..\.."
set "APP_DOCKERFILE=%SCRIPT_DIR%Dockerfile"

echo Building Docker image %IMAGE_NAME%:%VERSION%...

REM Build the Docker image
docker build -f "%APP_DOCKERFILE%" -t %IMAGE_NAME%:%VERSION% "%REPO_ROOT%"

echo Docker image %IMAGE_NAME%:%VERSION% built successfully.
