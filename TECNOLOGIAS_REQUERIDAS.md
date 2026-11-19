# 🛠️ Tecnologías y Requisitos para Replicar RetroTunes

Este documento describe detalladamente la totalidad del stack tecnológico, versiones exactas y los pasos requeridos para reproducir el entorno y ejecutar el proyecto **RetroTunes** en otra máquina o dispositivo.

---

## 📌 1. Ficha Técnica del Proyecto

- **Nombre del Proyecto:** RetroTunes
- **Artifact ID (Maven):** `CRUDBASICOVIERNES`
- **Versión:** `0.0.1-SNAPSHOT`
- **Puerto predeterminado:** `8097` (Accesible en `http://localhost:8097`)

---

## 💻 2. Versiones de Software y Herramientas Necesarias

### 2.1. Java Development Kit (JDK)
* **Versión requerida:** **Java 21 (LTS)** o superior.
* **Declaración en configuración (`pom.xml`):**
  ```xml
  <properties>
      <java.version>21</java.version>
  </properties>
  ```
* **Distribución recomendada:** Eclipse Temurin (OpenJDK 21), Oracle JDK 21 o Amazon Corretto 21.
* **Comprobación en consola:**
  ```bash
  java -version
  javac -version
  ```

### 2.2. Apache Maven
* **Versión instalada/recomendada:** **3.9.x** (Mínimo **3.8.x**).
* **Maven Wrapper Integrado:** El proyecto incluye Maven Wrapper configurado con Apache Maven **3.6.3** (`.mvn/wrapper/maven-wrapper.properties`).
* **Uso sin instalación:**
  - En Windows: `.\mvnw.cmd`
  - En Linux / macOS: `./mvnw`
* **Comprobación en consola:**
  ```bash
  mvn -version
  ```

### 2.3. Spring Framework & Spring Boot
* **Versión de Spring Boot:** **`4.1.2`**
* **Parent POM (`pom.xml`):**
  ```xml
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>4.1.2</version>
      <relativePath/>
  </parent>
  ```

---

## 📚 3. Módulos y Librerías del Stack Tecnológico

| Módulo / Dependencia | Artifact Maven | Función en el Proyecto |
| :--- | :--- | :--- |
| **Spring Boot Web** | `spring-boot-starter-web` | Implementación del patrón MVC, arquitectura REST, enrutamiento HTTP y servidor web embebido **Apache Tomcat**. |
| **Thymeleaf** | `spring-boot-starter-thymeleaf` | Motor de plantillas HTML para renderizado del lado del servidor (SSR), integración con atributos `th:*` y layouts modulares. |
| **Spring Data JPA** | `spring-boot-starter-data-jpa` | Abstracción de acceso a datos relacionales con **Hibernate ORM** y repositorios JPA automáticos. |
| **Jakarta Validation** | `spring-boot-starter-validation` | Validación declarativa de modelos y formularios (`@NotBlank`, `@NotNull`, `@Min`, `@DecimalMin`, etc.). |
| **Spring Boot DevTools** | `spring-boot-devtools` | Reinicio automático rápido y soporte para recarga en caliente durante el desarrollo. |
| **PostgreSQL Driver** | `postgresql` | Conector JDBC oficial para PostgreSQL, empleado para conectar con la base de datos en **Supabase**. |
| **H2 Database Driver** | `h2` | Motor de base de datos SQL embebido / en memoria, preparado para modo de desarrollo local o testing aislado. |
| **MySQL Driver** | `mysql-connector-j` | Conector oficial de MySQL disponible para migraciones o despliegues locales alternativos. |
| **Spring Boot Test** | `spring-boot-starter-test` | Entorno de pruebas con JUnit Jupiter (JUnit 5), Mockito y Spring Boot Test. |

---

## 🗄️ 4. Infraestructura de Base de Datos

### Conexión Activa: Supabase PostgreSQL (Cloud)
- **Host:** `db.zzjmcspwmtxqzxvnhhwv.supabase.co`
- **Puerto:** `5432`
- **Modo DDL:** `update` (Hibernate crea o actualiza las tablas faltantes automáticamente al iniciar).
- **Script SQL de referencia:** [`supabase_init.sql`](file:///C:/Spring_Projects/RetroTunes/supabase_init.sql) con la definición de tablas `roles`, `usuarios`, `artistas_empresas`, `productos_musica`, claves foráneas e índices B-Tree.

### Conexión Alternativa Offline (H2)
En caso de trabajar en un dispositivo sin conexión externa o sin acceso al puerto `5432`:
Modificar [`src/main/resources/application.properties`](file:///C:/Spring_Projects/RetroTunes/src/main/resources/application.properties):
```properties
spring.datasource.url=jdbc:h2:file:./data/retrotunesdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
```

---

## 🚀 5. Procedimiento para Replicar en Otro Dispositivo

1. **Instalar Git y JDK 21:**
   Asegurar que `JAVA_HOME` esté configurado en las variables de entorno del sistema.

2. **Descargar / Clonar el proyecto:**
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd RetroTunes
   ```

3. **Compilar y descargar dependencias:**
   - En Windows:
     ```powershell
     .\mvnw.cmd clean package -DskipTests
     ```
   - En Linux / macOS:
     ```bash
     chmod +x mvnw
     ./mvnw clean package -DskipTests
     ```

4. **Ejecutar la aplicación:**
   - En Windows:
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - En Linux / macOS:
     ```bash
     ./mvnw spring-boot:run
     ```
   - O directamente con el JAR compilado:
     ```bash
     java -jar target/CRUDBASICOVIERNES-0.0.1-SNAPSHOT.jar
     ```

5. **Verificación de Funcionamiento:**
   - Navegar a `http://localhost:8097`
   - La clase `DataInitializer` creará de manera automática los roles y el usuario administrador de prueba si la base de datos está vacía:
     - **Usuario:** `admin@retrotunes.com`
     - **Contraseña:** `admin123`
