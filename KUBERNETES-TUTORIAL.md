# Java Application with MariaDB - Kubernetes Deployment Tutorial

This tutorial shows how to deploy a Java application with MariaDB database on Kubernetes.

## Prerequisites

- Kubernetes cluster running (minikube, kind, or cloud provider)
- kubectl configured to connect to your cluster
- Docker installed for building images

## Application Overview

The application consists of:
- **Java App**: Connects to MariaDB, creates users table, and inserts sample data
- **MariaDB Database**: Stores application data with persistent storage

## Step 1: Build the Docker Image

First, build the Docker image for the Java application:

```bash
docker build -t java-mariadb-app:latest .
```

If using minikube, load the image into minikube's Docker daemon:

```bash
# For minikube
minikube image load java-mariadb-app:latest

# For kind
kind load docker-image java-mariadb-app:latest --name your-cluster-name
```

## Step 2: Deploy to Kubernetes

Deploy the application components in the following order:

### 2.1 Create Namespace
```bash
kubectl apply -f k8s/namespace.yaml
```

### 2.2 Create Persistent Volume Claim
```bash
kubectl apply -f k8s/mariadb-pvc.yaml
```

### 2.3 Deploy MariaDB
```bash
kubectl apply -f k8s/mariadb-deployment.yaml
kubectl apply -f k8s/mariadb-service.yaml
```

### 2.4 Deploy Java Application
```bash
kubectl apply -f k8s/app-deployment.yaml
```

## Step 3: Verify Deployment

Check that all pods are running:

```bash
kubectl get pods -n java-app
```

Expected output:
```
NAME                                 READY   STATUS    RESTARTS   AGE
app-deployment-xxxxx                 1/1     Running   0          30s
mariadb-deployment-xxxxx             1/1     Running   0          60s
```

Check services:
```bash
kubectl get services -n java-app
```

## Step 4: View Application Logs

View the Java application logs to see database operations:

```bash
kubectl logs -f deployment/app-deployment -n java-app
```

You should see output like:
```
1 row(s) inserted.
1 row(s) inserted.
ID: 1, Name: John Doe, Email: john.doe@example.com
ID: 2, Name: Tony Stark, Email: tony.stark@example.com
```

## Step 5: Access MariaDB (Optional)

To connect to MariaDB directly for debugging:

```bash
kubectl exec -it deployment/mariadb-deployment -n java-app -- mariadb -u appuser -papppassword myapp
```

Run SQL queries:
```sql
SHOW TABLES;
SELECT * FROM users;
```

## Kubernetes Components Explained

### Namespace (`namespace.yaml`)
- Isolates the application resources
- Name: `java-app`

### MariaDB Components
- **PVC (`mariadb-pvc.yaml`)**: Provides 5GB persistent storage
- **Deployment (`mariadb-deployment.yaml`)**: 
  - Single replica (databases shouldn't scale horizontally)
  - Health checks with mariadb-admin
  - Persistent volume mounted at `/var/lib/mysql`
- **Service (`mariadb-service.yaml`)**: ClusterIP service on port 3306

### Application Deployment (`app-deployment.yaml`)
- Uses the built Docker image
- Environment variables for database connection
- Health probes to ensure application is running
- Resource limits for memory and CPU

## Troubleshooting

### Pod Not Starting
Check pod status and events:
```bash
kubectl describe pod -l app=java-app -n java-app
kubectl describe pod -l app=mariadb -n java-app
```

### Database Connection Issues
1. Verify MariaDB pod is ready:
   ```bash
   kubectl get pods -l app=mariadb -n java-app
   ```

2. Check MariaDB logs:
   ```bash
   kubectl logs -l app=mariadb -n java-app
   ```

3. Test database connectivity:
   ```bash
   kubectl exec -it deployment/app-deployment -n java-app -- nslookup mariadb-service
   ```

### Storage Issues
Check PVC status:
```bash
kubectl get pvc -n java-app
```

If PVC is pending, check storage class availability:
```bash
kubectl get storageclass
```

## Cleanup

To remove all resources:

```bash
kubectl delete namespace java-app
```

This will delete all resources in the namespace including the PVC and its data.

## Production Considerations

For production deployments, consider:

1. **Secrets Management**: Use Kubernetes Secrets instead of plain text passwords
2. **Resource Limits**: Adjust CPU/memory based on actual usage
3. **High Availability**: Consider MariaDB clustering or managed database services  
4. **Monitoring**: Add monitoring and alerting
5. **Backup Strategy**: Implement database backup procedures
6. **Security**: Network policies, pod security standards, and RBAC
7. **Load Balancer**: Add a LoadBalancer or Ingress for external access

## Configuration Details

The application uses these environment variables for database connection:
- `DB_HOST`: mariadb-service (Kubernetes service name)
- `DB_PORT`: 3306
- `DB_NAME`: myapp
- `DB_USER`: appuser  
- `DB_PASSWORD`: apppassword

The Java application will automatically create the `users` table and insert sample data on startup.