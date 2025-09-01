# Building a Java Kafka Microservice with Kubernetes

## Tutorial Overview

This incremental tutorial will guide you through building a complete microservice that:
- Accepts HTTP requests with names
- Publishes messages to Apache Kafka
- Consumes and stores messages in a database
- Provides an endpoint to retrieve the last message
- Deploys everything on Kubernetes with proper configuration management

## Prerequisites

- Java 11+
- Maven 3.6+
- Docker Desktop with Kubernetes enabled
- kubectl CLI tool
- Basic knowledge of Java, REST APIs, and Kubernetes concepts

---

## Phase 1: Basic Java Application Setup

### Step 1.1: Create Maven Project Structure

```
javadb/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    ├── App.java
                    └── DatabaseManager.java
```

### Step 1.2: Configure Maven Dependencies (pom.xml)

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>javadb</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>javadb</name>

  <properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
  </properties>

  <dependencies>
    <!-- Database Driver -->
    <dependency>
      <groupId>org.mariadb.jdbc</groupId>
      <artifactId>mariadb-java-client</artifactId>
      <version>3.5.2</version>
    </dependency>
    
    <!-- Web Framework -->
    <dependency>
        <groupId>io.javalin</groupId>
        <artifactId>javalin</artifactId>
        <version>5.6.3</version>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- Kafka Client -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>3.5.1</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.4.1</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>com.example.App</mainClass>
                </transformer>
              </transformers>
            </configuration> 
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

### Step 1.3: Create Database Manager

Create `DatabaseManager.java`:

```java
package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_HOST = System.getenv("DB_HOST");
    private static final String DB_PORT = System.getenv("DB_PORT");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static final String JDBC_URL = String.format("jdbc:mariadb://%s:%s/%s", 
                                                        DB_HOST, DB_PORT, DB_NAME);
    private Connection connection;

    public DatabaseManager() throws SQLException {
        connect();
        initDatabase();
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    private void initDatabase() throws SQLException {
        String createMessagesTableSQL = "CREATE TABLE IF NOT EXISTS messages (" +
             "    id INT AUTO_INCREMENT PRIMARY KEY," +
             "    name VARCHAR(255) NOT NULL," +
             "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
             ")";
            
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createMessagesTableSQL);
        }
    }

    public void insertMessage(String name) throws SQLException {
        String insertSQL = "INSERT INTO messages (name) VALUES (?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            stmt.setString(1, name);
            int rowsAffected = stmt.executeUpdate();
            System.out.println(rowsAffected + " message(s) inserted.");
        }
    }

    public String getLastMessage() throws SQLException {
        String selectSQL = "SELECT name FROM messages ORDER BY created_at DESC LIMIT 1";
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(selectSQL);
            if (rs.next()) {
                return rs.getString("name");
            }
            return null;
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
```

**Key Learning Points:**
- Environment variable configuration for database connection
- Automatic table creation with `CREATE TABLE IF NOT EXISTS`
- Prepared statements for SQL injection prevention
- Resource management with try-with-resources

---

## Phase 2: Kafka Integration

### Step 2.1: Create Kafka Producer Service

Create `KafkaProducerService.java`:

```java
package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProducerService {
    private static final String TOPIC_NAME = "names-topic";
    private final KafkaProducer<String, String> producer;

    public KafkaProducerService() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                  System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        this.producer = new KafkaProducer<>(props);
    }

    public void sendMessage(String name) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, name);
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Message sent successfully to topic " + metadata.topic() + 
                                     " partition " + metadata.partition() + 
                                     " offset " + metadata.offset());
                } else {
                    System.err.println("Failed to send message: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
```

**Key Learning Points:**
- Kafka producer configuration with proper serialization
- Asynchronous message sending with callbacks
- Environment-based configuration with defaults
- Proper resource cleanup

### Step 2.2: Create Kafka Consumer Service

Create `KafkaConsumerService.java`:

```java
package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerService implements Runnable {
    private static final String TOPIC_NAME = "names-topic";
    private static final String GROUP_ID = "names-consumer-group";
    private final KafkaConsumer<String, String> consumer;
    private final DatabaseManager dbManager;
    private volatile boolean running = true;

    public KafkaConsumerService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                  System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);

        this.consumer = new KafkaConsumer<>(props);
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received message: " + record.value() + 
                                     " from topic " + record.topic() + 
                                     " partition " + record.partition() + 
                                     " offset " + record.offset());
                    
                    try {
                        dbManager.insertMessage(record.value());
                    } catch (SQLException e) {
                        System.err.println("Failed to insert message to database: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    public void stop() {
        running = false;
    }
}
```

**Key Learning Points:**
- Kafka consumer groups and offset management
- Continuous polling with timeout
- Thread-safe shutdown mechanism
- Integration with database for message persistence

### Step 2.3: Update Main Application

Update `App.java`:

```java
package com.example;

import java.sql.SQLException;
import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        DatabaseManager dbManager = null;
        KafkaProducerService kafkaProducer = null;
        KafkaConsumerService kafkaConsumer = null;
        
        try {
            dbManager = new DatabaseManager();
            kafkaProducer = new KafkaProducerService();
            kafkaConsumer = new KafkaConsumerService(dbManager);
            
            // Start Kafka consumer in background thread
            Thread consumerThread = new Thread(kafkaConsumer);
            consumerThread.start();

            ObjectMapper objectMapper = new ObjectMapper();
            final DatabaseManager finalDbManager = dbManager;
            final KafkaProducerService finalKafkaProducer = kafkaProducer;
            final KafkaConsumerService finalKafkaConsumer = kafkaConsumer;

            // Create Javalin web server
            Javalin app = Javalin.create().start(8080);

            app.get("/", ctx -> ctx.result("Hello World"));
            
            // POST endpoint to send name to Kafka
            app.post("/send-name", ctx -> {
                try {
                    String requestBody = ctx.body();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonMap = objectMapper.readValue(requestBody, Map.class);
                    String name = (String) jsonMap.get("name");
                    
                    if (name == null || name.trim().isEmpty()) {
                        ctx.status(400).result("Name is required");
                        return;
                    }
                    
                    finalKafkaProducer.sendMessage(name);
                    ctx.status(200).result("Message sent to Kafka successfully");
                    
                } catch (Exception e) {
                    ctx.status(500).result("Error processing request: " + e.getMessage());
                }
            });
            
            // GET endpoint to retrieve last consumed message
            app.get("/last-message", ctx -> {
                try {
                    String lastMessage = finalDbManager.getLastMessage();
                    if (lastMessage != null) {
                        ctx.json(Map.of("lastMessage", lastMessage));
                    } else {
                        ctx.json(Map.of("lastMessage", "No messages found"));
                    }
                } catch (SQLException e) {
                    ctx.status(500).result("Error retrieving last message: " + e.getMessage());
                }
            });
            
            // Graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down...");
                finalKafkaConsumer.stop();
                finalKafkaProducer.close();
                try {
                    finalDbManager.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Key Learning Points:**
- Multi-threaded application with background Kafka consumer
- RESTful API design with proper HTTP status codes
- JSON request/response handling
- Graceful shutdown with cleanup

---

## Phase 3: Kubernetes Deployment

### Step 3.1: Create Namespace

Create `k8s/namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: java-app
```

### Step 3.2: Deploy MariaDB Database

Create `k8s/mariadb-pvc.yaml`:

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

Create `k8s/mariadb-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mariadb
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
          image: mariadb:10.11
          env:
            - name: MARIADB_ROOT_PASSWORD
              value: "rootpassword"
            - name: MARIADB_DATABASE
              value: "myapp"
            - name: MARIADB_USER
              value: "appuser"
            - name: MARIADB_PASSWORD
              value: "apppassword"
          ports:
            - containerPort: 3306
          volumeMounts:
            - name: mariadb-storage
              mountPath: /var/lib/mysql
      volumes:
        - name: mariadb-storage
          persistentVolumeClaim:
            claimName: mariadb-pvc

---
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

### Step 3.3: Deploy Zookeeper and Kafka

Create `k8s/zookeeper-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zookeeper
  namespace: java-app
  labels:
    app: zookeeper
spec:
  replicas: 1
  selector:
    matchLabels:
      app: zookeeper
  template:
    metadata:
      labels:
        app: zookeeper
    spec:
      containers:
        - name: zookeeper
          image: confluentinc/cp-zookeeper:latest
          ports:
            - containerPort: 2181
          env:
            - name: ZOOKEEPER_CLIENT_PORT
              value: "2181"
            - name: ZOOKEEPER_TICK_TIME
              value: "2000"
          volumeMounts:
            - name: zookeeper-data
              mountPath: /var/lib/zookeeper/data
      volumes:
        - name: zookeeper-data
          emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: zookeeper-service
  namespace: java-app
spec:
  selector:
    app: zookeeper
  ports:
    - protocol: TCP
      port: 2181
      targetPort: 2181
  type: ClusterIP
```

Create `k8s/kafka-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka
  namespace: java-app
  labels:
    app: kafka
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:latest
          ports:
            - containerPort: 9092
          env:
            - name: KAFKA_BROKER_ID
              value: "1"
            - name: KAFKA_ZOOKEEPER_CONNECT
              value: "zookeeper-service:2181"
            - name: KAFKA_ADVERTISED_LISTENERS
              value: "PLAINTEXT://kafka:9092"
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_AUTO_CREATE_TOPICS_ENABLE
              value: "true"
          volumeMounts:
            - name: kafka-data
              mountPath: /var/lib/kafka/data
      volumes:
        - name: kafka-data
          emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: kafka-service
  namespace: java-app
spec:
  selector:
    app: kafka
  ports:
    - protocol: TCP
      port: 9092
      targetPort: 9092
  type: ClusterIP
```

**Key Learning Points:**
- Kubernetes service discovery through service names
- Persistent storage for stateful services
- Proper service exposure within cluster
- Kafka-Zookeeper dependency management

---

## Phase 4: Configuration Management with ConfigMaps and Secrets

### Step 4.1: Create ConfigMap for Non-Sensitive Configuration

Create `k8s/app-configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: java-app
data:
  DB_HOST: "mariadb-service"
  DB_PORT: "3306"
  DB_NAME: "myapp"
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
```

### Step 4.2: Create Secret for Sensitive Data

Create `k8s/app-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: java-app
type: Opaque
data:
  # Base64 encoded values
  # DB_USER: appuser (encoded)
  DB_USER: YXBwdXNlcg==
  # DB_PASSWORD: apppassword (encoded)
  DB_PASSWORD: YXBwcGFzc3dvcmQ=
```

**Note:** To encode secrets:
```bash
echo -n "appuser" | base64
echo -n "apppassword" | base64
```

### Step 4.3: Deploy Application with Configuration

Create `k8s/app-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-deployment
  namespace: java-app
  labels:
    app: app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: app
  template:
    metadata:
      labels:
        app: app
    spec:
      containers:
        - name: app
          image: java-mariadb-app
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: app-config
            - secretRef:
                name: app-secret

---
apiVersion: v1
kind: Service
metadata:
  name: app-service
  namespace: java-app
spec:
  selector:
    app: app
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: ClusterIP

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  namespace: java-app
spec:
  rules:
    - host: java-app.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: app-service
                port:
                  number: 8080
```

**Key Learning Points:**
- Separation of sensitive and non-sensitive configuration
- Base64 encoding for secrets
- Environment variable injection from ConfigMaps and Secrets
- Ingress for external access

---

## Phase 5: Building and Deployment

### Step 5.1: Build Application

```bash
# Build the application
mvn clean package

# Build Docker image
docker build -t java-mariadb-app .
```

### Step 5.2: Deploy to Kubernetes

```bash
# Apply all Kubernetes manifests in order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/app-configmap.yaml
kubectl apply -f k8s/app-secret.yaml
kubectl apply -f k8s/mariadb-pvc.yaml
kubectl apply -f k8s/mariadb-deployment.yaml
kubectl apply -f k8s/zookeeper-deployment.yaml
kubectl apply -f k8s/kafka-deployment.yaml
kubectl apply -f k8s/app-deployment.yaml
```

### Step 5.3: Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n java-app

# Check services
kubectl get services -n java-app

# View application logs
kubectl logs -f deployment/app-deployment -n java-app
```

---

## Phase 6: Testing the Application

### Step 6.1: Test POST Endpoint

```bash
# Port forward to access the application
kubectl port-forward service/app-service 8080:8080 -n java-app

# Send a name to Kafka
curl -X POST http://localhost:8080/send-name \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe"}'
```

### Step 6.2: Test GET Endpoint

```bash
# Retrieve the last consumed message
curl http://localhost:8080/last-message
```

### Step 6.3: Monitor Kafka Messages

```bash
# Check Kafka consumer logs
kubectl logs -f deployment/app-deployment -n java-app

# Expected output should show:
# - Message sent to Kafka
# - Message consumed from Kafka
# - Message inserted to database
```

---

## Architecture Summary

```
[HTTP Client] 
     ↓ POST /send-name
[Java App] → [Kafka Topic] → [Kafka Consumer] → [MariaDB]
     ↑ GET /last-message ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
```

## Key Concepts Covered

1. **Microservice Architecture**: Single responsibility service with clear API
2. **Event-Driven Architecture**: Asynchronous message processing with Kafka
3. **Configuration Management**: Environment-based config with K8s ConfigMaps/Secrets
4. **Container Orchestration**: Multi-service deployment with Kubernetes
5. **Data Persistence**: Database integration with persistent storage
6. **Monitoring & Logging**: Application observability

## Next Steps for Advanced Learning

1. Add health checks and readiness probes
2. Implement retry mechanisms and dead letter queues
3. Add metrics and monitoring with Prometheus
4. Implement horizontal pod autoscaling
5. Add integration tests with Testcontainers
6. Implement schema registry for Kafka messages

This tutorial provides a solid foundation for building production-ready microservices with modern Java, Kafka, and Kubernetes technologies.