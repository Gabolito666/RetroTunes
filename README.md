# 🎵 RetroTunes — Guía de Tecnologías y Replicación del Proyecto

Esta guía contiene la especificación técnica completa, versiones exactas y los pasos necesarios para clonar, configurar y ejecutar el proyecto **RetroTunes** en cualquier otro equipo o dispositivo (Windows, macOS o Linux).

---

## 🛠️ 1. Matriz de Tecnologías y Versiones Requeridas

Para replicar este proyecto de forma exitosa, el nuevo entorno debe cumplir con las siguientes especificaciones:

| Componente | Versión Declarada / Requerida | Propósito / Descripción |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **Java 21 (LTS)** o superior | Entorno de ejecución y compilación base definido en `pom.xml` (`<java.version>21</java.version>`). Se recomienda OpenJDK (Eclipse Temurin, Oracle JDK o Amazon Corretto 21). |
| **Spring Boot** | **4.1.2** (`spring-boot-starter-parent`) | Framework principal para desarrollo de aplicaciones empresariales basadas en Spring. |
| **Apache Maven** | **3.9.x** (Mínimo **3.8.x**) / Wrapper **3.6.3** | Herramienta de gestión y construcción de dependencias. El proyecto incluye los scripts de conveniencia `mvnw` y `mvnw.cmd` que descargan automáticamente Maven 3.6.3+ sin instalación previa. |
| **Spring MVC / Web** | Incluido en Spring Boot | Módulo `spring-boot-starter-web` con servidor embebido **Apache Tomcat** configurado en el puerto `8097`. |
| **Thymeleaf** | Incluido en Spring Boot | Módulo `spring-boot-starter-thymeleaf` para renderizado de vistas dinámicas del lado del servidor (SSR) y fragmentos modulares (`layout.html`). |
| **Spring Data JPA & Hibernate**| Incluido en Spring Boot | Módulo `spring-boot-starter-data-jpa` para persistencia relacional ORM y mapeo de entidades. |
| **Jakarta Validation** | Incluido en Spring Boot | Módulo `spring-boot-starter-validation` para validación de datos en controladores y entidades (`@NotNull`, `@NotBlank`, etc.). |
| **Base de Datos Principal** | **PostgreSQL 15+ / Supabase** | Driver `org.postgresql:postgresql`. Instancia activa en la nube alojada en **Supabase**. |
| **Bases de Datos Alternativas** | **H2 Database** / **MySQL** | Drivers `com.h2database:h2` (modo embebido/memoria) y `com.mysql:mysql-connector-j` disponibles en `pom.xml`. |
| **Frontend Stack** | HTML5, CSS3, JavaScript (ES6+) | Diseño web responsivo retro/vintage con CSS personalizado (`retrotunes.css`) y JavaScript vainilla (`main.js`). |
| **Testing** | JUnit 5, Mockito, AssertJ | Módulo `spring-boot-starter-test` para pruebas unitarias y de integración. |
| **Control de Versiones** | **Git 2.x+** | Para clonación y control de ramas del proyecto. |

---

## 💻 2. Requisitos Previos del Sistema

Antes de iniciar en el nuevo dispositivo, asegúrese de tener instalados los siguientes componentes:

1. **JDK 21 instalado**:
   - Descargar desde [Adoptium / Eclipse Temurin 21](https://adoptium.net/) o [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21).
   - Configurar la variable de entorno `JAVA_HOME` apuntando al directorio raíz del JDK.
   - Agregar `%JAVA_HOME%\bin` (Windows) o `$JAVA_HOME/bin` (Linux/macOS) a la variable de entorno `PATH`.
   - Comprobar en terminal:
     ```bash
     java -version
     ```
     *(Debe indicar `openjdk version "21.x.x"` o superior)*.

2. **Apache Maven (Opcional si usa el Wrapper)**:
   - Si no dispone de Maven instalado globalmente, el proyecto cuenta con el **Maven Wrapper** (`./mvnw` o `mvnw.cmd`) que automatiza este paso.

3. **Disponibilidad de Puertos de Red**:
   - El puerto local **`8097`** debe estar libre (puerto configurado en `application.properties`).
   - Salida hacia internet permitida en el puerto **`5432`** si se conecta a la base de datos de Supabase.

---

## 🗄️ 3. Configuración de Base de Datos y Persistencia

El archivo de configuración principal se encuentra en [`src/main/resources/application.properties`](file:///C:/Spring_Projects/RetroTunes/src/main/resources/application.properties).

### Opción A: Supabase PostgreSQL en la Nube (Configuración por Defecto)
El proyecto viene preconfigurado con una base de datos PostgreSQL alojada en Supabase:
```properties
server.port=8097
spring.datasource.url=jdbc:postgresql://db.zzjmcspwmtxqzxvnhhwv.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=sopVVYBZL2EfqD0y
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

> [!NOTE]
> En la raíz del repositorio se encuentra el archivo [`supabase_init.sql`](file:///C:/Spring_Projects/RetroTunes/supabase_init.sql). Este script incluye:
> - Creación de tablas (`roles`, `usuarios`, `artistas_empresas`, `productos_musica`).
> - Índices B-Tree optimizados (`idx_usuarios_correo`, `idx_productos_sku`).
> - Datos iniciales de prueba (Seeds).
> Si conecta a un proyecto nuevo de Supabase o a un PostgreSQL limpio, ejecute dicho script en el editor SQL.

### Opción B: Base de Datos Local H2 (Modo Offline)
Si el nuevo dispositivo no cuenta con conexión a internet o no tiene acceso a Supabase, puede descomentar la línea H2 en `application.properties`:
```properties
spring.datasource.url=jdbc:h2:file:./data/retrotunesdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
```

---

## 🚀 4. Paso a Paso para Replicar el Proyecto en Otro Dispositivo

### Paso 1: Clonar el Repositorio
```bash
git clone https://github.com/tu-usuario/RetroTunes.git
cd RetroTunes
```

### Paso 2: Verificar la Versión de Java
```bash
java -version
```
Verifique que la salida apunte a la versión 21.

### Paso 3: Compilar y Descargar Dependencias

**En Windows:**
```powershell
.\mvnw.cmd clean compile
```
*O si tiene Maven global instalado:*
```powershell
mvn clean compile
```

**En Linux / macOS:**
```bash
chmod +x mvnw
./mvnw clean compile
```

### Paso 4: Ejecutar la Aplicación

**En Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```
*O con Maven:*
```powershell
mvn spring-boot:run
```

**En Linux / macOS:**
```bash
./mvnw spring-boot:run
```

### Paso 5: Acceder a la Aplicación
Una vez que en la consola se muestre el mensaje de inicio de Tomcat y `DataInitializer`:
- Abra su navegador web en: **`http://localhost:8097`**
- **Credenciales iniciales generadas por el sistema (`DataInitializer`):**
  - **Correo:** `admin@retrotunes.com`
  - **Contraseña:** `admin123`
  - **Rol:** `ADMIN`

---

## 📦 5. Generación de Archivo JAR Autoejecutable

Si desea desplegar la aplicación en un servidor o ejecutarla sin un IDE:

1. Genere el paquete ejecutable:
   ```bash
   mvn clean package -DskipTests
   ```
2. Ejecute el archivo JAR generado en `target/`:
   ```bash
   java -jar target/CRUDBASICOVIERNES-0.0.1-SNAPSHOT.jar
   ```

---

## 📂 6. Estructura General del Proyecto

```text
RetroTunes/
├── .mvn/wrapper/                  # Archivos y configuración de Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/crud_relacional/com/
│   │   │   │   ├── controlador/   # AuthController, CatalogoController
│   │   │   │   ├── entidades/     # Entidades JPA (Usuario, Rol, ProductoMusica, ArtistaEmpresa)
│   │   │   │   └── repositorio/   # Interfaces Spring Data JPA
│   │   │   └── com/example/app/
│   │   │       ├── DemoApplication.java  # Clase principal con @SpringBootApplication
│   │   │       └── DataInitializer.java  # Carga inicial de datos semilla
│   │   └── resources/
│   │       ├── static/            # Archivos estáticos (CSS y JS)
│   │       ├── templates/         # Vistas HTML con Thymeleaf (catalogo, auth, layout)
│   │       └── application.properties # Parámetros del servidor y conexión a base de datos
│   └── test/                      # Pruebas unitarias
├── mvnw / mvnw.cmd                # Scripts Maven Wrapper para Linux/Mac y Windows
├── pom.xml                        # Definición de dependencias y versiones de Maven/Spring
├── supabase_init.sql              # Script DDL e índices para PostgreSQL/Supabase
└── README.md                      # Esta documentación técnica
```
