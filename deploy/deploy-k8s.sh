#!/usr/bin/env bash
#
# Deploys the three-tier application to a local Kubernetes cluster.
# Works with minikube, k3s, kind, or Docker Desktop Kubernetes.
#
#   ./deploy/deploy-k8s.sh
#
# The numeric filename prefixes on the manifests enforce apply order:
# config and secrets must exist before the workloads that reference them.
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K8S_DIR="${REPO_DIR}/k8s"
CHART_DIR="${REPO_DIR}/helm/threetier-app"
RELEASE="threetier"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
warn() { printf '\n\033[1;33m[WARNING] %s\033[0m\n' "$*"; }
die()  { printf '\n\033[1;31m[ERROR] %s\033[0m\n' "$*" >&2; exit 1; }

# --------------------------------------------------------------------
# 1. Preflight
# --------------------------------------------------------------------
command -v kubectl >/dev/null 2>&1 || die "kubectl not found. Install it first."
command -v helm   >/dev/null 2>&1 || die "helm not found. See the README for install steps."

kubectl cluster-info >/dev/null 2>&1 || die "No reachable cluster. Start minikube (minikube start) or k3s."
log "Cluster reachable: $(kubectl config current-context)"

# --------------------------------------------------------------------
# 2. Build images locally
# --------------------------------------------------------------------
# The manifests reference threetier-backend:latest and threetier-frontend:latest.
# On a local cluster the image must exist inside that cluster's runtime, not
# just in the local Docker daemon.
log "Building container images"
docker build -t threetier-backend:latest "${REPO_DIR}/backend"
docker build -t threetier-frontend:latest "${REPO_DIR}/frontend"

# Load into whichever cluster is running
if kubectl config current-context | grep -qi minikube; then
    log "Loading images into minikube"
    minikube image load threetier-backend:latest
    minikube image load threetier-frontend:latest
elif kubectl config current-context | grep -qi k3s; then
    log "Importing images into k3s (containerd)"
    # k3s uses containerd, not Docker, so images must be saved and imported
    docker save threetier-backend:latest   | sudo k3s ctr images import -
    docker save threetier-frontend:latest | sudo k3s ctr images import -
else
    warn "Unknown cluster type. If pods show ImagePullBackOff, push the"
    warn "images to a registry and set image.repository in the chart."
fi

# --------------------------------------------------------------------
# 3. Validate the Helm chart before touching the cluster
# --------------------------------------------------------------------
log "Linting the Helm chart"
helm lint "$CHART_DIR" || die "Chart failed lint."

# --------------------------------------------------------------------
# 4. Deploy with Helm
# --------------------------------------------------------------------
log "Deploying release '$RELEASE' with Helm"
helm upgrade --install "$RELEASE" "$CHART_DIR" \
    --namespace default \
    --set image.repository=threetier-backend \
    --set image.tag=latest \
    --wait --timeout 5m

# --------------------------------------------------------------------
# 5. Wait for everything to settle
# --------------------------------------------------------------------
log "Waiting for all pods to become Ready"
for i in $(seq 1 60); do
    NOT_READY=$(kubectl get pods -l 'app.kubernetes.io/instance='"$RELEASE" \
        --no-headers 2>/dev/null | grep -cv ' Running\| Completed' || true)
    TOTAL=$(kubectl get pods -l 'app.kubernetes.io/instance='"$RELEASE" \
        --no-headers 2>/dev/null | wc -l | tr -d ' ')

    if [ "$TOTAL" -gt 0 ] && [ "$NOT_READY" -eq 0 ]; then
        echo "    All $TOTAL pods are ready."
        break
    fi
    [ $((i % 4)) -eq 0 ] && echo "    waiting... ($((i * 5))s, $NOT_READY of $TOTAL not ready)"
    sleep 5
done

# --------------------------------------------------------------------
# 6. Report
# --------------------------------------------------------------------
log "Pod status"
kubectl get pods -o wide
echo ""
kubectl get svc
echo ""
kubectl get hpa
echo ""
helm list
echo ""

NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}' 2>/dev/null || echo "127.0.0.1")
NODE_PORT=$(kubectl get svc "$RELEASE-frontend" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "?")

cat <<EOF
============================================================
  Deployed
============================================================

  Frontend    http://${NODE_IP}:${NODE_PORT}
  API         kubectl port-forward svc/${RELEASE}-backend 8080:8080
              then: curl http://localhost:8080/api/items

  To access the API without port-forward, use:
    kubectl port-forward svc/${RELEASE}-backend 8080:8080

  Useful commands:
    kubectl get pods -w
    kubectl logs -f -l app.kubernetes.io/instance=${RELEASE}
    kubectl describe pod <name>
    helm history ${RELEASE}
    helm rollback ${RELEASE} 1
    helm uninstall ${RELEASE}

============================================================

EOF

warn "If pods are not Running, run: kubectl describe pod <name> | tail -30"
warn "If ImagePullBackOff, the image is not in the cluster runtime. See step 2."
