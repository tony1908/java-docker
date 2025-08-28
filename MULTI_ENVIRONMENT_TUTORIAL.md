# Multi-Environment Docker & Maven Tutorial

This comprehensive tutorial covers how to build and deploy a Java application with MariaDB across different environments (dev, test, prod) using Maven profiles, Docker, and Podman.

## Table of Contents
1. [Project Structure](#project-structure)
2. [Maven Profiles Setup](#maven-profiles-setup)
3. [Docker Configuration](#docker-configuration)
4. [Environment-Specific Deployments](#environment-specific-deployments)
5. [Podman Alternative](#podman-alternative)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

## Project Structure

```
javadb/
├── src/
├── pom.xml                      # Maven configuration with profiles
├── Dockerfile                   # Base Dockerfile
├── Dockerfile.dev              # Development-specific Dockerfile
├── Dockerfile.test             # Test-specific Dockerfile
├── Dockerfile.prod             # Production-specific Dockerfile
├── docker-compose.yaml         # Base compose file
├── docker-compose.dev.yaml     # Development environment
├── docker-compose.test.yaml    # Test environment
├── docker-compose.prod.yaml    # Production environment
├── secrets/                     # Production secrets (create manually)
│   ├── db_root_password.txt
│   ├── db_user.txt
│   └── db_password.txt
└── db/
    ├── init/                    # Development seed data
    ├── test/                    # Test data
    └── prod/                    # Production initialization
```

## Maven Profiles Setup

The `pom.xml` includes four profiles:

### Development Profile (default)
```bash
mvn clean package -Pdev
# or simply
mvn clean package
```
- Debug logging enabled
- Tests run
- Debug symbols included

### Test Profile
```bash
mvn clean package -Ptest
```
- Warning level logging
- All tests executed
- Separate test database

### Production Profile
```bash
mvn clean package -Pprod
```
- Info level logging
- Tests skipped
- Optimized compilation
- Environment variables for credentials

### Docker Profile
```bash
mvn clean package -Pdocker
```
- Container-optimized settings
- Tests skipped

## Docker Configuration

### Dockerfile Comparison

| Feature | Dev | Test | Prod |
|---------|-----|------|------|
| Debug Port | ✅ 5005 | ❌ | ❌ |
| Dev Tools | ✅ curl, netcat | ✅ curl, netcat | ✅ curl only |
| Memory | 512MB max | 256MB max | 1024MB max |
| Security | Root user | Root user | Non-root user |
| Health Check | 30s interval | 10s interval | 60s interval |
| JVM Options | Debug enabled | G1GC optimized | Production optimized |

### Building Images

```bash
# Development
docker build -f Dockerfile.dev -t java-app:dev .

# Test
docker build -f Dockerfile.test -t java-app:test .

# Production
docker build -f Dockerfile.prod -t java-app:prod .
```

## Environment-Specific Deployments

### Development Environment

**Features:**
- Debug port exposed (5005)
- Source code volume mounting
- phpMyAdmin included
- Database port exposed for external access

```bash
# Start development environment
docker-compose -f docker-compose.dev.yaml up -d

# View logs
docker-compose -f docker-compose.dev.yaml logs -f app

# Stop environment
docker-compose -f docker-compose.dev.yaml down
```

**Access Points:**
- Application: http://localhost:8080
- Debug Port: localhost:5005
- phpMyAdmin: http://localhost:8081
- MariaDB: localhost:3306

### Test Environment

**Features:**
- Faster health checks
- tmpfs for database (faster, ephemeral)
- Separate test runner container
- No restart policies

```bash
# Run tests
docker-compose -f docker-compose.test.yaml up --build test-runner

# Run application for integration tests
docker-compose -f docker-compose.test.yaml up -d app

# Clean up after tests
docker-compose -f docker-compose.test.yaml down -v
```

### Production Environment

**Features:**
- Docker secrets for sensitive data
- Resource limits and reservations
- Log rotation
- Health checks with longer intervals
- Watchtower for automatic updates
- Non-root user execution

**Setup:**

1. Create secrets directory:
```bash
mkdir -p secrets
echo "your-root-password" > secrets/db_root_password.txt
echo "appuser" > secrets/db_user.txt
echo "your-app-password" > secrets/db_password.txt
chmod 600 secrets/*.txt
```

2. Deploy:
```bash
# Set environment variables
export DB_URL="jdbc:mariadb://mariadb-prod:3306/myapp"
export DB_USERNAME="appuser"
export DB_PASSWORD="your-app-password"
export DB_NAME="myapp"

# Deploy
docker-compose -f docker-compose.prod.yaml up -d

# Monitor
docker-compose -f docker-compose.prod.yaml logs -f
```

## Podman Alternative

### Using Podman Compose

```bash
# Install podman-compose
pip3 install podman-compose

# Development
podman-compose -f docker-compose.dev.yaml up -d

# Test
podman-compose -f docker-compose.test.yaml up --build test-runner

# Production
podman-compose -f docker-compose.prod.yaml up -d
```

### Using Podman Pods

For each environment, you can create dedicated pods:

```bash
# Development pod
podman pod create --name javadb-dev-pod -p 8080:8080 -p 5005:5005 -p 3306:3306 -p 8081:80

# Test pod
podman pod create --name javadb-test-pod -p 8090:8080

# Production pod
podman pod create --name javadb-prod-pod -p 80:8080
```

Then run containers in their respective pods using `--pod` flag.

## Best Practices

### Security
1. **Production**: Always use non-root users
2. **Secrets**: Use Docker secrets or external secret management
3. **Networks**: Isolate environments with separate networks
4. **Images**: Regularly update base images

### Performance
1. **Multi-stage builds**: Separate build and runtime stages
2. **Layer caching**: Order Dockerfile commands by change frequency
3. **Resource limits**: Set appropriate CPU/memory limits
4. **Health checks**: Implement proper health endpoints

### Development Workflow
1. **Hot reloading**: Mount source code in dev environment
2. **Debug access**: Expose debug ports only in development
3. **Database admin**: Include tools like phpMyAdmin in dev only
4. **Logging**: Use appropriate log levels per environment

### Testing
1. **Isolation**: Use separate test databases
2. **Cleanup**: Always clean up test resources
3. **Speed**: Use tmpfs for test databases
4. **Automation**: Include test runner containers

## Troubleshooting

### Common Issues

**1. Database Connection Failures**
```bash
# Check database health
docker-compose -f docker-compose.dev.yaml exec mariadb-dev-container mariadb-admin ping -uroot -p

# Check network connectivity
docker-compose -f docker-compose.dev.yaml exec app nc -zv mariadb 3306
```

**2. Memory Issues**
```bash
# Check container memory usage
docker stats

# Adjust JVM memory settings in respective Dockerfiles
```

**3. Permission Errors (Production)**
```bash
# Check file ownership
docker-compose -f docker-compose.prod.yaml exec app ls -la /app

# Fix permissions if needed
docker-compose -f docker-compose.prod.yaml exec --user root app chown -R appuser:appuser /app
```

**4. Port Conflicts**
```bash
# Check port usage
netstat -tulpn | grep :8080

# Use different ports in compose files if needed
```

### Debugging Commands

```bash
# Container logs
docker-compose -f docker-compose.{env}.yaml logs -f {service}

# Execute commands in containers
docker-compose -f docker-compose.{env}.yaml exec {service} /bin/bash

# Check container health
docker-compose -f docker-compose.{env}.yaml ps

# View detailed container information
docker inspect {container_name}
```

### Performance Monitoring

```bash
# Resource usage
docker stats

# Application metrics (if health endpoint available)
curl http://localhost:8080/health

# Database performance
docker-compose -f docker-compose.dev.yaml exec mariadb-dev-container mariadb-admin processlist -uroot -p
```

## Quick Reference

### Environment Commands

| Environment | Build | Run | Debug | Clean |
|-------------|-------|-----|-------|-------|
| **Dev** | `docker-compose -f docker-compose.dev.yaml build` | `docker-compose -f docker-compose.dev.yaml up -d` | Connect to `:5005` | `docker-compose -f docker-compose.dev.yaml down -v` |
| **Test** | `docker-compose -f docker-compose.test.yaml build` | `docker-compose -f docker-compose.test.yaml up test-runner` | Check logs | `docker-compose -f docker-compose.test.yaml down -v` |
| **Prod** | `docker-compose -f docker-compose.prod.yaml build` | `docker-compose -f docker-compose.prod.yaml up -d` | Monitor logs | `docker-compose -f docker-compose.prod.yaml down` |

### Maven Commands with Profiles

```bash
# Development build
mvn clean package -Pdev

# Test with coverage
mvn clean test -Ptest

# Production build
mvn clean package -Pprod -DskipTests

# Docker build
mvn clean package -Pdocker
```

This tutorial provides a complete foundation for managing Java applications across multiple environments using modern containerization practices.