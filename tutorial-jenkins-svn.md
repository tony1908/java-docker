# Tutorial Completo: Jenkins Pipeline con SVN para Proyecto Java-MariaDB-Kafka

Este tutorial explica paso a paso cómo configurar y usar Jenkins con SVN como sistema de control de versiones para automatizar el CI/CD del proyecto Java con MariaDB y Kafka.

## Arquitectura del Pipeline

El pipeline automatiza:
1. **Checkout SVN**: Descarga código desde repositorio SVN
2. **Test & Build**: Compila y construye imagen Docker
3. **Push**: Sube imagen a JFrog Artifactory
4. **Deploy**: Despliega en Kubernetes
5. **Notifications**: Envía notificaciones por email

## Paso 1: Configuración de Jenkins

### 1.1 Configurar Plugins de Jenkins

1. **Acceder a Jenkins**: `http://localhost:8080`

2. **Instalar plugins necesarios**:
   - Ir a **Manage Jenkins** → **Manage Plugins**
   - Instalar plugins adicionales:
     - Subversion Plugin
     - Docker Pipeline Plugin
     - Kubernetes Plugin
     - Email Extension Plugin

### 1.2 Configurar Credenciales SVN en Jenkins

1. **Ir a "Manage Jenkins" → "Manage Credentials"**
2. **Agregar credenciales tipo "Username with password"**
   - **ID**: `svn-credentials`
   - **Username**: Usuario SVN
   - **Password**: Contraseña SVN
   - **Description**: "SVN Repository Credentials"

## Paso 2: Estructura de Repositorio SVN

```
java-app-repo/
├── trunk/                  # Desarrollo principal
│   ├── src/
│   ├── k8s/
│   ├── pom.xml
│   ├── Dockerfile
│   └── Jenkinsfile
├── branches/               # Ramas de desarrollo
│   ├── feature-kafka/
│   └── feature-monitoring/
└── tags/                   # Versiones liberadas
    ├── v1.0.0/
    └── v1.1.0/
```

## Paso 3: Análisis del Jenkinsfile Actual

### 3.1 Jenkinsfile Existente (Adaptado para SVN)

```groovy
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app'
        JFROG_CREDENTIALS = credentials('jfrog')
        SVN_URL = 'http://svn-server/svn/java-app-repo/trunk'
        PATH = "/usr/local/bin:${env.PATH}"
    }

    triggers {
        // Verificar cambios en SVN cada minuto
        pollSCM('* * * * *')
    }

    stages {
        stage('Checkout SVN') {
            steps {
                checkout([
                    $class: 'SubversionSCM',
                    locations: [[
                        credentialsId: 'svn-credentials',
                        depthOption: 'infinity',
                        ignoreExternalsOption: true,
                        local: '.',
                        remote: env.SVN_URL
                    ]],
                    workspaceUpdater: [$class: 'UpdateUpdater']
                ])
            }
        }

        stage('Test & Build') {
            steps {
                script {
                    // Ejecutar tests Maven (comentado en original)
                    // sh 'mvn clean test'
                    
                    // Construir imagen Docker
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} ."
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push to Artifactory') {
            steps {
                script {
                    // Login a JFrog Artifactory
                    sh "echo \$JFROG_CREDENTIALS_PSW | docker login trial2ot5pk.jfrog.io --username \$JFROG_CREDENTIALS_USR --password-stdin"
                    
                    // Push con número de build y latest
                    sh "docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                    sh "docker push ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                // Desplegar en orden correcto
                sh '/usr/local/bin/kubectl apply -f k8s/namespace.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/app-configmap.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/app-secret.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/mariadb-pvc.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/mariadb-deployment.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/mariadb-service.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/kafka-deployment.yaml'
                sh '/usr/local/bin/kubectl apply -f k8s/app-deployment.yaml'
                
                // Verificar despliegue
                sh '/usr/local/bin/kubectl rollout status deployment/app-deployment -n java-app --timeout=300s'
            }
        }
    }

    post {
        always {
            // Limpiar imágenes Docker locales
            sh "docker rmi ${DOCKER_IMAGE}:${BUILD_NUMBER} || true"
        }
        
        success {
            mail to: 'toony1908@gmail.com', 
                 subject: "✅ Build Success: ${JOB_NAME} - ${BUILD_NUMBER}", 
                 body: """
                 Build completado exitosamente!
                 
                 Job: ${JOB_NAME}
                 Build: ${BUILD_NUMBER}
                 SVN Revision: ${env.SVN_REVISION}
                 
                 Ver logs: ${BUILD_URL}
                 """
        }
        
        failure {
            mail to: 'toony1908@gmail.com', 
                 subject: "❌ Build Failed: ${JOB_NAME} - ${BUILD_NUMBER}", 
                 body: """
                 Build falló!
                 
                 Job: ${JOB_NAME}
                 Build: ${BUILD_NUMBER}
                 Error en: ${env.STAGE_NAME}
                 
                 Ver logs: ${BUILD_URL}
                 """
        }
    }
}
```

## Paso 4: Configurar Job en Jenkins

### 4.1 Crear Pipeline Job

1. **New Item → Pipeline**
2. **Nombre**: `java-app-svn-pipeline`
3. **Configuración**:

#### General
- ✅ **Build Triggers**: Poll SCM (`* * * * *`)

#### Pipeline
- **Definition**: Pipeline script from SCM
- **SCM**: Subversion
- **Repository URL**: `http://svn-server/svn/java-app-repo/trunk`
- **Credentials**: Seleccionar `svn-credentials`
- **Script Path**: `Jenkinsfile`

### 4.2 Configuración de Poll SCM

```cron
# Verificar cada minuto (desarrollo)
* * * * *

# Verificar cada 5 minutos (recomendado)
H/5 * * * *

# Solo horario laboral
H/10 8-18 * * 1-5
```

## Paso 5: Workflow de Desarrollo con SVN

### 5.1 Comandos SVN Básicos

```bash
# Checkout inicial
svn checkout http://svn-server/svn/java-app-repo/trunk java-app

# Ver estado
svn status

# Agregar archivos nuevos
svn add nuevo-archivo.java

# Commit cambios
svn commit -m "Agregar nueva funcionalidad"

# Actualizar desde servidor
svn update

# Ver log
svn log -l 10
```

### 5.2 Branching en SVN

```bash
# Crear branch
svn copy http://svn-server/svn/java-app-repo/trunk \
         http://svn-server/svn/java-app-repo/branches/feature-nueva \
         -m "Crear branch para nueva funcionalidad"

# Checkout branch
svn checkout http://svn-server/svn/java-app-repo/branches/feature-nueva

# Merge branch a trunk
svn checkout http://svn-server/svn/java-app-repo/trunk
svn merge http://svn-server/svn/java-app-repo/branches/feature-nueva
svn commit -m "Merge feature-nueva to trunk"
```

## Paso 6: Verificación del Pipeline

### 6.1 Ejecutar Build Manual

1. **Ir al job**: `http://localhost:8080/job/java-app-svn-pipeline/`
2. **Hacer clic en "Build Now"**
3. **Ver progreso en tiempo real**

### 6.2 Verificar Logs

- **Console Output**: Ver logs detallados del build
- **Blue Ocean**: Vista moderna del pipeline (si está instalado)
- **Pipeline Steps**: Ver cada stage individualmente

## Paso 7: Monitoreo y Logs

### 7.1 Ver Logs del Pipeline

1. **En Jenkins UI**: 
   - Ir a `http://localhost:8080/job/java-app-svn-pipeline/`
   - Hacer clic en el número del build
   - Seleccionar **Console Output**

2. **Ver logs en tiempo real**:
   - Durante el build, hacer clic en el progreso
   - Los logs se actualizan automáticamente

### 7.2 Verificar Despliegue

```bash
# Verificar pods en Kubernetes
kubectl get pods -n java-app

# Ver logs de la aplicación
kubectl logs -f deployment/app-deployment -n java-app

# Verificar servicios
kubectl get services -n java-app
```

## Paso 8: Comandos Útiles

### 8.1 SVN Commands
```bash
# Status del working copy
svn status

# Update desde servidor
svn update

# Commit cambios
svn commit -m "Mensaje del commit"

# Ver diferencias
svn diff

# Ver log
svn log -l 10

# Info del repositorio
svn info
```

### 8.2 Docker & Kubernetes
```bash
# Ver imágenes Docker
docker images | grep java-mariadb-app

# Ver deployments en Kubernetes
kubectl get deployments -n java-app

# Ver logs del deployment
kubectl logs -f deployment/app-deployment -n java-app

# Rollback deployment
kubectl rollout undo deployment/app-deployment -n java-app
```

## Paso 9: Mejores Prácticas

### 9.1 Estructura del Repositorio SVN

```
java-app-repo/
├── trunk/                          # Línea principal de desarrollo
│   ├── src/main/java/             # Código fuente
│   ├── src/test/java/             # Tests unitarios
│   ├── k8s/                       # Manifiestos Kubernetes
│   ├── scripts/                   # Scripts de deployment
│   ├── docs/                      # Documentación
│   ├── Jenkinsfile               # Pipeline principal
│   ├── Dockerfile                # Imagen Docker
│   └── pom.xml                   # Configuración Maven
├── branches/                      # Ramas de desarrollo
│   ├── feature-monitoring/        # Nueva funcionalidad
│   ├── hotfix-security/          # Corrección urgente
│   └── release-1.1/              # Rama de release
└── tags/                         # Versiones liberadas
    ├── v1.0.0/                   # Release 1.0.0
    ├── v1.0.1/                   # Hotfix 1.0.1
    └── v1.1.0/                   # Release 1.1.0
```

### 9.2 Estrategia de Branching

```bash
# Feature branch
svn copy trunk branches/feature-nueva-funcionalidad
# Desarrollar en branch
# Merge a trunk cuando esté listo

# Release branch
svn copy trunk branches/release-1.1
# Stabilizar en release branch
# Tag cuando esté listo
svn copy branches/release-1.1 tags/v1.1.0

# Hotfix
svn copy tags/v1.0.0 branches/hotfix-seguridad
# Fix en branch
# Merge a trunk y a release branch si existe
svn copy branches/hotfix-seguridad tags/v1.0.1
```

### 9.3 Pipeline por Entornos

```groovy
def deployToEnvironment(environment) {
    script {
        def namespace = environment == 'prod' ? 'java-app' : "java-app-${environment}"
        def imageTag = environment == 'prod' ? 'latest' : BUILD_NUMBER
        
        sh """
            # Update image tag in deployment
            sed -i 's|${DOCKER_IMAGE}:.*|${DOCKER_IMAGE}:${imageTag}|' k8s/app-deployment.yaml
            
            # Apply to specific namespace
            kubectl apply -f k8s/ -n ${namespace}
            kubectl rollout status deployment/app-deployment -n ${namespace} --timeout=300s
        """
        
        // Health check
        sh """
            kubectl wait --for=condition=ready pod -l app=app -n ${namespace} --timeout=300s
        """
    }
}

pipeline {
    stages {
        stage('Deploy Dev') {
            steps {
                deployToEnvironment('dev')
            }
        }
        
        stage('Deploy Staging') {
            steps {
                deployToEnvironment('staging')
            }
        }
        
        stage('Deploy Production') {
            input {
                message "Deploy to production?"
                ok "Deploy"
                parameters {
                    choice(name: 'CONFIRM', choices: ['No', 'Yes'], description: 'Confirm production deployment')
                }
            }
            when {
                environment name: 'CONFIRM', value: 'Yes'
            }
            steps {
                deployToEnvironment('prod')
            }
        }
    }
}
```

## Paso 10: Comandos de Referencia

### Jenkins
```bash
# Acceder a Jenkins
http://localhost:8080

# Crear nuevo job: New Item → Pipeline
# Configurar SCM: Subversion
# Repository URL: http://svn-server/svn/java-app-repo/trunk
```

### SVN Commands
```bash
# Status del working copy
svn status

# Update desde servidor
svn update

# Commit cambios
svn commit -m "Mensaje del commit"

# Ver diferencias
svn diff

# Ver log
svn log -l 10

# Info del repositorio
svn info
```

### 10.2 Docker & Kubernetes
```bash
# Ver imágenes Docker
docker images | grep java-mariadb-app

# Ver deployments en Kubernetes
kubectl get deployments -n java-app

# Ver logs del deployment
kubectl logs -f deployment/app-deployment -n java-app

# Rollback deployment
kubectl rollout undo deployment/app-deployment -n java-app
```

Este tutorial cubre la configuración y uso de Jenkins con SVN para automatizar el CI/CD del proyecto Java-MariaDB-Kafka de forma sencilla y práctica.

