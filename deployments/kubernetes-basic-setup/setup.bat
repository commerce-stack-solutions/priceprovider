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

echo Waiting for deployments...
kubectl rollout status deployment/db -n price-provider
kubectl rollout status deployment/keycloak -n price-provider
kubectl rollout status deployment/service -n price-provider
kubectl rollout status deployment/app -n price-provider

echo Setup complete!
kubectl get all -n price-provider
pause
