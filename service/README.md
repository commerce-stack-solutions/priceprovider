# Price Provider Service

This project is a Java/Spring Boot backend that provides a RESTful API for managing and retrieving price information. It serves as the backend for the Price Provider application.

## Development and Architecture

For detailed information on the project's architecture, development guidelines, and conventions, please see the [AGENTS.md](AGENTS.md) file.

## Developer Setup

### Prerequisites

*   **Open JDK 21**
*   **IntelliJ IDEA** (or another IDE with Gradle support)

### Building and Running (local developer)

1.  **Clean and Build the project:**
    ```bash
    ./gradlew clean build
    ```

2.  **Run the service locally:**
    ```bash
    ./gradlew bootRun
    ```

### Environment Configuration

For local development, you can configure environment variables using a `.env` file:

1.  **Copy the example file:**
    ```bash
    cp .env.example .env
    ```

2.  **Edit `.env` to customize your local settings** (e.g., database configuration)

3.  **Using with IntelliJ IDEA:**
    - Install the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile)
    - In your Run Configuration, enable the EnvFile plugin and select the `.env` file
    - Now you can run or debug the application with the environment variables loaded

4.  **Using with Gradle bootRun:**
    ```bash
    # Source the .env file before running
    set -a && source .env && set +a && ./gradlew bootRun
    ```

**Note:** The `.env` file is ignored by git to prevent committing sensitive credentials. Use `.env.example` as a template.

### Getting Started with Development (IntelliJ)

The `.run/` directory at the project root contains shared IntelliJ IDEA run configurations that are picked up automatically.

#### Available Run Configurations

**PriceProviderService (dev)**

Runs the service with the `dev` Spring profile using the default H2 in-memory database. No additional setup required.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`
- SQL and DEBUG logging enabled
- Sample data initialization enabled

**PriceProviderService (dev, postgres)**

Runs the service with the `dev` Spring profile connected to the PostgreSQL database started via Docker Compose.

**Prerequisites**: start the database first:
```bash
docker-compose up -d db
```

The following environment variables are injected automatically (these are the default development credentials matching the Docker Compose defaults):

| Variable | Value |
|---|---|
| `PPS_DB_JDBCURL` | `jdbc:postgresql://localhost:5432/priceProviderService` |
| `PPS_DB_USERNAME` | `postgres` |
| `PPS_DB_PASSWORD` | `postgres` |
| `PPS_DB_DRIVER` | `org.postgresql.Driver` |
| `PPS_JPA_DB_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` |

> **Note:** The credentials above match the Docker Compose defaults for local development only. Do not use them in any non-local environment.

Features are the same as the `dev` configuration except the H2 console is not available.

### Testing

To run the unit tests for the service, execute the following command:

```bash
./gradlew test
```

## API Testing with Postman

The repository includes a comprehensive Postman collection for testing all API endpoints.

### Postman Collection Location

The Postman collection is located at:
```
service/postman/pps-postmancollection.json
```

### Importing the Collection

1. **Open Postman** (download from [postman.com](https://www.postman.com/) if needed)
2. Click **Import** in the top left
3. Select **File** and choose `service/postman/pps-postmancollection.json`
4. The collection will be imported with all endpoints pre-configured

### Available Endpoints in Collection

The collection includes:

**Health Check:**
- Health endpoint for service monitoring

**Units:**
- Get all units (with pagination and sorting)
- Get unit by symbol
- Create new unit (POST /api/units/create)
- Create or update unit (PUT /api/units/{symbol})
- Patch unit (PATCH with RFC 6902 JSON Patch)
- Examples for localized name management

**Price Rows:**
- Get all price rows (with pagination and sorting)
- Get price row by ID
- Create new price row (POST /api/pricerows/create)
- Update price row (PUT /api/pricerows/{id})
- Patch price row (PATCH with RFC 6902 JSON Patch)
- Examples for updating prices, units, and dates

### Configuration

The collection uses a `{{baseUrl}}` variable set to `http://localhost:8080` by default. You can modify this in Postman:

1. Click on the collection name
2. Go to **Variables** tab
3. Update the `baseUrl` value as needed

### Important Notes

- **PUT vs PATCH**: 
  - `PUT` performs a full replacement of the resource (recreate operation)
  - `PATCH` performs a partial update using RFC 6902 JSON Patch standard
  - Use PATCH for incremental updates to avoid unintended data loss
- All PATCH requests use `Content-Type: application/json-patch+json`
- The collection includes examples for both simple and complex operations

### Building Individual Docker Images

To build the backend service and frontend application Docker images separately:

1. **Build the Price Provider Service (Backend):**

   **Linux/macOS:**
   ```bash
   cd service
   ./dockerimage-create.sh [version]
   ```

   **Windows:**
   ```bat
   cd service
   dockerimage-create.bat [version]
   ```

2. **Build the Price Manager App (Frontend):**

   **Linux/macOS:**
   ```bash
   cd app
   ./dockerimage-create.sh [version]
   ```

   **Windows:**
   ```bat
   cd app
   dockerimage-create.bat [version]
   ```

If no version is specified, `0.0.0-SNAPSHOT` will be used as the default.

## Run with Docker Compose (Recommended)

The easiest way to run the entire application stack (database, backend, frontend) is using Docker Compose:

**Linux/macOS:**
```bash
# From the project root directory
./docker-compose-up.sh [version]
```

**Windows:**
```bat
# From the project root directory
docker-compose-up.bat [version]
```

If no version is specified, `0.0.0-SNAPSHOT` will be used as the default.

This will start:
- PostgreSQL database on port 5432
- Backend API on port 8080
- Frontend app on port 80

**Environment Variables:**
You can customize the deployment by setting these environment variables before running docker-compose:

- `PPS_DB_USERNAME` - Database username (default: `postgres`)
- `PPS_DB_PASSWORD` - Database password (default: `postgres`)
- `PPS_DB_JDBCURL` - Database JDBC URL (default: `jdbc:postgresql://db:5432/priceProviderService`)
- `PPS_BASEURL` - Backend API URL for the frontend (default: `http://localhost:8080/`)

**Example:**
```bash
export PPS_DB_PASSWORD=mySecurePassword
export PPS_BASEURL=http://localhost:8080/
./docker-compose-up.sh 1.0.0
```

**To stop all services:**
```bash
./docker-compose-down.sh
```

## Database Configuration

### H2 Database (Default for Local Development)

By default, the service uses H2 in-memory database when run locally via `./gradlew bootRun`. No additional configuration is needed.

The H2 console is available at `http://localhost:8080/h2-console` with these credentials:
- JDBC URL: `jdbc:h2:mem:priceProviderService`
- Username: `sa`
- Password: `password`

### PostgreSQL Database (Recommended for Production)

For production deployments, PostgreSQL is recommended. The service automatically detects PostgreSQL when the appropriate environment variables are set:

**Environment Variables:**
- `PPS_DB_JDBCURL` - JDBC URL (e.g., `jdbc:postgresql://localhost:5432/priceProviderService`)
- `PPS_DB_USERNAME` - Database username
- `PPS_DB_PASSWORD` - Database password
- `PPS_DB_DRIVER` - Database driver class (e.g., `org.postgresql.Driver`)
- `PPS_JPA_DB_PLATFORM` - Hibernate dialect (e.g., `org.hibernate.dialect.PostgreSQLDialect`)

The service will automatically:
- Use the specified database driver
- Set the appropriate Hibernate dialect
- Create/update the database schema on startup

**Note:** When using Docker Compose, these are automatically configured via the `PPS_DB_*` environment variables.

### Sample Data

The service can be configured to import sample data on startup. This is controlled by the `service-config.initialize.sample-data-on` property in the `src/main/resources/application.yaml` file.

To enable or disable the sample data import, set the property to `true` or `false` respectively:

```yaml
service-config:
  initialize:
    sample-data-on: true # or false
```
