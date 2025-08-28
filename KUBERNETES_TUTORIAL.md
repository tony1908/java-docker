# Kubernetes Tutorial: Running Java Application with MariaDB

This tutorial will guide you through deploying your Java application with MariaDB database in a Kubernetes cluster using a dedicated namespace.

## Prerequisites

- Kubernetes cluster running (local or remote)
- kubectl configured to connect to your cluster
- Docker image built for your Java application

## Project Structure

The Kubernetes configuration files are located in the `k8s/` directory:

```
k8s/
├── namespace.yaml           # Creates the javadb-app namespace
├── mariadb-deployment.yaml  # MariaDB database deployment, service, and storage
├── app-deployment.yaml      # Java application deployment and service
├── apply-all.sh            # Script to deploy everything
└── cleanup.sh              # Script to clean up resources
```

## Step 1: Build Your Docker Image

First, build your Java application Docker image:

```bash
docker build -t java-app:latest .
```

**Note**: If you're using Minikube, make sure to use Minikube's Docker daemon:
```bash
eval $(minikube docker-env)
docker build -t java-app:latest .
```

## Step 2: Deploy to Kubernetes

### Option A: Use the automated script (Recommended)

```bash
cd k8s/
./apply-all.sh
```

### Option B: Deploy manually step by step

1. **Create the namespace:**
   ```bash
   kubectl apply -f k8s/namespace.yaml
   ```

2. **Deploy MariaDB:**
   ```bash
   kubectl apply -f k8s/mariadb-deployment.yaml
   ```

3. **Wait for MariaDB to be ready:**
   ```bash
   kubectl wait --for=condition=available --timeout=300s deployment/mariadb -n javadb-app
   ```

4. **Deploy the Java application:**
   ```bash
   kubectl apply -f k8s/app-deployment.yaml
   ```

## Step 3: Verify Deployment

Check that all pods are running:

```bash
kubectl get pods -n javadb-app
```

You should see something like:
```
NAME                         READY   STATUS    RESTARTS   AGE
java-app-xxxxxxxxx-xxxxx     1/1     Running   0          2m
mariadb-xxxxxxxxx-xxxxx      1/1     Running   0          3m
```

Check services:

```bash
kubectl get services -n javadb-app
```

## Step 4: Access Your Application

### Option A: Using port forwarding (Recommended for testing)

```bash
kubectl port-forward service/java-app-service 8080:8080 -n javadb-app
```

Then visit: http://localhost:8080

### Option B: Using NodePort (if running on accessible cluster)

The application is exposed on NodePort 30080. If your cluster nodes are accessible:
- Get your cluster node IP: `kubectl get nodes -o wide`
- Access the application at: `http://<node-ip>:30080`

## Step 5: Monitor and Debug

### View application logs:
```bash
kubectl logs -f deployment/java-app -n javadb-app
```

### View MariaDB logs:
```bash
kubectl logs -f deployment/mariadb -n javadb-app
```

### Get detailed pod information:
```bash
kubectl describe pod <pod-name> -n javadb-app
```

### Execute commands inside pods:
```bash
# Connect to MariaDB
kubectl exec -it deployment/mariadb -n javadb-app -- mariadb -u appuser -p myapp

# Check Java application container
kubectl exec -it deployment/java-app -n javadb-app -- /bin/sh
```

## Configuration Details

### Namespace
- **Name**: `javadb-app`
- **Purpose**: Isolates all resources for this application

### MariaDB Configuration
- **Image**: mariadb:latest
- **Database**: myapp
- **User**: appuser
- **Password**: apppassword
- **Storage**: 1Gi persistent volume
- **Service**: ClusterIP on port 3306

### Java Application Configuration
- **Image**: java-app:latest
- **Environment Variables**:
  - `DB_URL`: jdbc:mariadb://mariadb-service:3306/myapp
  - `DB_USERNAME`: appuser
  - `DB_PASSWORD`: apppassword
  - `SPRING_PROFILES_ACTIVE`: prod
- **Service**: NodePort on port 8080 (NodePort 30080)

## Cleanup

To remove all resources:

```bash
cd k8s/
./cleanup.sh
```

Or manually:

```bash
kubectl delete -f k8s/app-deployment.yaml
kubectl delete -f k8s/mariadb-deployment.yaml
kubectl delete -f k8s/namespace.yaml
```

## Troubleshooting

### Common Issues

1. **Image pull errors**: Make sure your Docker image is available to the cluster
   - For Minikube: Use `eval $(minikube docker-env)` before building
   - For remote clusters: Push image to a registry

2. **Database connection issues**: Check if MariaDB is fully started before the app
   - View MariaDB logs: `kubectl logs deployment/mariadb -n javadb-app`
   - Check service connectivity: `kubectl get svc -n javadb-app`

3. **Application not starting**: Check application logs
   - `kubectl logs deployment/java-app -n javadb-app`

4. **Persistent volume issues**: Ensure your cluster supports dynamic volume provisioning
   - Check PVC status: `kubectl get pvc -n javadb-app`

### Useful Commands

```bash
# Get all resources in the namespace
kubectl get all -n javadb-app

# Describe all resources
kubectl describe all -n javadb-app

# Watch pod status in real-time
kubectl get pods -n javadb-app -w

# Forward database port for direct access
kubectl port-forward service/mariadb-service 3306:3306 -n javadb-app
```

## Next Steps

- **Scaling**: Increase replicas in deployment files
- **Security**: Use Kubernetes secrets for database credentials
- **Monitoring**: Add health checks and metrics
- **Ingress**: Configure ingress controller for external access
- **ConfigMaps**: Externalize application configuration