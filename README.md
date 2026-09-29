# Three-Tier Web Application on AWS

A React frontend, Spring Boot backend, and MySQL database deployed on AWS using
Terraform, GitHub Actions CI/CD, Docker, and Kubernetes with Helm.

Built as a DevOps practice project to demonstrate deployment automation,
infrastructure as code, containerization, and monitoring.

![App running](screenshots/app-running.png)

---

## Architecture

```
                      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                      â”‚        GitHub            â”‚
                      â”‚   Actions (CI/CD)        â”‚
                      â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                                   â”‚ build â†’ test â†’ scan â†’ push
                                   â–¼
                      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                      â”‚   Amazon ECR             â”‚
                      â”‚  (container images)      â”‚
                      â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                                   â”‚ pull
              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
              â”‚            AWS EC2 / EKS                 â”‚
              â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”‚
              â”‚  â”‚  Frontend   React + Nginx   :3000   â”‚  â”‚
              â”‚  â”‚      â”‚                              â”‚  â”‚
              â”‚  â”‚      â”‚  /api  reverse proxy         â”‚  â”‚
              â”‚  â”‚      â–¼                              â”‚  â”‚
              â”‚  â”‚  Backend  Spring Boot + Tomcat:8080 â”‚  â”‚
              â”‚  â”‚      â”‚  JDBC                         â”‚  â”‚
              â”‚  â””â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚
              â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                        â–¼
              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
              â”‚   MySQL / Amazon RDS     â”‚
              â”‚          :3306           â”‚
              â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜

  Monitoring:  Spring Boot Actuator â†’ Prometheus â†’ Grafana
  Alerting:    CloudWatch alarms â†’ SNS email notifications
```

### Request flow

1. Browser loads the React build from Nginx on port 3000
2. Nginx serves the static files and proxies `/api/*` to the backend
3. Backend reaches MySQL over JDBC
4. Backend exposes `/actuator/prometheus`, which Prometheus scrapes every 15s
5. Grafana visualises request rate, latency, and error percentage
6. CloudWatch alarms notify via SNS when CPU is high or an instance is unhealthy

---

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Nginx, multi-stage Docker build |
| Backend | Java 17, Spring Boot 3, Apache Tomcat, Maven |
| Database | MySQL 8, JDBC / Spring Data JPA |
| Infrastructure as Code | Terraform (VPC, EC2, security groups, S3) |
| CI/CD | GitHub Actions, Maven, Trivy, OWASP Dependency-Check |
| Containers | Docker, Docker Compose, Amazon ECR |
| Orchestration | Kubernetes, Helm |
| Monitoring | Prometheus, Grafana, Spring Boot Actuator, CloudWatch |

---

## Repository layout

```
.
â”œâ”€â”€ backend/                  Spring Boot REST API
â”‚   â”œâ”€â”€ Dockerfile            Multi-stage build â†’ Tomcat runtime
â”‚   â”œâ”€â”€ pom.xml
â”‚   â””â”€â”€ src/                  Application source
â”‚
â”œâ”€â”€ frontend/                 React SPA
â”‚   â”œâ”€â”€ Dockerfile            Node build â†’ nginx:alpine runtime
â”‚   â”œâ”€â”€ nginx.conf            Static serving + /api reverse proxy
â”‚   â””â”€â”€ src/                  React source
â”‚
â”œâ”€â”€ database/
â”‚   â””â”€â”€ init.sql              Schema and seed data
â”‚
â”œâ”€â”€ terraform/
â”‚   â”œâ”€â”€ modules/              Reusable modules: ec2, frontend, backend, SGs
â”‚   â””â”€â”€ environments/dev/     dev environment configuration
â”‚
â”œâ”€â”€ k8s/                      Plain Kubernetes manifests
â”‚   â”œâ”€â”€ backend.yaml          Deployment, Service, ConfigMap, Secret, HPA
â”‚   â”œâ”€â”€ frontend.yaml         Deployment, Service, Ingress
â”‚   â””â”€â”€ mysql.yaml            StatefulSet with PersistentVolumeClaim
â”‚
â”œâ”€â”€ helm/threetier-app/       Helm chart packaging the same app
â”‚   â”œâ”€â”€ Chart.yaml
â”‚   â”œâ”€â”€ values.yaml
â”‚   â””â”€â”€ templates/            Deployment, Service, HPA, Ingress, StatefulSet
â”‚
â”œâ”€â”€ monitoring/
â”‚   â”œâ”€â”€ prometheus.yml        Scrape config for Actuator endpoint
â”‚   â””â”€â”€ grafana/provisioning/ Datasource auto-configuration
â”‚
â”œâ”€â”€ deploy/                   Ubuntu setup and Nginx configuration
â”‚
â”œâ”€â”€ screenshots/              Images used in this README
â”‚
â”œâ”€â”€ .github/workflows/
â”‚   â”œâ”€â”€ ci-cd.yml             Build, test, package, deploy
â”‚   â”œâ”€â”€ frontend-cd.yml       Frontend build and deploy
â”‚   â””â”€â”€ terraform.yml         Terraform plan / validate / apply
â”‚
â”œâ”€â”€ docker-compose.yml                  App stack
â”œâ”€â”€ docker-compose.monitoring.yml       Prometheus + Grafana + node_exporter
â””â”€â”€ README.md
```

---

## Getting started locally

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+
- Docker Desktop

### Option A: run everything with Docker (recommended)

```bash
# Build and start frontend, backend, and database
docker compose up --build -d

# Check that all three containers are running
docker compose ps

# View logs
docker compose logs -f backend
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Health check | http://localhost:8080/actuator/health |

Verify the API:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/items
```

### Option B: run on the host

```bash
# Database
mysql -u root -p < database/init.sql

# Backend
cd backend
mvn clean package
mvn spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
npm start
```

---

## Deployment stages

This project was deployed in three stages, each one removing manual work from
the previous stage.

### 1. Manual deployment

Everything done by hand on the EC2 instance.

```bash
# Build locally
mvn clean package

# Copy the WAR to the server
scp target/backend-1.0.0.war ec2-user@<ip>:/opt/tomcat/webapps/

# Restart Tomcat
sudo systemctl restart tomcat

# Copy the React build
scp -r frontend/build/* ec2-user@<ip>:/var/www/html/

# Copy Nginx config and reload
scp deploy/nginx-three-tier.conf ec2-user@<ip>:/etc/nginx/conf.d/
sudo nginx -s reload
```

### 2. Scripted deployment

The same steps wrapped in a script, so the release is repeatable.

```bash
./deploy/deploy-backend.sh
```

The script builds, transfers, deploys, restarts Tomcat, and verifies the health
endpoint before reporting success.

### 3. Automated CI/CD

Every push to `main` runs the full pipeline with no manual step.

![CI pipeline success](screenshots/ci-pipeline-success.png)

`.github/workflows/ci-cd.yml` performs:

1. Source checkout
2. Java 17 setup
3. `mvn clean package` with unit tests
4. OWASP Dependency-Check for vulnerable dependencies
5. Trivy scan of the built image
6. Docker build and push to Amazon ECR
7. Copy the WAR to the EC2 instance over SSH
8. Restart Tomcat via systemd
9. Health check against the live API

Secrets â€” SSH private key, EC2 host, database credentials â€” are stored in
**GitHub Secrets**, never in the repository or in source code.

---

## Infrastructure as Code

Terraform provisions the AWS environment.

```
terraform/
â”œâ”€â”€ modules/
â”‚   â”œâ”€â”€ ec2/                    Instance, key pair, user data
â”‚   â”œâ”€â”€ frontend/               Frontend server
â”‚   â”œâ”€â”€ backend/                Backend server
â”‚   â”œâ”€â”€ security-group/         Individual security groups
â”‚   â””â”€â”€ application-security-groups/  Shared rule sets
â””â”€â”€ environments/dev/           dev-specific values and outputs
```

Usage:

```bash
cd terraform/environments/dev

terraform init     # download providers
terraform validate # check syntax
terraform plan     # preview changes
terraform apply    # create resources
```

![Terraform plan output](screenshots/terraform-plan.png)

Importing existing resources into state, so infrastructure created by hand comes
under management and drift is eliminated:

```bash
terraform import aws_instance.backend i-0abc123def456
```

---

## Containerization

Both services use multi-stage builds, so build tools and dependencies stay out
of the runtime image.

**Backend** â€” Maven build stage, then a Tomcat runtime stage running as a
non-root user with a health check:

```bash
cd backend
docker build -t threetier-backend:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/devops_practice_db \
  -e DB_USERNAME=root -e DB_PASSWORD=root \
  threetier-backend:latest
```

**Frontend** â€” Node build stage, then `nginx:alpine` serving only the static
output:

```bash
cd frontend
docker build --build-arg REACT_APP_API_URL=http://localhost:8080/api -t threetier-frontend:latest .
docker run -p 3000:80 threetier-frontend:latest
```

Check the image size difference this makes:

```bash
docker images | grep threetier
```

---

## Kubernetes

### Prerequisites

| Tool | Purpose |
|---|---|
| Docker | builds the images and runs the cluster |
| kubectl | talks to the cluster |
| Helm | packages the app as a versioned release |
| minikube *or* k3s | the local cluster itself |

Start a cluster â€” pick one:

```bash
# minikube (requires Docker Desktop running)
minikube start

# k3s on an EC2 instance (lighter, roughly 500 MB)
curl -sfL https://get.k3s.io | sh -
sudo k3s kubectl get nodes
```

### Deploy everything with one command

```bash
chmod +x deploy/deploy-k8s.sh
./deploy/deploy-k8s.sh
```

The script builds both images, loads them into the cluster, lints the chart,
deploys with `--wait`, and reports the Service and HPA status.

### Deploy the plain manifests instead

Manifests are numbered because apply order matters: the ConfigMap and Secret
must exist before the workloads that reference them.

```bash
kubectl apply -f k8s/01-config.yaml    # ConfigMap, Secret, database schema
kubectl apply -f k8s/02-mysql.yaml     # StatefulSet + Service
kubectl apply -f k8s/03-backend.yaml   # Deployment, Service, HPA
kubectl apply -f k8s/04-frontend.yaml  # Deployment, NodePort Service
```

`05-ingress-aws.yaml` is kept separate on purpose. An Ingress with
`ingressClassName: alb` does nothing without the AWS Load Balancer Controller
installed, so it only makes sense on EKS.

### Check it

```bash
kubectl get pods -o wide
kubectl get svc
kubectl get hpa
```

![Kubernetes pods running](screenshots/k8s-pods-running.png)

Reach the API:

```bash
kubectl port-forward svc/threetier-backend 8080:8080
curl http://localhost:8080/api/items
```

### Helm directly

```bash
helm lint ./helm/threetier-app
helm template threetier ./helm/threetier-app      # preview the rendered YAML
helm install threetier ./helm/threetier-app --wait
helm upgrade threetier ./helm/threetier-app --set image.tag=v1.1.0
helm history threetier
helm rollback threetier 1
```

![Helm release history](screenshots/helm-history.png)

### Chart values

| Value | Default | Purpose |
|---|---|---|
| `replicaCount` | 2 | Backend replicas |
| `image.repository` | `threetier-backend` | Image to deploy |
| `image.tag` | `latest` | Image tag |
| `autoscaling.enabled` | `true` | Create an HPA |
| `autoscaling.maxReplicas` | 6 | Upper scaling bound |
| `startupProbe.failureThreshold` | 30 | Allows 5 minutes to boot |
| `livenessProbe.path` | `/actuator/health/liveness` | Liveness endpoint |
| `readinessProbe.path` | `/actuator/health/readiness` | Readiness endpoint |
| `database.seedSchema` | `true` | Create the schema on first start |
| `ingress.enabled` | `false` | Create an Ingress (EKS only) |

### Design decisions worth explaining

**Three probes, not one.** Spring Boot needs 30 to 60 seconds to boot. A
liveness probe that begins at 10 seconds kills the container during that window,
so the pod restarts forever. The startup probe holds liveness off until the
application answers, and only then does liveness begin. The same problem shows
up in the Dockerfile `HEALTHCHECK`, which is why its `start-period` is 90
seconds.

**Liveness and readiness ask different questions.** Liveness asks "is this
process wedged?" and triggers a restart. Readiness asks "should this pod
receive traffic?" and only removes it from the Service. The readiness health
group includes the database check, so a pod that cannot reach MySQL stops
taking requests instead of returning errors to users.

**MySQL runs as a StatefulSet.** It needs a PersistentVolumeClaim so data
survives a restart, which a Deployment with an emptyDir cannot provide.

**The database Service is a ClusterIP, not headless.** A headless service
(`clusterIP: None`) resolves directly to the pod IP, so the connection breaks
whenever the pod is recreated. A ClusterIP is a stable virtual IP that
kube-proxy load-balances across pods.

**`maxUnavailable: 0`.** During a rolling update the old pod keeps serving
until its replacement passes readiness, so a rollout has no downtime.

**Secrets are placeholders.** In a real environment they come from AWS Secrets
Manager through the External Secrets Operator, so no password is stored in git.

---

## Monitoring

### Prometheus and Grafana locally

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

| Tool | URL | Credentials |
|---|---|---|
| Prometheus | http://localhost:9090 | none |
| Grafana | http://localhost:3000 | admin / admin |

![Grafana dashboard](screenshots/grafana-dashboard.png)

The Spring Boot backend exposes metrics at `/actuator/prometheus`. Prometheus
scrapes it every 15 seconds. Useful queries:

```promql
# Request rate per second
rate(http_server_requests_seconds_count[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### CloudWatch alerting in AWS

CloudWatch alarms on high CPU and unhealthy instance status, with SNS sending
email notifications.

![CloudWatch alarm](screenshots/cloudwatch-alarm.png)

---

## API reference

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Health check |
| GET | `/api/items` | List all items |
| POST | `/api/items` | Create an item |
| DELETE | `/api/items/{id}` | Delete an item |
| GET | `/actuator/health` | Application health |
| GET | `/actuator/prometheus` | Prometheus metrics |

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/items
curl -X POST http://localhost:8080/api/items \
  -H "Content-Type: application/json" \
  -d '{"name":"Learn load balancing"}'
```

---

## Security notes

- No credentials are committed. Database settings come from environment
  variables, and CI secrets live in GitHub Secrets.
- `.gitignore` excludes `*.pem`, `*.key`, `*.p12`, `*.env`, and `*.tfvars`
- `*.tfvars` is ignored, but `example.tfvars` is committed as a template
- Container images are scanned with Trivy before deployment
- Dependencies are checked with OWASP Dependency-Check in CI
- Containers run as non-root with dropped Linux capabilities
- The Helm chart ships a placeholder secret, which a real deployment replaces
  with AWS Secrets Manager via the External Secrets Operator
