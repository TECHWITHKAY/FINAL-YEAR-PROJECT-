# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy the Maven wrapper scripts and POM
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Ensure the wrapper is executable
RUN chmod +x ./mvnw

# Download dependencies (this step is cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline

# Copy source code and build the application
COPY src src
RUN ./mvnw package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for Docker HEALTHCHECK
RUN apk add --no-cache curl

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the standard Spring Boot port
EXPOSE 8080

# Health check for container orchestration (App Runner, ECS, Docker Compose)
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Activate the production profile via environment variable (can be overridden)
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
