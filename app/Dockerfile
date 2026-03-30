# Stage 1: Build stage
# Use a Node.js image to build the Angular application
FROM node:20-alpine AS builder

# Set the working directory in the container
WORKDIR /workspace/app

# Copy package files to download dependencies
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy the source code
COPY . .

# Build the application for production
RUN npm run build

# Stage 2: Runtime stage
# Use Apache HTTP Server to serve the static files
FROM httpd:2.4-alpine

# Install required tools
RUN apk add --no-cache bash sed dos2unix

# Copy the built application from the builder stage
COPY --from=builder /workspace/app/dist/pricemanager-app/browser /usr/local/apache2/htdocs/

# Copy custom Apache configuration
COPY docker/httpd.conf /usr/local/apache2/conf/httpd.conf

# Copy .htaccess file for URL rewriting
COPY docker/.htaccess /usr/local/apache2/htdocs/.htaccess

# Copy the entrypoint script and fix line endings
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN dos2unix /usr/local/bin/entrypoint.sh && \
    chmod +x /usr/local/bin/entrypoint.sh

# ---------------------------------------------------------------------------
# Environment variables (configure at runtime via docker-compose or -e flags)
# ---------------------------------------------------------------------------
# PPS_BASEURL – Base URL of the Price Provider Service backend that the Angular
#               application calls for API requests.
#               (default: http://localhost:8080/, set by the ENV statement below)
# ---------------------------------------------------------------------------

# Set environment variable for the API base URL (default value)
ENV PPS_BASEURL=http://localhost:8080/

# Expose port 80
EXPOSE 80

# Use the entrypoint script to replace the API base URL before starting Apache
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
