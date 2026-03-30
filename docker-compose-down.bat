@echo off
REM docker-compose-down.bat [version] [services...]
REM If first parameter contains a dot (e.g. 1.2.3) it is treated as VERSION and ignored for down; remaining params are service names.

setlocal

REM If first arg looks like a version (contains a dot) drop it
if not "%~1"=="" (
    echo %~1 | findstr "\." >nul
    if %errorlevel%==0 (
        shift
    )
)

REM If no services specified -> full down, else stop+rm specified services
if "%*"=="" (
    echo Stopping and removing all services...
    docker-compose down
) else (
    echo Stopping services: %*
    docker-compose stop %*
    echo Removing stopped services: %*
    docker-compose rm -f %*
)

echo Done.
endlocal
