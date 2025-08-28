# Tutorial de Redes Docker: Conectando Java a MariaDB con Maven

Este tutorial demuestra cómo crear una red personalizada de Docker y conectar una aplicación Java a una base de datos MariaDB usando contenedores Docker y Maven para la gestión de dependencias.

## Prerequisitos

- Docker instalado en tu sistema
- Java Development Kit (JDK) 11 o superior
- Apache Maven 3.6 o superior
- Conocimiento básico de Java y Maven
- Comprensión básica de contenedores Docker

## Paso 1: Crear una Red Personalizada de Docker

Primero, crea una red bridge personalizada que permitirá que nuestros contenedores se comuniquen:

```bash
docker network create myapp-network
```

Verifica que la red fue creada:
```bash
docker network ls
```

## Paso 2: Configurar el Contenedor MariaDB

Ejecuta un contenedor MariaDB conectado a nuestra red personalizada:

```bash
docker run -d \
  --name mariadb-container \
  --network myapp-network \
  -e MARIADB_ROOT_PASSWORD=rootpassword \
  -e MARIADB_DATABASE=myapp \
  -e MARIADB_USER=appuser \
  -e MARIADB_PASSWORD=apppassword \
  -p 3306:3306 \
  mariadb:latest
```

**Variables de entorno explicadas:**
- `MARIADB_ROOT_PASSWORD`: Contraseña del usuario root
- `MARIADB_DATABASE`: Base de datos inicial a crear
- `MARIADB_USER`: Usuario de la aplicación
- `MARIADB_PASSWORD`: Contraseña del usuario de la aplicación

Espera a que MariaDB se inicialice (normalmente 30-60 segundos):
```bash
docker logs mariadb-container
```

Deberías ver un mensaje como "mysqld: ready for connections" cuando esté listo.

## Paso 3: Crear el Proyecto Java Maven

Crea la estructura del directorio del proyecto y Maven:

```bash
mkdir docker-java-mariadb
cd docker-java-mariadb
mvn archetype:generate -DgroupId=com.example \
  -DartifactId=javadb \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
cd javadb
```

### Estructura del Proyecto
```
javadb/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               ├── App.java
│   │               └── DatabaseManager.java
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java
├── Dockerfile
├── docker-compose.yaml
└── pom.xml
```

### pom.xml

Actualiza el archivo de configuración Maven:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/maven-v4_0_0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>javadb</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>javadb</name>
  <url>http://maven.apache.org</url>

  <properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <!-- Driver JDBC de MariaDB -->
    <dependency>
      <groupId>org.mariadb.jdbc</groupId>
      <artifactId>mariadb-java-client</artifactId>
      <version>3.5.2</version>
    </dependency>
    
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.13.2</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- Plugin del Compilador -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <source>11</source>
          <target>11</target>
        </configuration>
      </plugin>

      <!-- Plugin Shade - Crea JAR ejecutable con dependencias -->
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
              <createDependencyReducedPom>false</createDependencyReducedPom>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

</project>
```

### DatabaseManager.java

Crea `src/main/java/com/example/DatabaseManager.java`:

```java
package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_HOST = "mariadb-container"; // Nombre del contenedor como hostname
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "myapp";
    private static final String DB_USER = "appuser";
    private static final String DB_PASSWORD = "apppassword";
    
    private static final String JDBC_URL = String.format(
        "jdbc:mariadb://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME
    );
    
    private Connection connection;
    
    public DatabaseManager() throws SQLException {
        conectar();
        inicializarBaseDeDatos();
    }
    
    private void conectar() throws SQLException {
        try {
            // Cargar el driver JDBC de MariaDB
            Class.forName("org.mariadb.jdbc.Driver");
            
            // Establecer conexión con lógica de reintento
            int reintentos = 5;
            while (reintentos > 0) {
                try {
                    connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
                    System.out.println("✅ ¡Conectado a MariaDB exitosamente!");
                    break;
                } catch (SQLException e) {
                    reintentos--;
                    if (reintentos == 0) throw e;
                    
                    System.out.println("⏳ Reintentando conexión a la base de datos... (" + reintentos + " intentos restantes)");
                    try {
                        Thread.sleep(3000); // Esperar 3 segundos antes del reintento
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Conexión interrumpida", ie);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC de MariaDB no encontrado", e);
        }
    }
    
    private void inicializarBaseDeDatos() throws SQLException {
        String crearTablaSQL = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(crearTablaSQL);
            System.out.println("✅ ¡Tabla de la base de datos inicializada exitosamente!");
        }
    }
    
    public void insertarUsuario(String nombre, String email) throws SQLException {
        String insertarSQL = "INSERT INTO usuarios (nombre, email) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertarSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, email);
            
            int filasAfectadas = pstmt.executeUpdate();
            
            // Obtener el ID generado
            try (ResultSet clavesGeneradas = pstmt.getGeneratedKeys()) {
                if (clavesGeneradas.next()) {
                    int idUsuario = clavesGeneradas.getInt(1);
                    System.out.println("✅ Usuario insertado: " + nombre + " (ID: " + idUsuario + ")");
                }
            }
        }
    }
    
    public void obtenerTodosLosUsuarios() throws SQLException {
        String seleccionarSQL = "SELECT id, nombre, email, creado_en FROM usuarios ORDER BY id";
        
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(seleccionarSQL)) {
            
            System.out.println("\n📋 Todos los Usuarios:");
            System.out.println("=" + "=".repeat(80));
            System.out.printf("%-5s %-20s %-30s %-20s%n", "ID", "Nombre", "Email", "Creado En");
            System.out.println("-" + "-".repeat(80));
            
            int contador = 0;
            while (resultSet.next()) {
                System.out.printf("%-5d %-20s %-30s %-20s%n",
                    resultSet.getInt("id"),
                    resultSet.getString("nombre"),
                    resultSet.getString("email"),
                    resultSet.getTimestamp("creado_en").toString().substring(0, 19)
                );
                contador++;
            }
            
            if (contador == 0) {
                System.out.println("No se encontraron usuarios.");
            } else {
                System.out.println("-" + "-".repeat(80));
                System.out.println("Total de usuarios: " + contador);
            }
        }
    }
    
    public int obtenerContadorUsuarios() throws SQLException {
        String contarSQL = "SELECT COUNT(*) FROM usuarios";
        
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(contarSQL)) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }
    
    public boolean probarConexion() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    public void cerrar() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("✅ Conexión a la base de datos cerrada.");
        }
    }
}
```

### App.java

Actualiza `src/main/java/com/example/App.java`:

```java
package com.example;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class App {
    private static final String VERSION_APP = "1.0-SNAPSHOT";
    
    public static void main(String[] args) {
        DatabaseManager gestorBD = null;
        
        try {
            imprimirEncabezado();
            
            // Inicializar conexión a la base de datos
            System.out.println("🔌 Inicializando conexión a la base de datos...");
            gestorBD = new DatabaseManager();
            
            // Probar conexión
            if (!gestorBD.probarConexion()) {
                throw new RuntimeException("¡Prueba de conexión a la base de datos falló!");
            }
            
            System.out.println("🧪 ¡Prueba de conexión exitosa!");
            
            // Verificar si necesitamos insertar datos de muestra
            int contadorUsuarios = gestorBD.obtenerContadorUsuarios();
            if (contadorUsuarios == 0) {
                System.out.println("\n📝 Insertando datos de muestra...");
                insertarDatosDeMuestra(gestorBD);
            } else {
                System.out.println("\n📊 Se encontraron " + contadorUsuarios + " usuarios existentes en la base de datos");
            }
            
            // Mostrar todos los usuarios
            gestorBD.obtenerTodosLosUsuarios();
            
            // Bucle principal de la aplicación
            System.out.println("\n🚀 ¡La aplicación se está ejecutando exitosamente!");
            System.out.println("💡 Consejo: Puedes conectarte a la base de datos directamente usando:");
            System.out.println("   docker exec -it mariadb-container mysql -u appuser -papppassword myapp");
            
            ejecutarBucleAplicacion();
            
        } catch (SQLException e) {
            System.err.println("❌ Error de base de datos: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error de aplicación: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Limpiar conexión de base de datos
            if (gestorBD != null) {
                try {
                    gestorBD.cerrar();
                } catch (SQLException e) {
                    System.err.println("⚠️ Error cerrando conexión a la base de datos: " + e.getMessage());
                }
            }
        }
    }
    
    private static void imprimirEncabezado() {
        System.out.println("=" + "=".repeat(60));
        System.out.println("🐳 Aplicación Docker Java MariaDB v" + VERSION_APP);
        System.out.println("⏰ Iniciada en: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("=" + "=".repeat(60));
    }
    
    private static void insertarDatosDeMuestra(DatabaseManager gestorBD) throws SQLException {
        String[][] usuariosMuestra = {
            {"Juan Pérez", "juan.perez@ejemplo.com"},
            {"María García", "maria.garcia@ejemplo.com"},
            {"Roberto Martínez", "roberto.martinez@ejemplo.com"},
            {"Ana López", "ana.lopez@ejemplo.com"},
            {"Carlos Rodríguez", "carlos.rodriguez@ejemplo.com"}
        };
        
        for (String[] usuario : usuariosMuestra) {
            try {
                gestorBD.insertarUsuario(usuario[0], usuario[1]);
                Thread.sleep(100); // Pequeña pausa para ver la progresión
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    System.out.println("⚠️ Usuario " + usuario[0] + " ya existe, saltando...");
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private static void ejecutarBucleAplicacion() {
        try {
            int contadorLatido = 0;
            while (true) {
                Thread.sleep(10000); // Dormir por 10 segundos
                contadorLatido++;
                System.out.println("💓 Latido de aplicación #" + contadorLatido + " a las " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                
                // Cada 5º latido (50 segundos), mostrar un mensaje de estado
                if (contadorLatido % 5 == 0) {
                    System.out.println("📊 La aplicación ha estado ejecutándose por " + (contadorLatido * 10) + " segundos");
                }
            }
        } catch (InterruptedException e) {
            System.out.println("\n🛑 Aplicación interrumpida. Cerrando graciosamente...");
            Thread.currentThread().interrupt();
        }
    }
}
```

## Paso 4: Crear Dockerfile

Crea el Dockerfile para construir y ejecutar la aplicación Java:

```dockerfile
# Construcción multi-etapa para tamaño de imagen óptimo
FROM maven:3.8.6-openjdk-11-slim AS constructor

# Establecer directorio de trabajo
WORKDIR /app

# Copiar pom.xml primero para mejor cache de capas de Docker
# Esto permite a Docker cachear el paso de descarga de dependencias
COPY pom.xml .

# Descargar dependencias (esta capa será cacheada si pom.xml no cambia)
RUN mvn dependency:resolve dependency:resolve-sources

# Copiar código fuente
COPY src/ ./src/

# Construir la aplicación
RUN mvn clean package -DskipTests

# Verificar que el JAR fue creado
RUN ls -la target/

# Etapa de ejecución - imagen más pequeña para producción
FROM openjdk:11-jre-slim

# Instalar herramientas útiles para depuración (opcional)
RUN apt-get update && apt-get install -y \
    curl \
    iputils-ping \
    && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root por seguridad
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el archivo JAR desde la etapa constructor
COPY --from=constructor /app/target/javadb-1.0-SNAPSHOT.jar app.jar

# Cambiar propietario al usuario no-root
RUN chown -R appuser:appuser /app
USER appuser

# Agregar verificación de salud
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Exponer puerto (opcional, para futuros endpoints web)
EXPOSE 8080

# Ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]
```

## Paso 5: Crear docker-compose.yaml

Crea un archivo `docker-compose.yaml` para una gestión más fácil:

```yaml
version: '3.9'

services:
  app:
    build: .
    container_name: java-app-container
    depends_on:
      mariadb:
        condition: service_healthy
    networks:
      - myapp-network

  mariadb:
    image: mariadb:latest
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
      retries: 3
    volumes:
      - mariadb-data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - myapp-network

volumes:
  mariadb-data:

networks:
  myapp-network:
    driver: bridge
```

## Paso 6: Construir y Ejecutar la Aplicación

### Usando Docker Compose (Recomendado)

```bash
# Construir y ejecutar todos los servicios
docker-compose up --build

# Ejecutar en segundo plano
docker-compose up --build -d

# Ver logs en tiempo real
docker-compose logs -f

# Ver logs solo de la aplicación
docker-compose logs -f app
```

### Usando comandos Docker manuales

```bash
# Crear la red personalizada
docker network create myapp-network

# Construir la imagen de la aplicación Java
docker build -t java-mariadb-app .

# Ejecutar MariaDB
docker run -d \
  --name mariadb-container \
  --network myapp-network \
  -e MARIADB_ROOT_PASSWORD=rootpassword \
  -e MARIADB_DATABASE=myapp \
  -e MARIADB_USER=appuser \
  -e MARIADB_PASSWORD=apppassword \
  -p 3306:3306 \
  mariadb:latest

# Ejecutar la aplicación Java
docker run -d \
  --name java-app-container \
  --network myapp-network \
  java-mariadb-app
```

## Paso 7: Probar y Monitorear la Aplicación

### Verificar logs de la aplicación
```bash
# Ver logs en tiempo real
docker logs -f java-app-container

# Ver logs recientes
docker logs --tail 50 java-app-container
```

Deberías ver una salida similar a:
```
============================================================
🐳 Aplicación Docker Java MariaDB v1.0-SNAPSHOT
⏰ Iniciada en: 2024-01-15 10:30:45
============================================================
🔌 Inicializando conexión a la base de datos...
⏳ Reintentando conexión a la base de datos... (4 intentos restantes)
✅ ¡Conectado a MariaDB exitosamente!
✅ ¡Tabla de la base de datos inicializada exitosamente!
🧪 ¡Prueba de conexión exitosa!

📝 Insertando datos de muestra...
✅ Usuario insertado: Juan Pérez (ID: 1)
✅ Usuario insertado: María García (ID: 2)
✅ Usuario insertado: Roberto Martínez (ID: 3)
✅ Usuario insertado: Ana López (ID: 4)
✅ Usuario insertado: Carlos Rodríguez (ID: 5)

📋 Todos los Usuarios:
================================================================================
ID    Nombre               Email                          Creado En           
--------------------------------------------------------------------------------
1     Juan Pérez           juan.perez@ejemplo.com         2024-01-15 10:30:45
2     María García         maria.garcia@ejemplo.com       2024-01-15 10:30:45
3     Roberto Martínez     roberto.martinez@ejemplo.com   2024-01-15 10:30:45
4     Ana López            ana.lopez@ejemplo.com          2024-01-15 10:30:46
5     Carlos Rodríguez     carlos.rodriguez@ejemplo.com   2024-01-15 10:30:46
--------------------------------------------------------------------------------
Total de usuarios: 5

🚀 ¡La aplicación se está ejecutando exitosamente!
💡 Consejo: Puedes conectarte a la base de datos directamente usando:
   docker exec -it mariadb-container mysql -u appuser -papppassword myapp
💓 Latido de aplicación #1 a las 10:30:55
```

### Verificar conectividad de base de datos

```bash
# Probar conectividad de red
docker exec java-app-container ping mariadb-container

# Conectar directamente a MariaDB para verificar datos
docker exec -it mariadb-container mysql -u appuser -papppassword myapp -e "SELECT * FROM usuarios;"

# Verificar logs de MariaDB
docker logs mariadb-container
```

### Monitorear ambos contenedores

```bash
# Verificar estado de contenedores
docker ps

# Verificar detalles de la red
docker network inspect myapp-network

# Ver uso de recursos
docker stats java-app-container mariadb-container
```

## Paso 8: Desarrollo y Depuración

### Reconstruir después de cambios en el código

```bash
# Usando Docker Compose
docker-compose down
docker-compose up --build

# Usando comandos Docker manuales
docker build -t java-mariadb-app .
docker stop java-app-container
docker rm java-app-container
docker run -d --name java-app-container --network myapp-network java-mariadb-app
```

### Depuración dentro de contenedores

```bash
# Acceder al shell del contenedor Java
docker exec -it java-app-container /bin/bash

# Acceder al shell del contenedor MariaDB
docker exec -it mariadb-container /bin/bash

# Ejecutar comandos SQL directamente
docker exec -it mariadb-container mysql -u appuser -papppassword myapp
```

### Ver dependencias de Maven

```bash
# Verificar qué dependencias descargó Maven
docker run --rm -v $(pwd):/app -w /app maven:3.8.6-openjdk-11-slim mvn dependency:tree
```

## Paso 9: Limpieza

Cuando termines las pruebas, limpia los recursos:

```bash
# Usando Docker Compose
docker-compose down -v

# Usando comandos Docker manuales
# Detener contenedores
docker stop java-app-container mariadb-container

# Remover contenedores
docker rm java-app-container mariadb-container

# Remover la red
docker network rm myapp-network

# Remover la imagen (opcional)
docker rmi java-mariadb-app

# Limpiar artefactos de construcción de Maven localmente (opcional)
mvn clean
```

## Conceptos Clave Explicados

### Redes de Docker
- **Red Bridge Personalizada**: Los contenedores en la misma red personalizada pueden comunicarse usando nombres de contenedores como hostnames
- **Aislamiento de Red**: Las redes personalizadas aíslan tu stack de aplicaciones de otros contenedores Docker
- **Resolución de Nombres de Contenedores**: `mariadb-container` se resuelve automáticamente a la dirección IP del contenedor MariaDB

### Beneficios de Maven
- **Gestión de Dependencias**: Descarga y gestiona automáticamente las dependencias JAR
- **Ciclo de Vida de Construcción**: Fases estandarizadas (compile, test, package)
- **Creación de Fat JAR**: El plugin Shade agrupa todas las dependencias en un solo JAR ejecutable
- **Integración con Docker**: La construcción multi-etapa separa el entorno de construcción del de ejecución

### Arquitectura de la Aplicación Java
- **Pool de Conexiones**: Usa una sola conexión con lógica de reintento
- **Cierre Gracioso**: Cierra apropiadamente las conexiones de base de datos
- **Manejo de Errores**: Manejo comprensivo de excepciones con mensajes significativos
- **Monitoreo**: Latido incorporado y reportes de estado

## Consideraciones para Producción

Para despliegues de producción, considera estas mejoras:

1. **Variables de Entorno**: Usar variables de entorno de Docker para configuración
2. **Gestión de Secretos**: Usar secretos de Docker o gestión externa de secretos
3. **Pool de Conexiones**: Implementar pool de conexiones apropiado (HikariCP)
4. **Logging**: Usar logging estructurado con frameworks como Logback
5. **Verificaciones de Salud**: Implementar endpoints apropiados de verificación de salud
6. **Monitoreo**: Agregar métricas de aplicación y monitoreo
7. **Seguridad**: Ejecutar contenedores como usuarios no-root, escanear vulnerabilidades
8. **Persistencia**: Usar volúmenes de Docker para persistencia de datos de MariaDB

### Ejemplo con Variables de Entorno

```bash
# Ejecutar con variables de entorno
docker run -d \
  --name java-app-container \
  --network myapp-network \
  -e DB_HOST=mariadb-container \
  -e DB_USER=appuser \
  -e DB_PASSWORD=apppassword \
  -e DB_NAME=myapp \
  java-mariadb-app
```

Este tutorial proporciona una base completa para ejecutar aplicaciones Java con MariaDB usando redes Docker y Maven para gestión de construcción.