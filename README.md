# Containerized Application Deployment with Kubernetes

A React + Spring Boot + MySQL three-tier application packaged into containers and
deployed to a Kubernetes cluster, using plain manifests and a Helm chart.

---

## What this demonstrates

| Area | Detail |
|---|---|
| Containerisation | Multi-stage Dockerfiles — Maven to Tomcat, Node.js to nginx:alpine |
| Kubernetes | Deployment, StatefulSet, Service, ConfigMap, Secret, HPA, PVC |
| Health management | Separate startup, liveness, and readiness probes |
| Packaging | Helm chart with install, upgrade, rollback, and values externalisation |
| Automation | One script that builds, loads, lints, deploys, and verifies |
| Security | Non-root containers, dropped capabilities, secrets from a Secret object |

---

## Architecture

```
                    ┌──────────────────────────────┐
                    │        Browser                │
                    └───────────────┬──────────────┘
                                    │ :30000 (NodePort)
                    ┌───────────────▼──────────────┐
                    │  frontend  Deployment (2)     │
                    │  nginx:alpine, static React   │
                    └───────────────┬──────────────┘
                                    │ /api
                    ┌───────────────▼──────────────┐
                    │  backend   Deployment (2)     │
                    │  Tomcat + Spring Boot :8080   │
                    │  startup + liveness +         │
                    │  readiness probes             │
                    └───────────────┬──────────────┘
                                    │ JDBC
                    ┌───────────────▼──────────────┐
                    │  mysql     StatefulSet (1)    │
                    │  PersistentVolumeClaim 5 Gi   │
                    │  ClusterIP :3306              │
                    └──────────────────────────────┘

  Config:  ConfigMap (backend-config)   Secrets: Secret (backend-secrets)
  Scale:   HorizontalPodAutoscaler, 2-6 replicas at 70% CPU
```

Two backend replicas sit behind a ClusterIP Service. The frontend proxies `/api`
to the backend, so a single NodePort entry point serves the whole application.

---

## Prerequisites

| Tool | Purpose |
|---|---|
| Docker | builds the images |
| kubectl | talks to the cluster |
| Helm | installs the chart |
| a cluster | minikube, k3s, kind, or EKS |

Start a local cluster, either way:

```bash
# minikube — needs Docker Desktop running
minikube start

# k3s — lighter, about 500 MB, good on a small EC2 instance
curl -sfL https://get.k3s.io | sh -
sudo k3s kubectl get nodes
```

---

## Quick start

One command builds the images, loads them into the cluster, lints the chart,
deploys it, and waits for everything to become ready.

```bash
chmod +x deploy/deploy-k8s.sh
./deploy/deploy-k8s.sh
```

Expect roughly two to four minutes on the first run, most of it the Maven build.

### Or deploy the manifests directly

Files are numbered because apply order matters: the ConfigMap and Secret must
exist before the workloads that reference them.

```bash
kubectl apply -f k8s/01-config.yaml    # ConfigMap, Secret, database schema
kubectl apply -f k8s/02-mysql.yaml     # StatefulSet + Service
kubectl apply -f k8s/03-backend.yaml   # Deployment, Service, HPA
kubectl apply -f k8s/04-frontend.yaml  # Deployment, NodePort Service
```

`05-ingress-aws.yaml` is deliberately separate. An Ingress with
`ingressClassName: alb` does nothing without the AWS Load Balancer Controller
installed, so it only applies on EKS.

### Or use Helm directly

```bash
helm lint ./helm/threetier-app
helm template threetier ./helm/threetier-app       # preview rendered YAML
helm install threetier ./helm/threetier-app --wait
helm upgrade threetier ./helm/threetier-app --set image.tag=v1.1.0
helm history threetier
helm rollback threetier 1
```

---

## Verify

```bash
kubectl get pods -o wide        # all Running
kubectl get svc                 # ClusterIP services + the frontend NodePort
kubectl get hpa                 # backend target and current replicas
kubectl describe pod <name>     # probe status, events
```

Reach the application:

```bash
# Frontend
kubectl get svc threetier-frontend
# open http://<node-ip>:<node-port>

# API
kubectl port-forward svc/threetier-backend 8080:8080
curl http://localhost:8080/api/items
```

Prometheus metrics should be scrapable, which is what proves the monitoring
setup works:

```bash
kubectl port-forward svc/threetier-backend 8080:8080
curl http://localhost:8080/actuator/prometheus | head
```

### Screenshots worth capturing

| Image | Command |
|---|---|
| Pods running | `kubectl get pods -o wide` |
| Service and NodePort | `kubectl get svc` |
| HPA target | `kubectl get hpa` |
| Helm history | `helm history threetier` |
| API responding | `curl http://localhost:8080/api/items` |
| Probe detail | `kubectl describe pod <name>` |

---

## Project structure

```
k8s/
├── 01-config.yaml            ConfigMap, Secret, MySQL schema as a ConfigMap
├── 02-mysql.yaml             StatefulSet + ClusterIP Service
├── 03-backend.yaml           Deployment, Service, HorizontalPodAutoscaler
├── 04-frontend.yaml          Deployment + NodePort Service
└── 05-ingress-aws.yaml       ALB Ingress, for EKS only

helm/threetier-app/
├── Chart.yaml
├── values.yaml               All configuration in one place
├── .helmignore
└── templates/
    ├── _helpers.tpl          Reusable named templates
    ├── deployment.yaml       Backend Deployment
    ├── service.yaml          Service, ConfigMap, Secret, ServiceAccount, HPA, Ingress
    └── statefulset-mysql.yaml  MySQL StatefulSet, Service, schema ConfigMap

deploy/
└── deploy-k8s.sh             Build, load, lint, deploy, verify

docs/
└── KUBERNETES_INTERVIEW_PREP.md

backend/                      Spring Boot application (9 classes, 3 test classes)
├── Dockerfile                Multi-stage: Maven build to Tomcat runtime
└── src/main/resources/
    └── application-container.properties   Disables TLS, enables probe groups

frontend/                     React application
├── Dockerfile                Multi-stage: Node build to nginx:alpine
└── nginx.conf                Static serving plus /api reverse proxy

database/init.sql             Same schema, for Docker Compose and MySQL
```

---

## The manifests, explained

### `01-config.yaml`

Holds the three things every other resource depends on:

- **`backend-config` ConfigMap** — `DB_URL`, `PORT`, and
  `SPRING_PROFILES_ACTIVE=container`. The container profile is required: the
  default profile enables SSL with a keystore that is deliberately not
  committed to git, so the application would fail to start without it.
- **`backend-secrets` Secret** — placeholder database credentials. In a real
  environment the External Secrets Operator syncs this from AWS Secrets
  Manager, so no password is ever stored in the repository.
- **`mysql-init` ConfigMap** — the database schema. The MySQL image runs any
  `.sql` file in `/docker-entrypoint-initdb.d` exactly once, on first start of
  an empty volume. Without this the `items` table is never created and the
  backend crashes on its first query.

### `02-mysql.yaml`

A **StatefulSet**, because MySQL needs a PersistentVolumeClaim. A Deployment
with an `emptyDir` would lose all data on every pod restart.

The Service is a normal **ClusterIP**, not headless. A headless Service
(`clusterIP: None`) resolves straight to the pod IP, so the connection would
break whenever the pod is recreated. A ClusterIP is a stable virtual IP that
kube-proxy load-balances across pods.

### `03-backend.yaml`

The Deployment runs two replicas with `maxUnavailable: 0`, so during a rolling
update the old pod keeps serving until its replacement passes readiness. A
rollout has no dropped requests.

**Three probes, each answering a different question:**

| Probe | Question | On failure |
|---|---|---|
| startup | Has the application finished booting? | Keeps waiting, up to 5 minutes |
| liveness | Is the JVM still responsive? | Container is restarted |
| readiness | Should this pod receive traffic? | Removed from the Service, not restarted |

The startup probe exists because Spring Boot needs 30 to 60 seconds to boot. A
liveness probe starting at 10 seconds kills the container mid-boot, and the pod
restarts forever. The same problem appears in the Dockerfile `HEALTHCHECK`,
which is why its start period is 90 seconds.

The readiness group includes the database check, so a pod that cannot reach
MySQL stops taking requests instead of returning errors to users.

### `04-frontend.yaml`

A Deployment plus a **NodePort** Service, so the application is reachable
without installing an Ingress controller. The ALB Ingress lives in a separate
file for EKS.

---

## Helm chart

| Value | Default | Purpose |
|---|---|---|
| `replicaCount` | 2 | Backend replicas |
| `image.repository` | `threetier-backend` | Image to deploy |
| `image.tag` | `latest` | Image tag |
| `autoscaling.enabled` | `true` | Create an HPA |
| `autoscaling.minReplicas` | 2 | Lower bound |
| `autoscaling.maxReplicas` | 6 | Upper bound |
| `autoscaling.targetCPUUtilizationPercentage` | 70 | Scale threshold |
| `startupProbe.failureThreshold` | 30 | Allows 5 minutes to boot |
| `livenessProbe.path` | `/actuator/health/liveness` | Liveness endpoint |
| `readinessProbe.path` | `/actuator/health/readiness` | Readiness endpoint |
| `database.seedSchema` | `true` | Create the schema on first start |
| `database.storage` | 5Gi | Volume size |
| `ingress.enabled` | `false` | Create an Ingress (EKS only) |
| `secrets.*` | placeholder | Replaced by a secret manager in production |

Override at install time:

```bash
helm install threetier ./helm/threetier-app \
  --set replicaCount=3 \
  --set autoscaling.maxReplicas=10 \
  --set image.tag=v2.0.0
```

Or with a file:

```bash
helm install threetier ./helm/threetier-app -f production-values.yaml
```

**`DB_URL` is assembled inside the template**, not in `values.yaml`, because it
contains the release name. The Service the StatefulSet creates is named
`<release-name>-mysql`, so a hardcoded host breaks the moment the chart is
installed under a different name.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `ImagePullBackOff` | Image exists only in the local Docker daemon, not the cluster runtime | `minikube image load <image>`, or `docker save ... \| sudo k3s ctr images import -` |
| `CrashLoopBackOff` | Container starts then exits | `kubectl logs <pod> --previous` |
| Pods `Pending` | No node has room for the resource request, or the PVC is unbound | `kubectl describe pod <pod>` and read the events |
| `Running` but requests return 503 | Passing liveness, failing readiness, so not in the Service endpoints | `kubectl describe pod <pod>` and check the probe error |
| Pods restart repeatedly | Liveness probe too aggressive for a slow JVM boot | The startup probe handles this; confirm it is present |
| `helm install` says release already exists | Name is taken | `helm upgrade --install`, or `helm uninstall` first |
| MySQL restarts and loses data | Volume claim deleted | `kubectl get pvc` — a StatefulSet PVC should persist |

Useful commands:

```bash
kubectl get events --sort-by='.lastTimestamp' | tail -20
kubectl logs -f -l app.kubernetes.io/instance=threetier
kubectl rollout status deployment/backend
kubectl rollout history deployment/backend
kubectl rollout undo deployment/backend
```

---

## Security notes

- Containers run as a **non-root user**. The backend image creates a dedicated
  user, and both Deployments set `runAsNonRoot: true`.
- The Helm chart sets `readOnlyRootFilesystem: true`,
  `allowPrivilegeEscalation: false`, and drops all Linux capabilities.
- Database credentials come from a **Secret**, and the committed value is a
  placeholder. In production, the External Secrets Operator populates it from
  AWS Secrets Manager.
- `runAsUser: 1000` and `fsGroup: 1000` give the process a non-root identity
  without root privileges on the node.
- Containers listen on plain HTTP. TLS is terminated at the Ingress in a cloud
  environment, never by the application itself.

---

## Learn more

`docs/KUBERNETES_INTERVIEW_PREP.md` covers the questions this project raises in
an interview, with answers: cluster architecture, what happens on
`kubectl apply`, probe semantics, Service types, persistent storage, Helm
internals, rolling updates, and the troubleshooting scenarios above.

---

## Related

The same application is also deployed in two other ways in the full repository:

- **Docker Compose** — local development stack matching the production topology
- **AWS EC2** — with Terraform-provisioned infrastructure and a GitHub Actions
  pipeline that builds, scans, deploys over SSH, and rolls back automatically
