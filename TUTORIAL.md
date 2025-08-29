# Java API with Javalin, MariaDB, Docker Compose & Kubernetes Tutorial

## Overview

This tutorial demonstrates how to create a simple REST API using Java with Javalin framework, connected to MariaDB database, and deployed using both Docker Compose and Kubernetes.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Java App      │    │   MariaDB       │
│   (Javalin API) │◄──►│   Database      │
│   Port: 8080    │    │   Port: 3306    │
└─────────────────┘    └─────────────────┘
```

## Features

- **REST API** with Javalin framework
- **CRUD operations** for users
- **MariaDB integration** with connection pooling
- **Docker containerization**
- **Docker Compose** for multi-container setup
- **Kubernetes manifests** for orchestration
- **Health checks** and monitoring

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | API status |
| GET | `/users` | List all users |
| GET | `/users/{id}` | Get user by ID |
| POST | `/users` | Create new user |
| GET | `/health` | Health check |

## Project Structure

```
.
├── Dockerfile
├── docker-compose.yaml
├── pom.xml
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   ├── App.java
│                   ├── DatabaseManager.java
│                   └── User.java
└── k8s/
    ├── namespace.yaml
    ├── mariadb-pvc.yaml
    ├── mariadb-deployment.yaml
    ├── mariadb-service.yaml
    ├── app-deployment.yaml
    └── app-service.yaml
```

## Step-by-Step Implementation

### 1. Maven Dependencies

Updated `pom.xml` to include required dependencies:

```xml
<dependencies>
    <!-- MariaDB Driver -->
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
        <version>3.5.2</version>
    </dependency>
    
    <!-- Javalin Web Framework -->
    <dependency>
        <groupId>io.javalin</groupId>
        <artifactId>javalin</artifactId>
        <version>5.6.3</version>
    </dependency>
    
    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- Redis (optional) -->
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>6.1.0</version>
    </dependency>
    
    <!-- JUnit for testing -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>3.8.1</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. User Model

Created `User.java` as a simple POJO:

```java
package com.example;

public class User {
    private int id;
    private String name;
    private String email;
    private String createdAt;

    // Constructors
    public User() {}

    public User(int id, String name, String email, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
```

### 3. Enhanced Database Manager

Extended `DatabaseManager.java` with new methods:

```java
package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_HOST = System.getenv("DB_HOST");
    private static final String DB_PORT = System.getenv("DB_PORT");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static final String JDBC_URL = String.format("jdbc:mariadb://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);
    private Connection connection;

    public DatabaseManager() throws SQLException {
        connect();
        initDatabase();
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    private void initDatabase() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
             "    id INT AUTO_INCREMENT PRIMARY KEY," +
             "    name VARCHAR(100) NOT NULL," +
             "    email VARCHAR(100) NOT NULL UNIQUE," +
             "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
             ")";
            
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    // Get all users as List
    public List<User> getUsersList() throws SQLException {
        List<User> users = new ArrayList<>();
        String selectSQL = "SELECT * FROM users";
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(selectSQL);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                String createdAt = rs.getString("created_at");
                users.add(new User(id, name, email, createdAt));
            }
        }
        return users;
    }

    // Get user by ID
    public User getUserById(int id) throws SQLException {
        String selectSQL = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(selectSQL)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String email = rs.getString("email");
                String createdAt = rs.getString("created_at");
                return new User(id, name, email, createdAt);
            }
        }
        return null;
    }

    // Create user and return created user
    public User createUser(String name, String email) throws SQLException {
        String insertSQL = "INSERT INTO users (name, email) VALUES (?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return getUserById(id);
                }
            }
        }
        return null;
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
```

### 4. Javalin REST API

Completely rewrote `App.java` to implement the REST API:

```java
package com.example;

import io.javalin.Javalin;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class App {
    private static DatabaseManager dbManager;

    public static void main(String[] args) {
        try {
            dbManager = new DatabaseManager();
            
            // Insert sample data (ignore duplicates)
            try {
                dbManager.insertUser("John Doe", "john.doe@example.com");
            } catch (SQLException e) {
                // User already exists, ignore
            }
            try {
                dbManager.insertUser("Tony Stark", "tony.stark@example.com");
            } catch (SQLException e) {
                // User already exists, ignore
            }
            
            // Create Javalin app
            Javalin app = Javalin.create().start(8080);
            
            // CORS headers
            app.before(ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            // Routes
            app.get("/", ctx -> {
                ctx.json(Map.of("message", "Java API with MariaDB", "status", "running"));
            });

            app.get("/users", ctx -> {
                try {
                    List<User> users = dbManager.getUsersList();
                    ctx.json(users);
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "Database error: " + e.getMessage()));
                }
            });

            app.get("/users/{id}", ctx -> {
                try {
                    int id = Integer.parseInt(ctx.pathParam("id"));
                    User user = dbManager.getUserById(id);
                    if (user != null) {
                        ctx.json(user);
                    } else {
                        ctx.status(404).json(Map.of("error", "User not found"));
                    }
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("error", "Invalid user ID"));
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "Database error: " + e.getMessage()));
                }
            });

            app.post("/users", ctx -> {
                try {
                    String name = ctx.formParam("name");
                    String email = ctx.formParam("email");
                    
                    if (name == null || email == null) {
                        ctx.status(400).json(Map.of("error", "Name and email are required"));
                        return;
                    }
                    
                    User user = dbManager.createUser(name, email);
                    if (user != null) {
                        ctx.status(201).json(user);
                    } else {
                        ctx.status(500).json(Map.of("error", "Failed to create user"));
                    }
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "Database error: " + e.getMessage()));
                }
            });

            app.get("/health", ctx -> {
                ctx.json(Map.of("status", "healthy", "database", "connected"));
            });

            // Graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                app.stop();
                if (dbManager != null) {
                    try {
                        dbManager.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }));

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```

### 5. Docker Configuration

**Dockerfile:**
```dockerfile
FROM maven:3.8.4-openjdk-11-slim AS builder

WORKDIR /app

COPY src/ ./src/
COPY pom.xml ./

RUN mvn clean package

FROM openjdk:11-jre-slim AS runner

WORKDIR /app

COPY --from=builder /app/target/javadb-1.0-SNAPSHOT.jar /app/javadb-1.0-SNAPSHOT.jar

EXPOSE 8080

CMD ["java", "-jar", "javadb-1.0-SNAPSHOT.jar"]
```

**docker-compose.yaml:**
```yaml
version: '3.9'

services:
  app:
    build: .
    container_name: java-app-container
    ports:
      - "8082:8080"  # Using 8082 to avoid port conflicts
    environment:
      DB_HOST: mariadb
      DB_PORT: 3306
      DB_NAME: myapp
      DB_USER: appuser
      DB_PASSWORD: apppassword
    depends_on:
      mariadb:
        condition: service_healthy

  mariadb:
    image: mariadb
    container_name: mariadb-container
    environment:
      MARIADB_ROOT_PASSWORD: rootpassword
      MARIADB_DATABASE: myapp
      MARIADB_USER: appuser
      MARIADB_PASSWORD: apppassword
    healthcheck:
      test: ["CMD-SHELL", "mariadb-admin ping -h 127.0.0.1 -uroot -p$${MARIADB_ROOT_PASSWORD} --silent"]
      interval: 10s
      timeout: 5s
      retries: 2
    volumes:
      - mariadb-data:/var/lib/mysql

volumes:
  mariadb-data:
```

### 6. Kubernetes Manifests

**k8s/namespace.yaml:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: java-app
```

**k8s/mariadb-pvc.yaml:**
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mariadb-pvc
  namespace: java-app
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

**k8s/mariadb-deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mariadb-deployment
  namespace: java-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mariadb
  template:
    metadata:
      labels:
        app: mariadb
    spec:
      containers:
        - name: mariadb
          image: mariadb:latest
          env:
            - name: MARIADB_ROOT_PASSWORD
              value: rootpassword
            - name: MARIADB_DATABASE
              value: myapp
            - name: MARIADB_USER
              value: appuser
            - name: MARIADB_PASSWORD
              value: apppassword
          ports:
            - containerPort: 3306
          volumeMounts:
            - name: mariadb-storage
              mountPath: /var/lib/mysql
      volumes:
        - name: mariadb-storage
          persistentVolumeClaim:
            claimName: mariadb-pvc
```

**k8s/mariadb-service.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: mariadb-service
  namespace: java-app
spec:
  selector:
    app: mariadb
  ports:
    - protocol: TCP
      port: 3306
      targetPort: 3306
  type: ClusterIP
```

**k8s/app-deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-deployment
  namespace: java-app
  labels:
    app: java-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: java-app
  template:
    metadata:
      labels:
        app: java-app
    spec:
      containers:
        - name: java-app
          image: java-mariadb-app:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8080
          env:
            - name: DB_HOST
              value: mariadb-service
            - name: DB_PORT
              value: "3306"
            - name: DB_NAME
              value: myapp
            - name: DB_USER
              value: appuser
            - name: DB_PASSWORD
              value: apppassword
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
            timeoutSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
          startupProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 30
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
```

**k8s/app-service.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: app-service
  namespace: java-app
spec:
  selector:
    app: java-app
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
      nodePort: 30080
  type: NodePort
```

## Deployment Instructions

### Docker Compose Deployment

1. **Build and start services:**
   ```bash
   docker-compose build
   docker-compose up -d
   ```

2. **Check status:**
   ```bash
   docker-compose ps
   docker-compose logs app
   ```

3. **Test API:**
   ```bash
   # API Status
   curl http://localhost:8082/
   
   # List users
   curl http://localhost:8082/users
   
   # Health check
   curl http://localhost:8082/health
   
   # Create user
   curl -X POST -d "name=Bruce Wayne&email=bruce.wayne@example.com" http://localhost:8082/users
   ```

### Kubernetes Deployment

1. **Build Docker image:**
   ```bash
   docker build -t java-mariadb-app:latest .
   ```

2. **Apply manifests:**
   ```bash
   kubectl apply -f k8s/
   ```

3. **Check deployment:**
   ```bash
   kubectl get pods -n java-app
   kubectl get services -n java-app
   ```

4. **Test API:**
   ```bash
   # API will be available at http://localhost:30080
   curl http://localhost:30080/
   curl http://localhost:30080/users
   curl http://localhost:30080/health
   ```

## API Usage Examples

### Get API Status
```bash
curl http://localhost:8082/
# Response: {"message":"Java API with MariaDB","status":"running"}
```

### List All Users
```bash
curl http://localhost:8082/users
# Response: [{"id":1,"name":"John Doe","email":"john.doe@example.com","createdAt":"2025-08-28 00:22:49"}]
```

### Get User by ID
```bash
curl http://localhost:8082/users/1
# Response: {"id":1,"name":"John Doe","email":"john.doe@example.com","createdAt":"2025-08-28 00:22:49"}
```

### Create New User
```bash
curl -X POST -d "name=Bruce Wayne&email=bruce.wayne@example.com" http://localhost:8082/users
# Response: {"id":10,"name":"Bruce Wayne","email":"bruce.wayne@example.com","createdAt":"2025-08-29 04:11:19"}
```

### Health Check
```bash
curl http://localhost:8082/health
# Response: {"database":"connected","status":"healthy"}
```

## Architecture Benefits

1. **Microservices Ready:** Separate containers for app and database
2. **Scalable:** Kubernetes deployment with resource limits
3. **Health Monitoring:** HTTP health probes for container orchestration
4. **Environment Agnostic:** Configuration via environment variables
5. **Production Ready:** Proper error handling and graceful shutdown

## Troubleshooting

### Common Issues

1. **Port conflicts:** Change the host port in docker-compose.yaml
2. **Database connection failed:** Check MariaDB health and environment variables
3. **Kubernetes not accessible:** Start Kubernetes cluster (Docker Desktop/minikube)

### Logs

```bash
# Docker Compose logs
docker-compose logs app
docker-compose logs mariadb

# Kubernetes logs
kubectl logs -f deployment/app-deployment -n java-app
kubectl logs -f deployment/mariadb-deployment -n java-app
```

## Next Steps

1. **Add authentication:** Implement JWT or OAuth
2. **Add more endpoints:** Update, delete operations
3. **Database migrations:** Version controlled schema changes
4. **Monitoring:** Prometheus metrics and Grafana dashboards
5. **CI/CD:** Automated testing and deployment pipelines

This tutorial provides a complete foundation for a production-ready Java API with modern container orchestration!