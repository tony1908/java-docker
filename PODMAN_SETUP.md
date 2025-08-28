# Running Services with Podman Pod

This guide explains how to run the current Docker Compose services (Java application + MariaDB) in a Podman pod.

## Prerequisites

- Podman installed
- Maven (for building the Java application)

## Current Services Overview

The Docker Compose setup includes:
- **app**: Java application (built from Dockerfile)
- **mariadb**: MariaDB database with health checks

## Podman Pod Setup

### 1. Create a Pod

```bash
podman pod create --name javadb-pod -p 3306:3306
```

### 2. Create MariaDB Volume

```bash
podman volume create mariadb-data
```

### 3. Run MariaDB Container in Pod

```bash
podman run -d \
  --pod javadb-pod \
  --name mariadb-container \
  -e MARIADB_ROOT_PASSWORD=rootpassword \
  -e MARIADB_DATABASE=myapp \
  -e MARIADB_USER=appuser \
  -e MARIADB_PASSWORD=apppassword \
  -v mariadb-data:/var/lib/mysql \
  --health-cmd="mariadb-admin ping -h 127.0.0.1 -uroot -p\$MARIADB_ROOT_PASSWORD --silent" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-retries=2 \
  mariadb
```

### 4. Build Java Application Image

```bash
podman build -t java-app .
```

### 5. Wait for MariaDB to be Healthy

```bash
# Check health status
podman healthcheck run mariadb-container

# Wait until healthy (may take a few attempts)
while [ "$(podman inspect mariadb-container --format='{{.State.Health.Status}}')" != "healthy" ]; do
  echo "Waiting for MariaDB to be healthy..."
  sleep 5
done
```

### 6. Run Java Application Container in Pod

```bash
podman run -d \
  --pod javadb-pod \
  --name java-app-container \
  java-app
```

## Managing the Pod

### Start the Pod
```bash
podman pod start javadb-pod
```

### Stop the Pod
```bash
podman pod stop javadb-pod
```

### Remove the Pod
```bash
podman pod rm javadb-pod
```

### Check Pod Status
```bash
podman pod ps
```

### Check Container Logs
```bash
# MariaDB logs
podman logs mariadb-container

# Java app logs
podman logs java-app-container
```

## Alternative: Using Podman Compose

You can also use `podman-compose` with the existing `docker-compose.yaml` file:

```bash
# Install podman-compose if not already installed
pip3 install podman-compose

# Run with podman-compose
podman-compose up -d
```

## Networking

In the pod setup, containers share the same network namespace, so:
- The Java app can connect to MariaDB using `localhost:3306`
- External access to MariaDB is available on the host's port 3306

## Troubleshooting

1. **MariaDB not starting**: Check logs with `podman logs mariadb-container`
2. **Java app can't connect**: Ensure MariaDB is healthy before starting the app
3. **Permission issues**: Make sure the volume has proper permissions

## Cleanup

To completely remove everything:

```bash
podman pod rm -f javadb-pod
podman volume rm mariadb-data
podman rmi java-app
```