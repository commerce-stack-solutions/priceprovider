# Stage 1: Build stage
# Use a JDK image to build the application
FROM eclipse-temurin:25-jdk-noble AS builder

# Set the working directory in the container
WORKDIR /workspace/app

# Copy the Gradle wrapper and build files for both modules
# First, copy the platform/commons files
COPY platform/commons/gradlew platform/commons/
COPY platform/commons/gradle platform/commons/gradle
COPY platform/commons/build.gradle platform/commons/
COPY platform/commons/settings.gradle platform/commons/
COPY platform/commons/src platform/commons/src

# Next, copy the applications/priceprovider files
COPY applications/priceprovider/gradlew applications/priceprovider/
COPY applications/priceprovider/gradle applications/priceprovider/gradle
COPY applications/priceprovider/build.gradle applications/priceprovider/
COPY applications/priceprovider/settings.gradle applications/priceprovider/
COPY applications/priceprovider/src applications/priceprovider/src

# Grant execution rights and ensure LF line endings
RUN sed -i 's/\r$//' ./platform/commons/gradlew && chmod +x ./platform/commons/gradlew
RUN sed -i 's/\r$//' ./applications/priceprovider/gradlew && chmod +x ./applications/priceprovider/gradlew

# Build the application
# We build the priceprovider app, which will automatically build commons via includeBuild
WORKDIR /workspace/app/applications/priceprovider
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime stage
# Use a JRE image for a smaller final image size
FROM eclipse-temurin:25-jre-noble

# Install curl for the Docker health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Set the working directory for the application
WORKDIR /app

# The application requires the 'initialize' data directory to be present.
# It is now located in the priceprovider application module.
COPY applications/priceprovider/src/main/resources/initialize /app/initialize/

# Set the environment variable to inform the application where to find the data files.
ENV SERVICE_CONFIG_INITIALIZE_DATA_FOLDER=/app/initialize/

# Copy the built JAR from the builder stage to the final image
COPY --from=builder /workspace/app/applications/priceprovider/build/libs/*.jar app.jar

# Expose the port on which the application will run
EXPOSE 8080

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
