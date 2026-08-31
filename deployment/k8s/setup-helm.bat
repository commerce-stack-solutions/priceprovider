@echo off
setlocal enabledelayedexpansion

set NAMESPACE=price-provider
set ARGOCD_NAMESPACE=argocd
set SCRIPT_DIR=%~dp0

echo === Price Provider Kubernetes Helm ^& Argo CD Setup ===

where kubectl >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: kubectl is not installed.
    exit /b 1
)

where helm >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: helm is not installed.
    exit /b 1
)

echo Creating namespace '%NAMESPACE%'...
kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -

echo Updating Helm chart dependencies for local-dev environment...
helm dependency update "%SCRIPT_DIR%environments\local-dev"
echo Ensure gateway.className in environments\local-dev\values.yaml matches an installed GatewayClass before deploying.

set MODE=%1
if "%MODE%"=="argocd" (
    echo Deploying application layer via Argo CD App-of-Apps...
    kubectl create namespace %ARGOCD_NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -
    kubectl apply -f "%SCRIPT_DIR%argocd\app-of-apps.yaml"
    echo Argo CD App-of-Apps applied successfully!
    echo Optional infrastructure can be added with:
    echo kubectl apply -f "%SCRIPT_DIR%argocd\local-dev-infrastructure.yaml"
) else (
    echo Deploying local-dev environment directly via Helm...
    helm upgrade --install local-dev "%SCRIPT_DIR%environments\local-dev" --namespace %NAMESPACE%
    echo Helm release 'local-dev' deployed successfully!
)

echo.
echo === Hostnames Setup ===
echo Add the following entries to your C:\Windows\System32\drivers\etc\hosts file:
echo 127.0.0.1 app.priceprovider.local
echo 127.0.0.1 service.priceprovider.local
echo 127.0.0.1 keycloak.priceprovider.local
echo.
echo Setup complete!
