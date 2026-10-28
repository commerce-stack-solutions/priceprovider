# Stage 1: Build stage
# Use a JDK image to build the application
FROM eclipse-temurin:25-jdk-noble AS builder

# Set the working directory in the container
WORKDIR /workspace/app

# Copy the cdf-plugin (build logic plugin)
COPY platform/cdf-plugin platform/cdf-plugin

# Copy the platform platform libraries
COPY platform/commons platform/commons
COPY platform/corebusinessentities platform/corebusinessentities
COPY platform/coreserviceapp platform/coreserviceapp

# Copy the priceprovider application files
COPY applications/priceprovider applications/priceprovider

# Grant execution rights and ensure LF line endings for the priceprovider gradlew
RUN sed -i 's/\r$//' ./applications/priceprovider/gradlew && chmod +x ./applications/priceprovider/gradlew

# Build the application from the priceprovider root directory
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
# It is located in the priceprovider application module.
COPY applications/priceprovider/src/main/resources/initialize /app/initialize/

# Set the environment variable to inform the application where to find the data files.
ENV SERVICE_CONFIG_INITIALIZE_DATA_FOLDER=/app/initialize/

# Copy the built JAR from the builder stage to the final image
COPY --from=builder /workspace/app/applications/priceprovider/build/libs/*.jar app.jar

# Expose the port on which the application will run
EXPOSE 8080

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
