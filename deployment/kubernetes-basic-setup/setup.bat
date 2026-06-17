@echo off

echo Creating namespace...
kubectl apply -f namespace.yaml

echo Creating ConfigMap...
kubectl apply -f keycloak-config.yaml

echo Deploying services...
kubectl apply -f postgres.yaml
kubectl apply -f keycloak.yaml
kubectl apply -f service.yaml
kubectl apply -f app.yaml
kubectl apply -f ingress.yaml

echo Waiting for deployments...
kubectl rollout status deployment/db -n price-provider
kubectl rollout status deployment/keycloak -n price-provider
kubectl rollout status deployment/service -n price-provider
kubectl rollout status deployment/app -n price-provider

echo Setup complete!
kubectl get all -n price-provider

echo.
echo Access the applications via the following hostnames (ensure they are in your hosts file):
echo - http://app.priceprovider.local
echo - http://service.priceprovider.local
echo - http://keycloak.priceprovider.local
echo.

pause
