# Tutorial de Kubernetes: Despliegue de Aplicación Java con MariaDB

Este tutorial te guía paso a paso para desplegar una aplicación Java con base de datos MariaDB en Kubernetes.

## Prerrequisitos

- Cluster de Kubernetes funcionando (minikube, kind, o proveedor cloud)
- kubectl configurado para conectarse al cluster
- Docker instalado para construir imágenes

## Descripción de la Aplicación

La aplicación está compuesta por:
- **Aplicación Java**: Se conecta a MariaDB, crea tabla de usuarios e inserta datos de prueba
- **Base de Datos MariaDB**: Almacena datos de la aplicación con almacenamiento persistente

## Paso 1: Construcción de la Imagen Docker

### 1.1 Crear la imagen Docker
```bash
docker build -t java-mariadb-app:latest .
```

### 1.2 Cargar imagen en el entorno local (si usas minikube o kind)

Para **minikube**:
```bash
minikube image load java-mariadb-app:latest
```

Para **kind**:
```bash
kind load docker-image java-mariadb-app:latest --name nombre-de-tu-cluster
```

### Explicación del Dockerfile
- **FROM**: Imagen base de OpenJDK 11
- **COPY**: Copia el JAR compilado al contenedor
- **EXPOSE**: Expone el puerto 8080 para la aplicación
- **CMD**: Comando para ejecutar la aplicación Java

## Paso 2: Análisis de los Manifiestos de Kubernetes

### 2.1 Namespace (`k8s/namespace.yaml`)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: java-app
```

**Propósito**: Aísla los recursos de la aplicación del resto del cluster.

### 2.2 Persistent Volume Claim (`k8s/mariadb-pvc.yaml`)
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mariadb-pv-claim
  namespace: java-app
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

**Explicación**:
- **accessModes**: `ReadWriteOnce` permite que solo un pod monte el volumen
- **storage**: Solicita 5GB de almacenamiento persistente
- **Propósito**: Garantiza que los datos de MariaDB persistan aunque el pod se reinicie

### 2.3 Deployment de MariaDB (`k8s/mariadb-deployment.yaml`)
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
        image: mariadb:10.11
        env:
        - name: MYSQL_ROOT_PASSWORD
          value: "rootpassword"
        - name: MYSQL_DATABASE
          value: "myapp"
        - name: MYSQL_USER
          value: "appuser"
        - name: MYSQL_PASSWORD
          value: "apppassword"
        ports:
        - containerPort: 3306
        volumeMounts:
        - name: mariadb-persistent-storage
          mountPath: /var/lib/mysql
        livenessProbe:
          exec:
            command:
            - mariadb-admin
            - ping
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - mariadb-admin
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: mariadb-persistent-storage
        persistentVolumeClaim:
          claimName: mariadb-pv-claim
```

**Conceptos clave**:
- **replicas: 1**: Solo una instancia (las BD no se escalan horizontalmente)
- **env**: Variables de entorno para configurar MariaDB
- **volumeMounts**: Monta el almacenamiento persistente en `/var/lib/mysql`
- **livenessProbe**: Verifica si el contenedor está vivo
- **readinessProbe**: Verifica si el contenedor está listo para recibir tráfico

### 2.4 Service de MariaDB (`k8s/mariadb-service.yaml`)
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

**Explicación**:
- **selector**: Conecta con pods que tengan la etiqueta `app: mariadb`
- **type: ClusterIP**: Servicio interno del cluster (no expuesto externamente)
- **port/targetPort**: Puerto 3306 para conexiones MySQL/MariaDB

### 2.5 Deployment de la Aplicación Java (`k8s/app-deployment.yaml`)
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
        env:
        - name: DB_HOST
          value: "mariadb-service"
        - name: DB_PORT
          value: "3306"
        - name: DB_NAME
          value: "myapp"
        - name: DB_USER
          value: "appuser"
        - name: DB_PASSWORD
          value: "apppassword"
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

**Conceptos importantes**:
- **imagePullPolicy: Never**: No intenta descargar la imagen (usa la local)
- **env**: Variables de entorno para conexión a base de datos
- **resources**: Límites de CPU y memoria para el contenedor

## Paso 3: Despliegue en Kubernetes

### 3.1 Crear el Namespace
```bash
kubectl apply -f k8s/namespace.yaml
```

**¿Qué hace?**: Crea un namespace llamado `java-app` para aislar los recursos.

### 3.2 Crear el Persistent Volume Claim
```bash
kubectl apply -f k8s/mariadb-pvc.yaml
```

**¿Qué hace?**: Solicita 5GB de almacenamiento persistente para MariaDB.

### 3.3 Desplegar MariaDB
```bash
kubectl apply -f k8s/mariadb-deployment.yaml
kubectl apply -f k8s/mariadb-service.yaml
```

**¿Qué hace?**: 
- Despliega el pod de MariaDB con almacenamiento persistente
- Crea un servicio interno para que la aplicación Java pueda conectarse

### 3.4 Desplegar la Aplicación Java
```bash
kubectl apply -f k8s/app-deployment.yaml
```

**¿Qué hace?**: Despliega la aplicación Java que se conectará a MariaDB.

## Paso 4: Verificación del Despliegue

### 4.1 Verificar que todos los pods estén ejecutándose
```bash
kubectl get pods -n java-app
```

**Resultado esperado**:
```
NAME                                 READY   STATUS    RESTARTS   AGE
app-deployment-xxxxx                 1/1     Running   0          30s
mariadb-deployment-xxxxx             1/1     Running   0          60s
```

### 4.2 Verificar los servicios
```bash
kubectl get services -n java-app
```

### 4.3 Verificar el almacenamiento persistente
```bash
kubectl get pvc -n java-app
```

## Paso 5: Ver los Logs de la Aplicación

### Ver logs en tiempo real
```bash
kubectl logs -f deployment/app-deployment -n java-app
```

**Salida esperada**:
```
Conectando a la base de datos...
1 row(s) inserted.
1 row(s) inserted.
ID: 1, Name: John Doe, Email: john.doe@example.com
ID: 2, Name: Tony Stark, Email: tony.stark@example.com
```

## Paso 6: Acceso a MariaDB (Opcional)

### Conectarse directamente a MariaDB para depuración
```bash
kubectl exec -it deployment/mariadb-deployment -n java-app -- mariadb -u appuser -papppassword myapp
```

### Ejecutar consultas SQL
```sql
SHOW TABLES;
SELECT * FROM users;
DESCRIBE users;
```

## Comandos Útiles para Troubleshooting

### Ver detalles de un pod con problemas
```bash
kubectl describe pod -l app=java-app -n java-app
kubectl describe pod -l app=mariadb -n java-app
```

### Ver eventos del namespace
```bash
kubectl get events -n java-app --sort-by='.lastTimestamp'
```

### Acceder al shell de un contenedor
```bash
kubectl exec -it deployment/app-deployment -n java-app -- /bin/bash
```

### Probar conectividad de red
```bash
kubectl exec -it deployment/app-deployment -n java-app -- nslookup mariadb-service
```

## Conceptos Clave de Kubernetes

### Objetos Principales
1. **Pod**: Unidad más pequeña que se puede desplegar (uno o más contenedores)
2. **Deployment**: Gestiona múltiples réplicas de pods
3. **Service**: Punto de acceso estable para acceder a pods
4. **PVC**: Solicitud de almacenamiento persistente
5. **Namespace**: Aislamiento lógico de recursos

### Tipos de Services
- **ClusterIP** (default): Solo accesible dentro del cluster
- **NodePort**: Expone el servicio en un puerto del nodo
- **LoadBalancer**: Crea un balanceador de carga externo

### Health Checks
- **livenessProbe**: ¿Está el contenedor vivo? (reinicia si falla)
- **readinessProbe**: ¿Está listo para recibir tráfico? (quita del servicio si falla)

## Escalado de la Aplicación

### Escalar la aplicación Java
```bash
kubectl scale deployment app-deployment --replicas=3 -n java-app
```

### Ver el estado del escalado
```bash
kubectl get pods -l app=java-app -n java-app
```

**Nota**: MariaDB no debe escalarse horizontalmente (mantener 1 replica).

## Limpieza de Recursos

### Eliminar todos los recursos
```bash
kubectl delete namespace java-app
```

**¿Qué hace?**: Elimina el namespace y todos los recursos dentro de él, incluyendo el PVC y sus datos.

### Eliminar recursos individualmente (alternativa)
```bash
kubectl delete -f k8s/app-deployment.yaml
kubectl delete -f k8s/mariadb-service.yaml
kubectl delete -f k8s/mariadb-deployment.yaml
kubectl delete -f k8s/mariadb-pvc.yaml
kubectl delete -f k8s/namespace.yaml
```

## Consideraciones para Producción

### 1. Gestión de Secretos
En lugar de variables de entorno en texto plano:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mariadb-secret
type: Opaque
data:
  password: YXBwcGFzc3dvcmQ=  # base64 encoded
```

### 2. Límites de Recursos
Ajustar CPU y memoria según el uso real:
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### 3. Alta Disponibilidad
- Usar múltiples réplicas para la aplicación Java
- Considerar clustering de MariaDB o servicios gestionados

### 4. Monitoreo
- Implementar métricas con Prometheus
- Configurar alertas
- Usar herramientas de logging centralizadas

### 5. Seguridad
- Network Policies para aislar tráfico
- Pod Security Standards
- RBAC (Role-Based Access Control)
- Usar imágenes sin vulnerabilidades

### 6. Backup
- Implementar estrategia de respaldo de la base de datos
- Probar procedimientos de restauración

## Variables de Entorno de la Aplicación

La aplicación Java usa estas variables para conectarse a la base de datos:
- `DB_HOST`: mariadb-service (nombre del servicio de Kubernetes)
- `DB_PORT`: 3306
- `DB_NAME`: myapp
- `DB_USER`: appuser  
- `DB_PASSWORD`: apppassword

La aplicación Java creará automáticamente la tabla `users` e insertará datos de prueba al iniciarse.

## Arquitectura del Despliegue

```
┌─────────────────────────────────────────┐
│              Kubernetes Cluster         │
│  ┌─────────────────────────────────────┐ │
│  │         Namespace: java-app         │ │
│  │                                     │ │
│  │  ┌─────────────┐  ┌─────────────┐   │ │
│  │  │             │  │             │   │ │
│  │  │  Java App   │──│  MariaDB    │   │ │
│  │  │    Pod      │  │    Pod      │   │ │
│  │  │             │  │             │   │ │
│  │  └─────────────┘  └──────┬──────┘   │ │
│  │                          │          │ │
│  │                  ┌───────▼──────┐   │ │
│  │                  │              │   │ │
│  │                  │ Persistent   │   │ │
│  │                  │   Volume     │   │ │
│  │                  │              │   │ │
│  │                  └──────────────┘   │ │
│  └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

Este tutorial cubre todos los aspectos fundamentales para desplegar y gestionar una aplicación Java con base de datos en Kubernetes, desde conceptos básicos hasta consideraciones avanzadas para producción.