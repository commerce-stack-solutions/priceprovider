#!/bin/bash

# Create namespace
kubectl apply -f namespace.yaml

# Create ConfigMap
kubectl apply -f keycloak-config.yaml

# Deploy services
kubectl apply -f postgres.yaml
kubectl apply -f keycloak.yaml
kubectl apply -f service.yaml
kubectl apply -f app.yaml

echo "Waiting for PostgreSQL to be ready..."
kubectl rollout status deployment/db -n price-provider

echo "Waiting for Keycloak to be ready..."
kubectl rollout status deployment/keycloak -n price-provider

echo "Waiting for Backend Service to be ready..."
kubectl rollout status deployment/service -n price-provider

echo "Waiting for Frontend App to be ready..."
kubectl rollout status deployment/app -n price-provider

echo "Setup complete! Resources in 'price-provider' namespace:"
kubectl get all -n price-provider

echo ""
echo "Access the applications via the following LoadBalancer IPs (if available):"
kubectl get svc -n price-provider | grep LoadBalancer
