# Tutorial Completo: Despliegue Kubernetes del Proyecto Java-MariaDB-Kafka

Este tutorial explica paso a paso cómo desplegar y gestionar el proyecto actual en Kubernetes, incluyendo todos los componentes: aplicación Java, MariaDB, Kafka, configuraciones, secretos e Ingress.

## Arquitectura del Proyecto

El proyecto está compuesto por:
- **Aplicación Java**: Backend que consume datos de MariaDB y produce/consume mensajes de Kafka
- **Base de Datos MariaDB**: Almacenamiento persistente de datos
- **Apache Kafka**: Sistema de mensajería para stream processing
- **ConfigMaps y Secrets**: Configuración y credenciales
- **Ingress**: Punto de entrada HTTP desde el exterior

## Estructura de Archivos Kubernetes

```
k8s/
├── namespace.yaml           # Namespace para aislar recursos
├── app-configmap.yaml      # Configuraciones de la aplicación
├── app-secret.yaml         # Credenciales sensibles
├── mariadb-pvc.yaml        # Volumen persistente para MariaDB
├── mariadb-deployment.yaml # Deployment y configuración de MariaDB
├── mariadb-service.yaml    # Servicio para acceder a MariaDB
├── kafka-deployment.yaml   # Deployment de Kafka con servicios
└── app-deployment.yaml     # Deployment de la aplicación con Ingress
```

## Paso 1: Preparación del Entorno

### 1.1 Verificar Cluster Kubernetes
```bash
# Verificar conexión al cluster
kubectl cluster-info

# Verificar nodos disponibles
kubectl get nodes
```

## Paso 2: Análisis de Configuraciones

### 2.1 Namespace (`namespace.yaml`)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: java-app
  labels:
    name: java-app
```

**Propósito**: Crear un espacio aislado llamado `java-app` para todos los recursos del proyecto.

### 2.2 ConfigMap (`app-configmap.yaml`)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: java-app
data:
  DB_HOST: mariadb-service
  DB_PORT: "3306"
  DB_NAME: myapp
  KAFKA_BOOTSTRAP_SERVERS: kafka-service:9092
```

**Explicación**:
- **DB_HOST**: Apunta al servicio de MariaDB (`mariadb-service`)
- **DB_PORT**: Puerto estándar de MariaDB (3306)
- **DB_NAME**: Nombre de la base de datos (`myapp`)
- **KAFKA_BOOTSTRAP_SERVERS**: Dirección del broker Kafka

### 2.3 Secret (`app-secret.yaml`)
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: java-app
type: Opaque
data:
  DB_USER: YXBwdXNlcg==      # appuser en base64
  DB_PASSWORD: YXBwcGFzc3dvcmQ=  # apppassword en base64
```

**Seguridad**: 
- Las credenciales están codificadas en base64
- Para decodificar: `echo "YXBwdXNlcg==" | base64 -d`

## Paso 3: Almacenamiento Persistente

### 3.1 Persistent Volume Claim (`mariadb-pvc.yaml`)
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
  storageClassName: standard
```

**Conceptos clave**:
- **ReadWriteOnce**: Solo un pod puede montar el volumen
- **1Gi**: Solicita 1 GB de almacenamiento
- **standard**: Usa la storage class por defecto

## Paso 4: Base de Datos MariaDB

### 4.1 Deployment de MariaDB (`mariadb-deployment.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mariadb-deployment
  namespace: java-app
  labels:
    app: mariadb
spec:
  replicas: 2  # ⚠️ NOTA: Para producción debería ser 1
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
          ports:
            - containerPort: 3306
          env:
            - name: MARIADB_ROOT_PASSWORD
              value: rootpassword
            - name: MARIADB_DATABASE
              value: myapp
            - name: MARIADB_USER
              value: appuser
            - name: MARIADB_PASSWORD
              value: apppassword
      volumes:
        - name: mariadb-storage
          persistentVolumeClaim:
            claimName: mariadb-pvc
```

**⚠️ Problema Identificado**: 
- `replicas: 2` puede causar problemas con bases de datos
- Falta `volumeMounts` para conectar el almacenamiento persistente

### 4.2 Servicio de MariaDB (`mariadb-service.yaml`)
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

**Función**: Proporciona un endpoint estable para que la aplicación Java se conecte a MariaDB.

## Paso 5: Sistema de Mensajería Kafka

### 5.1 Deployment de Kafka (`kafka-deployment.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-deployment
  namespace: java-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    spec:
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:latest
          ports:
            - containerPort: 9092  # Puerto del broker
            - containerPort: 9093  # Puerto del controller
          env:
            - name: KAFKA_NODE_ID
              value: "1"
            - name: KAFKA_PROCESS_ROLES
              value: "broker,controller"  # Modo KRaft (sin Zookeeper)
            - name: KAFKA_LISTENERS
              value: PLAINTEXT://:9092,CONTROLLER://:9093
            - name: KAFKA_ADVERTISED_LISTENERS
              value: PLAINTEXT://kafka:9092
            - name: KAFKA_CONTROLLER_QUORUM_VOTERS
              value: "1@kafka-controller-service:9093"
            - name: KAFKA_CLUSTER_ID
              value: "my-cluster"
            - name: KAFKA_AUTO_CREATE_TOPICS_ENABLE
              value: "true"
```

**Configuración KRaft**:
- **KAFKA_PROCESS_ROLES**: Combina broker y controller (sin Zookeeper)
- **KAFKA_LISTENERS**: Puertos internos del contenedor
- **KAFKA_ADVERTISED_LISTENERS**: Cómo los clientes se conectan
- **AUTO_CREATE_TOPICS_ENABLE**: Permite crear topics automáticamente

### 5.2 Servicios de Kafka
```yaml
# Servicio del broker
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

---
# Servicio del controller
apiVersion: v1
kind: Service
metadata:
  name: kafka-controller-service
  namespace: java-app
spec:
  selector:
    app: kafka
  ports:
    - protocol: TCP
      port: 9093
      targetPort: 9093
```

## Paso 6: Aplicación Java

### 6.1 Deployment de la Aplicación (`app-deployment.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-deployment
  namespace: java-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: app
  template:
    spec:
      containers:
        - name: app
          image: trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: DB_HOST
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: DB_USER
            # ... más variables de entorno
```

**Características importantes**:
- **Imagen de JFrog**: Usa el registry privado de JFrog Artifactory
- **imagePullPolicy: Always**: Siempre descarga la imagen más reciente
- **Variables de entorno**: Combina ConfigMaps y Secrets

### 6.2 Ingress para Acceso Externo
```yaml
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

**Configuración de Ingress**:
- **host**: `java-app.local` (debe estar en `/etc/hosts`)
- **pathType: Prefix**: Coincide con todas las rutas que empiecen con `/`

## Paso 7: Orden de Despliegue

### 7.1 Secuencia Recomendada
```bash
# 1. Crear namespace
kubectl apply -f k8s/namespace.yaml

# 2. Crear configuraciones
kubectl apply -f k8s/app-configmap.yaml
kubectl apply -f k8s/app-secret.yaml

# 3. Crear almacenamiento persistente
kubectl apply -f k8s/mariadb-pvc.yaml

# 4. Desplegar base de datos
kubectl apply -f k8s/mariadb-deployment.yaml
kubectl apply -f k8s/mariadb-service.yaml

# 5. Desplegar Kafka
kubectl apply -f k8s/kafka-deployment.yaml

# 6. Desplegar aplicación
kubectl apply -f k8s/app-deployment.yaml
```

### 7.2 Comando Todo en Uno
```bash
# Aplicar todos los manifiestos
kubectl apply -f k8s/
```

### 7.3 Verificar el Despliegue
```bash
# Ver todos los recursos en el namespace
kubectl get all -n java-app

# Ver pods con más detalles
kubectl get pods -n java-app -o wide

# Ver persistentvolumeclaims
kubectl get pvc -n java-app

# Ver configmaps y secrets
kubectl get configmaps,secrets -n java-app
```

## Paso 8: Configuración de Acceso Externo

### 8.1 Configurar /etc/hosts (Para Ingress)
```bash
# Obtener IP del cluster (minikube)
minikube ip

# Agregar entrada a /etc/hosts
echo "$(minikube ip) java-app.local" | sudo tee -a /etc/hosts
```

### 8.2 Alternativa: Port Forward
```bash
# Acceder directamente al servicio
kubectl port-forward service/app-service 8080:8080 -n java-app

# Acceder en http://localhost:8080
```

## Paso 9: Monitoreo y Logs

### 9.1 Ver Logs de la Aplicación
```bash
# Logs en tiempo real
kubectl logs -f deployment/app-deployment -n java-app

# Logs de un pod específico
kubectl logs -f pod/[nombre-del-pod] -n java-app
```

### 9.2 Ver Logs de MariaDB
```bash
kubectl logs -f deployment/mariadb-deployment -n java-app
```

### 9.3 Ver Logs de Kafka
```bash
kubectl logs -f deployment/kafka-deployment -n java-app
```

### 9.4 Acceso Interactivo a Contenedores
```bash
# Conectarse a MariaDB
kubectl exec -it deployment/mariadb-deployment -n java-app -- mariadb -u appuser -papppassword myapp

# Acceder al contenedor de la aplicación
kubectl exec -it deployment/app-deployment -n java-app -- /bin/bash

# Conectarse a Kafka (crear/listar topics)
kubectl exec -it deployment/kafka-deployment -n java-app -- kafka-topics --bootstrap-server localhost:9092 --list
```