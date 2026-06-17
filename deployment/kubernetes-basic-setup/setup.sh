#!/bin/bash

# Create namespace
kubectl apply -f namespace.yaml

# Cleanup conflicting default nginx if present
echo "Checking for conflicting Nginx in default namespace..."
kubectl delete service nginx --namespace default 2>/dev/null || true
kubectl delete deployment nginx --namespace default 2>/dev/null || true

# Install Nginx Ingress Controller (Docker Desktop / Cloud provider style)
echo "Installing Nginx Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Create ConfigMap
kubectl apply -f keycloak-config.yaml

# Deploy services
kubectl apply -f postgres.yaml
kubectl apply -f keycloak.yaml
kubectl apply -f service.yaml
kubectl apply -f app.yaml
kubectl apply -f ingress.yaml

echo "Configuring internal host aliases..."
KEYCLOAK_IP=$(kubectl get svc keycloak -n price-provider -o jsonpath='{.spec.clusterIP}')
SERVICE_IP=$(kubectl get svc service -n price-provider -o jsonpath='{.spec.clusterIP}')

# Patch service to find keycloak
kubectl patch deployment service -n price-provider --type='json' -p="[{\"op\": \"add\", \"path\": \"/spec/template/spec/hostAliases\", \"value\": [{\"ip\": \"$KEYCLOAK_IP\", \"hostnames\": [\"keycloak.priceprovider.local\"]}]}]"

# Patch app to find keycloak and service
kubectl patch deployment app -n price-provider --type='json' -p="[{\"op\": \"add\", \"path\": \"/spec/template/spec/hostAliases\", \"value\": [{\"ip\": \"$KEYCLOAK_IP\", \"hostnames\": [\"keycloak.priceprovider.local\"]}, {\"ip\": \"$SERVICE_IP\", \"hostnames\": [\"service.priceprovider.local\"]}]}]"

echo "Waiting for PostgreSQL to be ready..."
kubectl rollout status deployment/db -n price-provider

echo "Waiting for Keycloak to be ready..."
kubectl rollout status deployment/keycloak -n price-provider

echo "Waiting for Backend Service to be ready..."
kubectl rollout status deployment/service -n price-provider

echo "Waiting for Frontend App to be ready..."
kubectl rollout status deployment/app -n price-provider

echo "Waiting for Ingress Controller to be ready..."
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx

echo "Waiting for Ingress to be ready (this might take a minute)..."

echo "Setup complete! Resources in 'price-provider' namespace:"
kubectl get all -n price-provider

echo ""
echo "Access the applications via the following hostnames (ensure they are in your /etc/hosts):"
echo "- http://app.priceprovider.local"
echo "- http://service.priceprovider.local"
echo "- http://keycloak.priceprovider.local"
