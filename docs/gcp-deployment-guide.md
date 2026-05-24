# Search Microservice — Google Cloud Deployment Guide

## Overview

This guide covers deploying the Search Microservice to **Google Cloud Platform (GCP)** with all its dependencies (Elasticsearch, Redis, RabbitMQ, Kong, Keycloak) using **Compute Engine + Docker Compose** — the simplest lift-and-shift approach.

---

## Architecture on GCP

```
                            Internet
                               │
                               ▼
                    ┌─────────────────────┐
                    │  External Static IP │
                    │  (reserved)         │
                    └──────────┬──────────┘
                               │ :8000
                               ▼
              ┌──────────────────────────────────┐
              │     Compute Engine VM             │
              │     e2-standard-4 (4 vCPU, 16 GB) │
              │     100 GB SSD Persistent Disk    │
              │     Ubuntu 22.04 LTS              │
              │                                   │
              │  ┌─────────────────────────────┐ │
              │  │   Docker Compose Stack       │ │
              │  │                              │ │
              │  │  Kong (8000) ← API Gateway   │ │
              │  │  Keycloak (8081) ← Auth      │ │
              │  │  Search API (8085)            │ │
              │  │  Elasticsearch (9200)         │ │
              │  │  Redis (6379)                 │ │
              │  │  RabbitMQ (5672)              │ │
              │  │  Prometheus (9090)            │ │
              │  │  Grafana (3000)              │ │
              │  └─────────────────────────────┘ │
              └──────────────────────────────────┘
```

---

## Prerequisites

- A Google Cloud account with billing enabled
- `gcloud` CLI installed on your local machine
- GitHub repository access (to clone the code)

---

## Step 1: Install Google Cloud CLI

```bash
# Install gcloud (Linux/macOS)
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Or on Windows, download from:
# https://cloud.google.com/sdk/docs/install
```

Authenticate and set your project:

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud config set compute/region us-central1
gcloud config set compute/zone us-central1-a
```

> If you don't have a project yet, create one at https://console.cloud.google.com

---

## Step 2: Enable Required APIs

```bash
gcloud services enable compute.googleapis.com
gcloud services enable cloudresourcemanager.googleapis.com
```

---

## Step 3: Create Firewall Rules

The VM needs these ports open to the internet:

| Port | Service | Purpose |
|------|---------|---------|
| 8000 | Kong | API Gateway (external access) |
| 22 | SSH | Remote access |

Optional ports (restrict to your IP for security):

| Port | Service | Purpose |
|------|---------|---------|
| 3000 | Grafana | Dashboards |
| 15672 | RabbitMQ | Management UI |
| 8081 | Keycloak | Admin console |
| 9090 | Prometheus | Metrics |

```bash
# API Gateway (public)
gcloud compute firewall-rules create allow-kong-api \
    --direction=INGRESS \
    --priority=1000 \
    --network=default \
    --action=ALLOW \
    --rules=tcp:8000 \
    --source-ranges=0.0.0.0/0 \
    --target-tags=search-microservice

# SSH
gcloud compute firewall-rules create allow-ssh \
    --direction=INGRESS \
    --priority=1000 \
    --network=default \
    --action=ALLOW \
    --rules=tcp:22 \
    --source-ranges=0.0.0.0/0 \
    --target-tags=search-microservice

# Grafana (restrict to your IP for security)
gcloud compute firewall-rules create allow-grafana \
    --direction=INGRESS \
    --priority=1000 \
    --network=default \
    --action=ALLOW \
    --rules=tcp:3000 \
    --source-ranges=YOUR_PUBLIC_IP/32 \
    --target-tags=search-microservice
```

> Replace `YOUR_PUBLIC_IP/32` with your actual public IP. Find it at https://ifconfig.me

---

## Step 4: Create and Configure the VM

```bash
gcloud compute instances create search-microservice-vm \
    --machine-type=e2-standard-4 \
    --boot-disk-size=100GB \
    --boot-disk-type=pd-ssd \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud \
    --tags=search-microservice \
    --metadata=startup-script='#! /bin/bash
# System tuning for Elasticsearch
sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" >> /etc/sysctl.conf

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker $USER

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
'
```

**Machine type reasoning:**
- `e2-standard-4` (4 vCPU, 16 GB RAM) — Elasticsearch needs at least 512 MB heap for a single-node dev setup. The extra RAM handles Redis, RabbitMQ, Kong, Keycloak, and the Java app.
- 100 GB SSD — Elasticsearch indices and Redis persistence grow over time. SSD is critical for ES performance.

> **Cost estimate**: ~$150–180/month with sustained use discounts. For a dev/student setup, you can use `e2-standard-2` (2 vCPU, 8 GB) and 50 GB disk (~$70/month), but ES may be slower.

---

## Step 5: Reserve a Static External IP

```bash
gcloud compute addresses create search-microservice-ip \
    --region=us-central1

# Get the reserved IP
gcloud compute addresses describe search-microservice-ip \
    --region=us-central1 \
    --format="value(address)"

# Attach it to the VM
gcloud compute instances delete-access-config search-microservice-vm \
    --access-config-name="external-nat"

gcloud compute instances add-access-config search-microservice-vm \
    --access-config-name="external-nat" \
    --address=$(gcloud compute addresses describe search-microservice-ip \
        --region=us-central1 --format="value(address)")
```

---

## Step 6: SSH Into the VM and Clone the Repo

```bash
gcloud compute ssh search-microservice-vm
```

Once inside the VM:

```bash
# Ensure Docker is running
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
newgrp docker

# Verify vm.max_map_count (Elasticsearch requirement)
sysctl vm.max_map_count
# Should output: vm.max_map_count = 262144

# Clone the repository
git clone https://github.com/AdrianCCRS/search-microservice.git
cd search-microservice

# Switch to the deployment branch
git checkout deployment
```

---

## Step 7: Configure Environment Variables

```bash
cd deploy

# Copy the example env file
cp .env.example .env

# Generate secure passwords
REDIS_PASS=$(openssl rand -base64 32)
RABBIT_PASS=$(openssl rand -base64 16)
KEYCLOAK_PASS=$(openssl rand -base64 16)

# Edit .env with your values
nano .env
```

**Minimum required changes in `.env`:**

```bash
REDIS_PASSWORD=<your-generated-redis-pass>
RABBITMQ_USER=admin
RABBITMQ_PASS=<your-generated-rabbit-pass>
KEYCLOAK_ADMIN_PASSWORD=<your-generated-keycloak-pass>
GRAFANA_ADMIN_PASSWORD=<your-grafana-pass>
```

> Do NOT commit the `.env` file — it's already in `.gitignore`.

---

## Step 8: Build and Start the Stack

```bash
# Build the search-api image
docker compose -f deploy/docker-compose.yml build

# Start all services (detached mode)
docker compose -f deploy/docker-compose.yml up -d

# Watch the logs to confirm startup
docker compose -f deploy/docker-compose.yml logs -f search-api
```

**What happens during startup:**

1. Infrastructure starts: Redis, Elasticsearch, RabbitMQ, Kong, Keycloak
2. Each service runs its healthcheck until healthy
3. `search-api` waits for all dependencies to pass healthchecks
4. `wait-for-services.sh` polls TCP ports for Redis (6379), ES (9200), RabbitMQ (5672)
5. Once all reachable, Spring Boot starts
6. If Java crashes, Docker restarts it up to 5 times (`restart: on-failure:5`)
7. After 90s, the healthcheck begins polling `/actuator/health`
8. Kong, Keycloak, Prometheus, and Grafana start in parallel

**Expected startup time:** 3–5 minutes (first run downloads images).

---

## Step 9: Verify the Deployment

```bash
# Check all containers are healthy
docker compose -f deploy/docker-compose.yml ps

# Test the search endpoint through Kong
curl -X GET "http://localhost:8000/api/search?q=laptop"

# Test the health endpoint directly
curl http://localhost:8085/actuator/health

# Check Elasticsearch
curl http://localhost:9200/_cluster/health

# Check Redis
docker exec search_redis redis-cli -a $REDIS_PASSWORD ping

# Check RabbitMQ
docker exec search_rabbitmq rabbitmq-diagnostics ping
```

**Access dashboards** (from your browser using the static IP):

| Service | URL | Credentials |
|---------|-----|-------------|
| Kong Gateway | `http://<STATIC_IP>:8000` | JWT via Keycloak |
| RabbitMQ UI | `http://<STATIC_IP>:15672` | admin / your-pass |
| Grafana | `http://<STATIC_IP>:3000` | admin / your-pass |
| Keycloak | `http://<STATIC_IP>:8081` | admin / your-pass |

> If you can't reach these, verify the firewall rules in Step 3 include your IP.

---

## Step 10: Seed the Search Index (Optional)

If Elasticsearch is empty, you can manually index test data:

```bash
# Index a sample product directly via curl
curl -X PUT "http://localhost:9200/products/_doc/test-1" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "test-1",
    "name": "Sample Product",
    "description": "A test product for verification",
    "category": "electronics",
    "price": 99.99,
    "rating": 4.5,
    "available": true,
    "brand": "TestBrand"
  }'

# Verify it's searchable
curl "http://localhost:9200/products/_search?q=sample"
```

In production, the index is populated automatically by consuming `ProductCreatedEvent` messages from RabbitMQ, published by the Catalog service.

---

## Step 11: Basic Monitoring

```bash
# View resource usage
docker stats --no-stream

# Check disk space
df -h

# View application logs
docker compose -f deploy/docker-compose.yml logs -f --tail=100 search-api

# Check ES health
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_cat/nodes?v
```

Grafana at `http://<STATIC_IP>:3000` comes pre-provisioned with:
- Search API metrics (latency, cache hit rate, indexing duration)
- Elasticsearch cluster health

---

## Step 12: Set Up Automatic Restart on VM Reboot

```bash
# The compose services already have restart: always or restart: unless-stopped.
# Ensure Docker starts on boot:
sudo systemctl enable docker

# Create a systemd service to auto-start the compose stack on boot:
sudo tee /etc/systemd/system/search-microservice.service << 'EOF'
[Unit]
Description=Search Microservice Stack
Requires=docker.service
After=docker.service network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/$USER/search-microservice
ExecStart=/usr/local/bin/docker-compose -f deploy/docker-compose.yml up -d
ExecStop=/usr/local/bin/docker-compose -f deploy/docker-compose.yml down
User=$USER
Group=docker

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable search-microservice
```

Now the stack will survive VM reboots.

---

## Cleanup (When No Longer Needed)

```bash
# Stop the stack
docker compose -f deploy/docker-compose.yml down -v

# Delete the VM
gcloud compute instances delete search-microservice-vm --zone=us-central1-a

# Release the static IP
gcloud compute addresses delete search-microservice-ip --region=us-central1

# Delete firewall rules
gcloud compute firewall-rules delete allow-kong-api allow-grafana
```

---

## Alternative: Cloud Run + Managed Services

For a more production-grade, lower-ops approach:

| Component | Google Cloud Service |
|-----------|---------------------|
| Search API (Java) | **Cloud Run** — serverless, scales to zero |
| Redis Cache | **Memorystore for Redis** — fully managed |
| RabbitMQ | **Cloud Run** (RabbitMQ container) or **GCE VM** |
| Elasticsearch | **Elastic Cloud on GCP** or self-hosted on **GCE** |
| Kong | **Cloud Run** or **Apigee** (GCP native API gateway) |
| Keycloak | **Cloud Run** |

This approach requires:
1. Building and pushing the Docker image to **Artifact Registry**
2. Deploying to Cloud Run with VPC connector for reaching Memorystore
3. Managing secrets via **Secret Manager**
4. Configuring environment variables in Cloud Run

Ask if you need a separate guide for this approach.

---

## Troubleshooting

### "Elasticsearch won't start — max virtual memory too low"

```bash
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
docker compose -f deploy/docker-compose.yml restart elasticsearch
```

### "Spring Boot crashes on startup — Connection refused"

The `wait-for-services.sh` script handles this by polling TCP ports before launching Java. If it still fails:
```bash
# Check each service individually
docker exec search_redis redis-cli -a $REDIS_PASSWORD ping
docker exec search_rabbitmq rabbitmq-diagnostics ping
curl http://localhost:9200

# Force restart the search-api
docker compose -f deploy/docker-compose.yml restart search-api
```

### "Port already in use"

```bash
# Find what's using the port
sudo lsof -i :8000
sudo lsof -i :9200

# Kill the process or change the port in .env
```

### "Out of disk space"

```bash
# Clean up old Docker images and volumes
docker system prune -a --volumes -f

# Or increase disk size in GCP Console (resize persistent disk)
```
