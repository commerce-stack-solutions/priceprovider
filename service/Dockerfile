# Stage 1: Build stage
# Use a JDK image to build the application
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory in the container
WORKDIR /workspace/app

# Copy the Gradle wrapper and build files to download dependencies
COPY gradlew .
COPY gradlew.bat .
COPY gradle ./gradle
COPY build.gradle .

# Copy the source code
COPY src ./src

# Grant execution rights to the Gradle wrapper
# Also ensure gradlew uses LF line endings (strip CRLF) so it runs inside /bin/sh
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Build the application, creating a fat JAR.
# Using --no-daemon is recommended for CI/CD environments as it avoids keeping a daemon process running.
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime stage
# Use a JRE image for a smaller final image size
FROM eclipse-temurin:21-jre-jammy

# Set the working directory for the application
WORKDIR /app

# The application requires the 'initialize' data directory to be present.
# We copy it from the build context into the image.
# Note: This assumes the Docker build context is the 'service' directory.
# COPY src/main/resources/initialize ./initialize
# Copy the initialize folder explicitly to /app/initialize/ so paths are stable and include the trailing slash
COPY src/main/resources/initialize /app/initialize/

# ---------------------------------------------------------------------------
# Environment variables (configure at runtime via docker-compose or -e flags)
# ---------------------------------------------------------------------------
# Database connection
# PPS_DB_JDBCURL    – JDBC URL of the database (default: jdbc:h2:mem:priceProviderService)
# PPS_DB_DRIVER     – JDBC driver class name   (default: org.h2.Driver)
# PPS_DB_USERNAME   – Database username         (default: sa)
# PPS_DB_PASSWORD   – Database password         (default: password)
#
# JPA / Hibernate
# PPS_JPA_DB_PLATFORM – Hibernate dialect class (default: org.hibernate.dialect.H2Dialect)
#
# Initialization
# PRICE_PROVIDER_SERVICE_INITIALIZE_DATA_FOLDER – Path to the data folder that contains
#   the initialization JSON files. Must end with a trailing slash.
#   (default: /app/initialize/, set by the ENV statement below)
# ---------------------------------------------------------------------------

# Set the environment variable to inform the application where to find the data files.
# Ensure the value ends with a trailing slash so code that concatenates subfolders produces
# paths like /app/initialize/essential/... (instead of /app/initializeessential/...)
# ENV PRICE_PROVIDER_SERVICE_INITIALIZE_DATA_FOLDER=/app/initialize
ENV PRICE_PROVIDER_SERVICE_INITIALIZE_DATA_FOLDER=/app/initialize/

# Copy the built JAR from the builder stage to the final image
# The JAR is located in the build/libs directory in the builder stage.
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# Expose the port on which the application will run
EXPOSE 8080

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]