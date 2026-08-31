#!/bin/bash
set -e

NAMESPACE="price-provider"
ARGOCD_NAMESPACE="argocd"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Price Provider Kubernetes Helm & Argo CD Setup ==="

# Check for kubectl
if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl is not installed."
    exit 1
fi

# Check for helm
if ! command -v helm &> /dev/null; then
    echo "Error: helm is not installed."
    exit 1
fi

# Create namespace
echo "Creating namespace '${NAMESPACE}'..."
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# Build Helm dependencies for local-dev
echo "Updating Helm chart dependencies for local-dev environment..."
helm dependency update "${SCRIPT_DIR}/environments/local-dev"
echo "Ensure gateway.className in environments/local-dev/values.yaml matches an installed GatewayClass before deploying."

# Option to deploy via direct Helm or via Argo CD
MODE="${1:-helm}"

if [ "$MODE" = "argocd" ]; then
    echo "Deploying application layer via Argo CD App-of-Apps..."
    kubectl create namespace ${ARGOCD_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    kubectl apply -f "${SCRIPT_DIR}/argocd/app-of-apps.yaml"
    echo "Argo CD App-of-Apps applied successfully!"
    echo "Optional infrastructure can be added with:"
    echo "kubectl apply -f ${SCRIPT_DIR}/argocd/local-dev-infrastructure.yaml"
else
    echo "Deploying local-dev environment directly via Helm..."
    helm upgrade --install local-dev "${SCRIPT_DIR}/environments/local-dev" --namespace ${NAMESPACE}
    echo "Helm release 'local-dev' deployed successfully!"
fi

echo ""
echo "=== Hostnames Setup ==="
echo "Add the following entries to your /etc/hosts file:"
echo "127.0.0.1 app.priceprovider.local"
echo "127.0.0.1 service.priceprovider.local"
echo "127.0.0.1 keycloak.priceprovider.local"
echo ""
echo "Setup complete!"
