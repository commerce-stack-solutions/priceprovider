# Stage 1: Build stage
FROM eclipse-temurin:25-jdk-noble AS builder

# Set the working directory
WORKDIR /workspace

# Copy Gradle wrappers and build files for both modules to leverage Docker cache
COPY applications/priceprovider/gradlew ./applications/priceprovider/gradlew
COPY applications/priceprovider/gradle ./applications/priceprovider/gradle
COPY applications/priceprovider/build.gradle ./applications/priceprovider/build.gradle
COPY applications/priceprovider/settings.gradle ./applications/priceprovider/settings.gradle

COPY platform/commons/build.gradle ./platform/commons/build.gradle
COPY platform/commons/settings.gradle ./platform/commons/settings.gradle

# Copy the source code for both modules
COPY platform/commons/src ./platform/commons/src
COPY applications/priceprovider/src ./applications/priceprovider/src

# Grant execution rights to the Gradle wrapper
WORKDIR /workspace/applications/priceprovider
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Build the application
# Gradle will automatically build the :commons dependency because it's included in settings.gradle
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-noble

# Install curl for the Docker health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Set the working directory for the application
WORKDIR /app

# Copy the 'initialize' data directory from the priceprovider module
COPY applications/priceprovider/src/main/resources/initialize /app/initialize/

# Set environment variable for data folder
ENV SERVICE_CONFIG_INITIALIZE_DATA_FOLDER=/app/initialize/

# Copy the built JAR from the builder stage
COPY --from=builder /workspace/applications/priceprovider/build/libs/*.jar app.jar

# Expose the port
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
