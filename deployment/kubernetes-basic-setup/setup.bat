@echo off

echo Creating namespace...
kubectl apply -f namespace.yaml

echo Installing Nginx Ingress Controller...
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

echo Creating ConfigMap...
kubectl apply -f keycloak-config.yaml

echo Deploying services...
kubectl apply -f postgres.yaml
kubectl apply -f keycloak.yaml
kubectl apply -f service.yaml
kubectl apply -f app.yaml
kubectl apply -f ingress.yaml

echo Configuring internal host aliases...
for /f "tokens=*" %%i in ('kubectl get svc keycloak -n price-provider -o jsonpath^="{.spec.clusterIP}"') do set KEYCLOAK_IP=%%i
for /f "tokens=*" %%i in ('kubectl get svc service -n price-provider -o jsonpath^="{.spec.clusterIP}"') do set SERVICE_IP=%%i

REM Patch service to find keycloak
kubectl patch deployment service -n price-provider --type="json" -p="[{\"op\": \"add\", \"path\": \"/spec/template/spec/hostAliases\", \"value\": [{\"ip\": \"%KEYCLOAK_IP%\", \"hostnames\": [\"keycloak.priceprovider.local\"]}]}]"

REM Patch app to find keycloak and service
kubectl patch deployment app -n price-provider --type="json" -p="[{\"op\": \"add\", \"path\": \"/spec/template/spec/hostAliases\", \"value\": [{\"ip\": \"%KEYCLOAK_IP%\", \"hostnames\": [\"keycloak.priceprovider.local\"]}, {\"ip\": \"%SERVICE_IP%\", \"hostnames\": [\"service.priceprovider.local\"]}]}]"

echo Waiting for deployments...
kubectl rollout status deployment/db -n price-provider
kubectl rollout status deployment/keycloak -n price-provider
kubectl rollout status deployment/service -n price-provider
kubectl rollout status deployment/app -n price-provider

echo Waiting for Ingress Controller to be ready...
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx

echo Setup complete!
kubectl get all -n price-provider

echo.
echo Access the applications via the following hostnames (ensure they are in your hosts file):
echo - http://app.priceprovider.local
echo - http://service.priceprovider.local
echo - http://keycloak.priceprovider.local
echo.

pause
