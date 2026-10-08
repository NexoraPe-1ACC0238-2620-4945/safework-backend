# Java 25 JDK (OpenJDK Temurin)
FROM eclipse-temurin:25-jdk

WORKDIR /app

# Copy your JAR to the container
COPY target/*.jar app.jar

EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
