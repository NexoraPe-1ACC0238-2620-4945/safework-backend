# Stage 1: Build con JDK 25
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Instalar Maven en la imagen con JDK 25
RUN apk add --no-cache maven

# Copiar configuración y dependencias primero para aprovechar el caché de Docker
COPY pom.xml .
COPY src ./src

# Compilar omitiendo los tests
RUN mvn clean package -DskipTests

# Stage 2: Runtime liviano con JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copiar el artefacto generado
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]