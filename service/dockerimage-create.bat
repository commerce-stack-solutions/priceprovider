@echo off
REM Use provided version or default to 0.0.0-SNAPSHOT
if [%1]==[] (
    set VERSION=0.0.0-SNAPSHOT
) else (
    set VERSION=%1
)

set IMAGE_NAME="price-provider-service"

echo Building Docker image %IMAGE_NAME%:%VERSION%...

REM Build the Docker image
docker build -t %IMAGE_NAME%:%VERSION% .

echo Docker image %IMAGE_NAME%:%VERSION% built successfully.