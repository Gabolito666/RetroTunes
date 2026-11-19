# Stage 1: Build con Maven 3.9.6 y JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cachear dependencias copiando únicamente pom.xml primero
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar artefacto JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime optimizado con JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app

# Crear directorio para subida de fotos físicas de equipos
RUN mkdir -p /app/uploads/equipos

# Copiar el ejecutable generado en la etapa de compilación
COPY --from=build /workspace/target/*.jar app.jar

# Configuración de puerto y variables de entorno por defecto
ENV PORT=8080
EXPOSE 8080

# Iniciar aplicación Spring Boot
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${PORT}", "-jar", "app.jar"]
