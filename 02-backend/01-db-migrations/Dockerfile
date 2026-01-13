# Build stage
FROM gradle:8.11-jdk21 AS builder

WORKDIR /app

# Copy gradle files first for caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build application
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Environment variables (override at runtime)
ENV DB_HOST=localhost
ENV DB_PORT=3306
ENV DB_NAME=monolitic_db
ENV DB_USERNAME=root
ENV DB_PASSWORD=password

ENTRYPOINT ["java", "-jar", "app.jar"]
