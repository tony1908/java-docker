# Tutorial de JFrog Artifactory: Registry Docker y Repositorio Maven

Este tutorial te guía paso a paso para configurar y usar JFrog Artifactory como registry de Docker y repositorio de Maven.

## Prerrequisitos

- Cuenta activa en JFrog Artifactory Cloud
- Docker instalado localmente
- Maven instalado localmente
- Proyecto Maven configurado

## Descripción General

JFrog Artifactory es un repositorio universal que permite:
- **Docker Registry**: Almacenar y gestionar imágenes Docker
- **Maven Repository**: Gestionar dependencias y artefactos Maven
- **Control de acceso**: Gestión de usuarios y permisos
- **Integración CI/CD**: Compatible con herramientas de DevOps

## Parte 1: Configuración de Docker Registry

### Paso 1: Acceder a la Administración de JFrog

1. **Acceder a tu instancia de JFrog**
   ```
   https://tu-instancia.jfrog.io
   ```

2. **Navegar a la sección de administración**
   - En la barra superior, encontrarás dos opciones: **Plataforma** y **Administración**
   - Hacer clic en **Administración**

### Paso 2: Crear Repositorio Docker

1. **Ir a Repositorios**
   - En el menú de administración, seleccionar **Repositorios**

2. **Crear nuevo repositorio**
   - Hacer clic en **Nuevo repositorio**
   - Seleccionar **Repository Setup** (configuración preconstruida)

3. **Seleccionar tipo Docker**
   - Elegir **Docker** de la lista de tipos disponibles
   - JFrog creará automáticamente la configuración del registry

4. **Configuración del repositorio**
   - **Nombre del repositorio**: `clase-docker` (o el nombre que prefieras)
   - **Tipo**: Local (para almacenar tus propias imágenes)
   - Mantener configuración por defecto

### Paso 3: Obtener la URL del Registry

Una vez creado el repositorio, JFrog te proporcionará la URL del registry:
```
trial2ot5pk.jfrog.io/clase-docker/
```

**Estructura de la URL**:
- `trial2ot5pk.jfrog.io`: Tu instancia de JFrog
- `clase-docker`: Nombre del repositorio Docker

### Paso 4: Generar Token de Acceso

1. **Ir al perfil de usuario**
   - Hacer clic en tu avatar/perfil (esquina superior derecha)
   - Seleccionar **Edit Profile** (Editar perfil)

2. **Generar Identity Token**
   - En la sección de tokens, hacer clic en **Generate Identity Token**
   - **Descripción**: "Docker Registry Access" (o descripción de tu elección)
   - **Expiración**: Configurar según tus necesidades
   - Hacer clic en **Generate**

3. **Guardar el token**
   - **⚠️ IMPORTANTE**: Copia y guarda el token inmediatamente
   - No podrás verlo nuevamente después de cerrar la ventana

### Paso 5: Login con Docker

1. **Autenticarse en Docker**
   ```bash
   docker login trial2ot5pk.jfrog.io
   ```

2. **Credenciales de acceso**
   - **Username**: Tu nombre de usuario de JFrog
   - **Password**: El Identity Token generado (NO tu contraseña de JFrog)

   ```bash
   Username: tu-usuario-jfrog
   Password: [pegar el token aquí]
   ```

3. **Verificar login exitoso**
   ```
   Login Succeeded
   ```

### Paso 6: Pushear Imagen Docker

1. **Etiquetar la imagen**
   ```bash
   docker tag java-mariadb-app:latest trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app:latest
   ```

2. **Pushear la imagen**
   ```bash
   docker push trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app:latest
   ```

3. **Verificar en JFrog**
   - Ir a **Artifacts** en la interfaz web
   - Navegar a `clase-docker/java-mariadb-app`
   - Deberías ver tu imagen listada

## Parte 2: Configuración de Maven Repository

### Paso 1: Crear Repositorio Maven

1. **Crear nuevo repositorio Maven**
   - Ir a **Administración** > **Repositorios**
   - Hacer clic en **Nuevo repositorio**
   - Seleccionar **Repository Setup**

2. **Seleccionar tipo Maven**
   - Elegir **Maven** de la lista de tipos disponibles
   - JFrog creará automáticamente la configuración

3. **Tipos de repositorios Maven creados**
   JFrog creará automáticamente tres repositorios:
   - **maven-libs-releases**: Para artefactos estables (releases)
   - **maven-libs-snapshots**: Para versiones de desarrollo (snapshots)
   - **maven-libs**: Repositorio virtual que combina ambos

### Paso 2: Obtener Configuración Maven

1. **Ir a Maven Settings**
   - En la interfaz de JFrog, ir a **Artifacts**
   - Seleccionar el repositorio **maven-libs**
   - Hacer clic en **Set Me Up**

2. **Generar settings.xml**
   - JFrog generará automáticamente un archivo `settings.xml`
   - Incluye las URLs de los repositorios
   - Incluye la configuración de autenticación

### Paso 3: Configurar Maven Local

1. **Localizar directorio Maven**
   ```bash
   # Directorio por defecto de Maven
   ~/.m2/settings.xml
   ```

2. **Respaldar configuración existente**
   ```bash
   # Si ya tienes settings.xml, haz un respaldo
   cp ~/.m2/settings.xml ~/.m2/settings.xml.backup
   ```

3. **Copiar nueva configuración**
   - Copiar el contenido del `settings.xml` generado por JFrog
   - Pegar en `~/.m2/settings.xml`

### Paso 4: Ejemplo de settings.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 http://maven.apache.org/xsd/settings-1.2.0.xsd" 
    xmlns="http://maven.apache.org/SETTINGS/1.2.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <servers>
    <server>
      <username>tu-usuario-jfrog</username>
      <password>tu-identity-token</password>
      <id>central</id>
    </server>
    <server>
      <username>tu-usuario-jfrog</username>
      <password>tu-identity-token</password>
      <id>snapshots</id>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <repositories>
        <repository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
          <id>central</id>
          <name>maven-libs</name>
          <url>https://trial2ot5pk.jfrog.io/artifactory/maven-libs</url>
        </repository>
        <repository>
          <snapshots />
          <id>snapshots</id>
          <name>maven-libs</name>
          <url>https://trial2ot5pk.jfrog.io/artifactory/maven-libs</url>
        </repository>
      </repositories>
      
      <pluginRepositories>
        <pluginRepository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
          <id>central</id>
          <name>maven-libs</name>
          <url>https://trial2ot5pk.jfrog.io/artifactory/maven-libs</url>
        </pluginRepository>
        <pluginRepository>
          <snapshots />
          <id>snapshots</id>
          <name>maven-libs</name>
          <url>https://trial2ot5pk.jfrog.io/artifactory/maven-libs</url>
        </pluginRepository>
      </pluginRepositories>
      
      <id>artifactory</id>
    </profile>
  </profiles>
  
  <activeProfiles>
    <activeProfile>artifactory</activeProfile>
  </activeProfiles>
</settings>
```

### Paso 5: Configurar pom.xml para Deployment

Agregar la configuración de `distributionManagement` en tu `pom.xml`:

```xml
<distributionManagement>
    <snapshotRepository>
        <id>snapshots</id>
        <name>trial2ot5pk-maven-libs-snapshots</name>
        <url>https://trial2ot5pk.jfrog.io/artifactory/maven-libs-snapshots</url>
    </snapshotRepository>
    <repository>
        <id>central</id>
        <name>trial2ot5pk-maven-libs-releases</name>
        <url>https://trial2ot5pk.jfrog.io/artifactory/maven-libs-releases</url>
    </repository>
</distributionManagement>
```

### Paso 6: Deployar con Maven

1. **Deploy de snapshot (versión de desarrollo)**
   ```bash
   # Asegúrate de que la versión en pom.xml termine con -SNAPSHOT
   mvn clean deploy
   ```

2. **Deploy de release (versión estable)**
   ```bash
   # Cambia la versión en pom.xml a una sin -SNAPSHOT
   mvn clean deploy
   ```

3. **Verificar en JFrog**
   - Ir a **Artifacts** en JFrog
   - Navegar a los repositorios Maven
   - Verificar que tu artefacto esté presente

## Comandos Útiles

### Docker
```bash
# Ver imágenes locales
docker images

# Descargar imagen desde JFrog
docker pull trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app:latest

# Eliminar imagen local
docker rmi trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app:latest
```

### Maven
```bash
# Verificar configuración Maven
mvn help:effective-settings

# Limpiar y compilar
mvn clean compile

# Ver dependencias
mvn dependency:tree

# Deploy específico a repositorio
mvn deploy -Dmaven.test.skip=true
```

## Integración con Kubernetes

### Actualizar Deployment para usar imagen de JFrog

Modificar `k8s/app-deployment.yaml`:

```yaml
spec:
  template:
    spec:
      containers:
      - name: java-app
        image: trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app:latest
        imagePullPolicy: Always  # Cambiar de Never a Always
```

### Configurar ImagePullSecrets (si es necesario)

1. **Crear secret para Docker registry**
   ```bash
   kubectl create secret docker-registry jfrog-secret \
     --docker-server=trial2ot5pk.jfrog.io \
     --docker-username=tu-usuario \
     --docker-password=tu-token \
     --namespace=java-app
   ```

2. **Usar el secret en el deployment**
   ```yaml
   spec:
     template:
       spec:
         imagePullSecrets:
         - name: jfrog-secret
         containers:
         - name: java-app
           image: trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app:latest
   ```

## Mejores Prácticas

### Seguridad
1. **Usar Identity Tokens** en lugar de contraseñas
2. **Configurar expiración** de tokens apropiada
3. **Principio de menor privilegio** - solo permisos necesarios
4. **Rotar tokens regularmente**

### Versionado
1. **Semantic Versioning**: `major.minor.patch`
2. **Tags descriptivos**: `v1.2.3`, `latest`, `stable`
3. **Snapshots para desarrollo**: `1.0.0-SNAPSHOT`
4. **Releases para producción**: `1.0.0`

### Organización
1. **Estructura consistente** de nombres de repositorios
2. **Metadatos descriptivos** para artefactos
3. **Políticas de retención** para gestionar espacio
4. **Documentación** de procedimientos

