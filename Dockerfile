# Run scripts/verify-local.ps1 (real isolated MySQL, no skipped tests) before release.
# Render's packaging build does not have the isolated integration-test database.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -B -ntp package -DskipTests
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S safework && adduser -S safework -G safework
COPY --from=build --chown=safework:safework /app/target/service-1.0.0-SNAPSHOT.jar app.jar
COPY --chown=safework:safework --chmod=0555 docker-entrypoint.sh /app/docker-entrypoint.sh
USER safework
ENV APP_PROFILE=prod
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["-jar", "app.jar"]
