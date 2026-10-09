# Stage 1: Build de la aplicación con JDK 25
FROM maven:3.9.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Copia de archivos de configuración y código fuente
COPY pom.xml .
COPY src ./src

# Compilación omitiendo pruebas
RUN mvn clean package -DskipTests

# Stage 2: Imagen final de ejecución con JDK 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copia del ejecutable generado en el stage 1
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]