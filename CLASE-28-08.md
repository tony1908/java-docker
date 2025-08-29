# Tutorial Completo: Java + MariaDB con Docker, Podman y Kubernetes

## 1. Guía Rápida: Ejecutar con Podman Pod (sin Redis)

### Prerrequisitos
- Podman instalado

### Pasos para ejecutar

```bash
# 1. Crear un pod (equivalente a docker-compose network)
podman pod create --name java-app-pod

# 2. Ejecutar MariaDB en el pod
podman run -d \
  --name mariadb-container \
  --pod java-app-pod \
  -e MARIADB_ROOT_PASSWORD=rootpassword \
  -e MARIADB_DATABASE=myapp \
  -e MARIADB_USER=appuser \
  -e MARIADB_PASSWORD=apppassword \
  mariadb

# 3. Esperar a que MariaDB esté listo (opcional)
sleep 10

# 4. Construir la imagen de la aplicación Java
podman build -t java-mariadb-app .

# 5. Ejecutar la aplicación Java en el mismo pod
podman run -d \
  --name java-app-container \
  --pod java-app-pod \
  -e DB_HOST=localhost \
  -e DB_PORT=3306 \
  -e DB_NAME=myapp \
  -e DB_USER=appuser \
  -e DB_PASSWORD=apppassword \
  java-mariadb-app

# Verificar que los contenedores estén ejecutándose
podman ps

# Ver información del pod
podman pod ps

# Ver logs de la aplicación
podman logs java-app-container

# Ver logs de MariaDB
podman logs mariadb-container

# Detener y eliminar todo
podman pod stop java-app-pod
podman pod rm java-app-pod
```

### Ventajas de usar Pods en Podman
- **Compartición de red**: Los contenedores en el mismo pod comparten la interfaz de red
- **Comunicación localhost**: Los contenedores pueden comunicarse usando `localhost`
- **Gestión unificada**: El pod actúa como unidad de despliegue
- **Sin privilegios root**: Los contenedores se ejecutan sin privilegios especiales

---

## 2. Tutorial de Comandos Básicos de Kubernetes

### Instalación previa
Asegúrate de tener `kubectl` instalado y configurado para conectar a tu clúster.

### 2.1 Listar Namespaces

```bash
kubectl get namespaces
```
**Salida esperada:**
```
NAME              STATUS   AGE
default           Active   1d
kube-node-lease   Active   1d
kube-public       Active   1d
kube-system       Active   1d
```

### 2.2 Listar Namespaces (versión corta)

```bash
kubectl get ns
```
**Salida esperada:**
```
NAME              STATUS   AGE
default           Active   1d
kube-node-lease   Active   1d
kube-public       Active   1d
kube-system       Active   1d
```

### 2.3 Crear un Namespace

```bash
kubectl create namespace tutorial-app
```
**Salida esperada:**
```
namespace/tutorial-app created
```

### 2.4 Ejecutar un Pod con Nginx

```bash
kubectl run nginx --image=nginx --port=80 -n tutorial-app
```
**Salida esperada:**
```
pod/nginx created
```

### 2.5 Listar Deployments

```bash
kubectl get deployments -n tutorial-app
```
**Salida esperada (inicialmente vacía):**
```
No resources found in tutorial-app namespace.
```

**Nota:** El comando anterior creó un Pod, no un Deployment. Para crear un Deployment:

```bash
kubectl create deployment nginx-deployment --image=nginx --port=80 -n tutorial-app
```
**Salida esperada:**
```
deployment.apps/nginx-deployment created
```

Ahora verificamos:
```bash
kubectl get deployments -n tutorial-app
```
**Salida esperada:**
```
NAME               READY   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment   1/1     1            1           30s
```

### 2.6 Exponer Deployment como ClusterIP

```bash
kubectl expose deployment nginx-deployment --port=80 --target-port=80 --type=ClusterIP -n tutorial-app
```
**Salida esperada:**
```
service/nginx-deployment exposed
```

### 2.7 Exponer Deployment como NodePort

```bash
kubectl expose deployment nginx-deployment --port=80 --target-port=80 --type=NodePort --name=nginx-nodeport -n tutorial-app
```
**Salida esperada:**
```
service/nginx-nodeport exposed
```

### 2.8 Listar Servicios

```bash
kubectl get services -n tutorial-app
```
**Salida esperada:**
```
NAME               TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
nginx-deployment   ClusterIP   10.96.123.45    <none>        80/TCP         2m
nginx-nodeport     NodePort    10.96.67.89     <none>        80:30123/TCP   1m
```

### 2.9 Describir Servicio NodePort

```bash
kubectl describe service nginx-nodeport -n tutorial-app
```
**Salida esperada:**
```
Name:                     nginx-nodeport
Namespace:                tutorial-app
Labels:                   app=nginx-deployment
Annotations:              <none>
Selector:                 app=nginx-deployment
Type:                     NodePort
IP Family Policy:        SingleStack
IP Families:              IPv4
IP:                       10.96.67.89
IPs:                      10.96.67.89
Port:                     <unset>  80/TCP
TargetPort:               80/TCP
NodePort:                 <unset>  30123/TCP
Endpoints:                10.244.0.15:80
Session Affinity:         None
External Traffic Policy:  Cluster
Events:                   <none>
```

### 2.10 Port Forward para Acceso Local

```bash
kubectl port-forward service/nginx-deployment 8080:80 -n tutorial-app
```
**Salida esperada:**
```
Forwarding from 127.0.0.1:8080 -> 80
Forwarding from [::1]:8080 -> 80
```

Ahora puedes acceder a http://localhost:8080 en tu navegador.

---

## 3. Variables de Entorno en DatabaseManager

### 3.1 Cómo Funciona

El archivo `src/main/java/com/example/DatabaseManager.java` utiliza variables de entorno para la configuración de la base de datos:

```java
private static final String DB_HOST = System.getenv("DB_HOST");
private static final String DB_PORT = System.getenv("DB_PORT");
private static final String DB_NAME = System.getenv("DB_NAME");
private static final String DB_USER = System.getenv("DB_USER");
private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
```

### 3.2 Variables Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DB_HOST` | Hostname o IP de la base de datos | `mariadb-container` |
| `DB_PORT` | Puerto de conexión | `3306` |
| `DB_NAME` | Nombre de la base de datos | `myapp` |
| `DB_USER` | Usuario de la base de datos | `appuser` |
| `DB_PASSWORD` | Contraseña del usuario | `apppassword` |

### 3.3 Configuración en Docker Compose

Las variables se configuran automáticamente en el `docker-compose.yaml`:

```yaml
services:
  app:
    build: .
    environment:
      - DB_HOST=mariadb-container
      - DB_PORT=3306
      - DB_NAME=myapp
      - DB_USER=appuser
      - DB_PASSWORD=apppassword
```

### 3.4 Configuración Manual (desarrollo local)

Para ejecutar localmente sin Docker:

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=myapp
export DB_USER=appuser
export DB_PASSWORD=apppassword

# Ejecutar la aplicación Java
java -cp target/classes:target/lib/* com.example.App
```

### 3.5 Buenas Prácticas de Seguridad

1. **Nunca hardcodear credenciales** en el código fuente
2. **Usar secrets** en Kubernetes para información sensible
3. **Rotar credenciales** regularmente
4. **Principio de menor privilegio** para usuarios de BD

---

## 4. Archivos YAML de Kubernetes - Explicación Detallada

### 4.1 namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: java-app
  labels:
    name: java-app
```

**Explicación:**
- **apiVersion: v1**: Versión de la API de Kubernetes para Namespaces
- **kind: Namespace**: Tipo de recurso (Namespace)
- **metadata**: Metadatos del namespace
  - **name**: Nombre del namespace (`java-app`)
  - **labels**: Etiquetas para organización y selección

**Propósito:** Crear un namespace aislado para nuestra aplicación Java y MariaDB.

### 4.2 mariadb-deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mariadb-deployment
  namespace: java-app
  labels:
    app: mariadb
spec:
  replicas: 2
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
```

**Explicación:**
- **apiVersion: apps/v1**: API de aplicaciones v1 para Deployments
- **kind: Deployment**: Tipo de recurso para desplegar aplicaciones
- **spec.replicas: 2**: Número de réplicas del pod (alta disponibilidad)
- **spec.selector**: Selecciona pods con label `app: mariadb`
- **template**: Plantilla para crear pods
- **containers**: Lista de contenedores en el pod
- **env**: Variables de entorno para configurar MariaDB

**Propósito:** Desplegar MariaDB con 2 réplicas para alta disponibilidad.

### 4.3 mariadb-service.yaml

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

**Explicación:**
- **kind: Service**: Tipo de recurso para exponer pods
- **spec.selector**: Selecciona pods con label `app: mariadb`
- **ports**: Configuración de puertos
  - **port: 3306**: Puerto del servicio
  - **targetPort: 3306**: Puerto del contenedor
- **type: ClusterIP**: Solo accesible dentro del clúster

**Propósito:** Exponer MariaDB internamente para que la aplicación Java pueda conectarse.

### 4.4 app-deployment.yaml

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
          image: java-mariadb-app:latest
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
```

**Explicación:**
- **image: java-mariadb-app:latest**: Imagen personalizada de nuestra app
- **env**: Variables de entorno para conectar con MariaDB
  - **DB_HOST: mariadb-service**: Utiliza el nombre del servicio de MariaDB
- **replicas: 1**: Una sola réplica de la aplicación

**Propósito:** Desplegar la aplicación Java configurada para conectarse a MariaDB.


