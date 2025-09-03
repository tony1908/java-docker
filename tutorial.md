# JavaDB Application Tutorial

## Overview
This tutorial covers how to build, deploy, and manage the JavaDB application using Docker and Jenkins.

## Prerequisites
- Docker installed
- Jenkins with required plugins
- Kubernetes cluster access (for K8s deployment)
- SVN access

## Quick Start

### 1. Build the Application
```bash
# Build with Maven
mvn clean package

# Build Docker image
docker build -t javadb:latest .
```

### 2. Run Locally
```bash
# Start the application
docker run -p 8080:8080 javadb:latest

# Test the endpoints
curl http://localhost:8080/
curl -X POST http://localhost:8080/message -d '{"name":"test"}'
curl http://localhost:8080/last-message
```

## Jenkins Pipelines

### Basic Pipeline (Jenkinsfile)
- Polls SVN every 2 minutes
- Builds Docker image
- Tags as latest

### Advanced Pipeline (Jenkinsfile-advanced)
- Everything from basic pipeline
- Pushes to Docker registry
- Sends email notifications on success/failure

### Kubernetes Pipeline (Jenkinsfile-k8s)
- Everything from advanced pipeline
- Deploys to local Kubernetes cluster
- Performs health checks
- Updates deployment with new image

## Configuration

### Jenkins Setup
1. Install required plugins:
   - SVN Plugin
   - Docker Plugin
   - Email Extension Plugin
   - Kubernetes Plugin

2. Configure credentials:
   - SVN credentials
   - Docker registry credentials
   - Kubeconfig for K8s access

3. Update pipeline variables:
   - `YOUR_SVN_URL_HERE`
   - `YOUR_REGISTRY_URL_HERE`
   - `YOUR_EMAIL_HERE`

### Application Endpoints
- `GET /` - Hello World
- `POST /message` - Send message to Kafka
- `GET /last-message` - Get last message from database

## Testing
```bash
# Run tests
mvn test

# Check test results
mvn surefire-report:report
```

## Troubleshooting

### Common Issues
1. **Build fails**: Check Maven dependencies in `pom.xml`
2. **Docker push fails**: Verify registry credentials
3. **K8s deployment fails**: Check kubeconfig and cluster connectivity
4. **Email not sent**: Verify SMTP configuration in Jenkins

### Logs
- Application logs: Check Docker container logs
- Jenkins logs: Check build console output
- K8s logs: `kubectl logs deployment/javadb-deployment`