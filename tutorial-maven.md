# Tutorial Paso a Paso: Creación de un Proyecto Maven

Este tutorial te guiará a través del proceso completo de creación y configuración de un proyecto Maven desde cero.

## Paso 1: Creación del Proyecto Base con Maven Archetype

### Comando de Creación
```bash
mvn -B archetype:generate \
  -DgroupId=com.example \
  -DartifactId=graphqlexample \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

### Explicación de Parámetros
- **`-B`**: Modo batch (no interactivo), evita que Maven haga preguntas durante la ejecución
- **`archetype:generate`**: Goal de Maven que genera un proyecto desde un archetype (plantilla)
- **`-DgroupId=com.example`**: Identificador único del grupo/organización (sigue convenciones de paquetes Java)
- **`-DartifactId=graphqlexample`**: Nombre del proyecto/artefacto (debe ser único dentro del groupId)
- **`-DarchetypeArtifactId=maven-archetype-quickstart`**: Plantilla básica de Maven con estructura estándar
- **`-DinteractiveMode=false`**: Desactiva el modo interactivo para automatizar la creación

### Estructura Generada
```
graphqlexample/
├── pom.xml
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── example/
    │               └── App.java
    └── test/
        └── java/
            └── com/
                └── example/
                    └── AppTest.java
```

## Paso 2: Configuración de la Versión del Compilador

### Agregar Properties al pom.xml
```xml
<properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Explicación de Properties
- **`maven.compiler.source`**: Versión de Java para el código fuente
- **`maven.compiler.target`**: Versión de Java para los archivos compilados (.class)
- **`project.build.sourceEncoding`**: Codificación de caracteres para los archivos fuente

## Paso 3: Configuración de Dependencias

### 3.1 Dependencias de Test (Scope: test)
```xml
<dependencies>
    <!-- JUnit para pruebas unitarias -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito para mocking en tests -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.5.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3.2 Dependencias del Proyecto (Scope: compile - default)
```xml
<!-- GraphQL Java -->
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java</artifactId>
    <version>21.0</version>
</dependency>

<!-- Jackson para JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- SLF4J para logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.9</version>
</dependency>
```

### Explicación de Scopes
- **`test`**: Dependencias solo disponibles durante pruebas y compilación de tests
- **`compile`** (default): Dependencias disponibles en todas las fases del ciclo de vida
- **`runtime`**: Solo disponibles en tiempo de ejecución, no en compilación
- **`provided`**: Proporcionadas por el contenedor (ej: servlet-api)

## Paso 4: Configuración del Build

### Plugin de Compilador
```xml
<build>
    <plugins>
        <!-- Plugin del compilador Maven -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>11</source>
                <target>11</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Plugin Shade (JAR ejecutable)
```xml
<!-- Plugin para crear JAR con dependencias -->
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
```

### Explicación de Plugins
- **`maven-compiler-plugin`**: Controla la compilación del código Java
- **`maven-shade-plugin`**: Crea un JAR "fat" con todas las dependencias incluidas
- **`ManifestResourceTransformer`**: Configura la clase principal en el MANIFEST.MF

## Paso 5: Comandos Útiles de Maven

### Comandos Básicos
```bash
# Limpiar el proyecto
mvn clean

# Compilar el código fuente
mvn compile

# Ejecutar tests
mvn test

# Empaquetar (crear JAR)
mvn package

# Instalar en repositorio local
mvn install

# Ciclo completo
mvn clean install
```

## Estructura Final del pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/maven-v4_0_0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>graphqlexample</artifactId>
    <packaging>jar</packaging>
    <version>1.0.0</version>
    <name>GraphQL Example</name>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Dependencias del proyecto -->
        <dependency>
            <groupId>com.graphql-java</groupId>
            <artifactId>graphql-java</artifactId>
            <version>21.0</version>
        </dependency>

        <!-- Dependencias de test -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
            
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

## Conceptos Clave de Maven

### Coordenadas de Maven
- **GroupId**: Identifica únicamente la organización o grupo
- **ArtifactId**: Identifica el proyecto específico
- **Version**: Versión del artefacto

### Ciclo de Vida de Maven
1. **validate**: Validar que la información del proyecto es correcta
2. **compile**: Compilar el código fuente
3. **test**: Ejecutar tests unitarios
4. **package**: Crear JAR/WAR
5. **verify**: Ejecutar tests de integración
6. **install**: Instalar en repositorio local
7. **deploy**: Copiar al repositorio remoto

Este tutorial cubre los aspectos fundamentales para crear y configurar un proyecto Maven exitoso.