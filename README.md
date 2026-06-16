# Price Provider Service and Price Manager App

This repository contains the backend service and frontend application for the Price Provider project.

## Price Provider Service
location: `service/**`

A Java/Spring Boot backend that provides a RESTful API for managing and retrieving price information. For more details, see the [service README](service/README.md).

## Price Manager App
location: `app/**`

An Angular frontend application that consumes the price provider API to display and manage pricing data. For more details, see the [app README](app/README.md).

For development guidelines, architectural conventions, and project-specific information, please refer to the `AGENTS.md` file in the root directory and in each subproject directory.

## Quick Start (Build & Run Dev Environment)

The fastest way to get the entire stack running from a fresh clone is to use the all-in-one script at the repository root.

**Prerequisites:** Docker Desktop must be installed and running.

Linux / macOS:

```bash
./build-and-run-devenv.sh
```

Windows (cmd.exe):

```cmd
build-and-run-devenv.bat
```

The script will:
1. Verify that Docker Desktop is running.
2. Check whether Node.js / npm is available (optional – used for example frontends).
3. Build a local dev Docker image for the **Price Provider Service** (`price-provider-service:0.0.0-SNAPSHOT`).
4. Build a local dev Docker image for the **Price Manager App** (`price-manager-app:0.0.0-SNAPSHOT`).
5. Start the full stack with `docker compose` (PostgreSQL, Keycloak, service, app).
6. If Node.js / npm is available, install dependencies and start the example frontends in the background.

After the script completes the following endpoints are available:

| Service                 | URL                      |
|-------------------------|--------------------------|
| Price Manager App       | http://localhost         |
| Price Provider API      | http://localhost:8080    |
| Keycloak (IdP)          | http://localhost:8081    |
| Shop Frontend (demo)    | http://localhost:3000 *(requires Node.js)* |
| Rental Frontend (demo)  | http://localhost:3001 *(requires Node.js)* |
| In-Store Kiosk (demo)   | http://localhost:3002 *(requires Flutter / Node.js)* |

To stop the stack run `docker compose down`.

## Examples

- [Shop Frontend (HTML/JS)](examples/shopfrontend/README.md) – A minimal shop demo using standard web technologies.
- [Rental Frontend (HTML/JS)](examples/rentalfrontend/README.md) – A demo for rental-specific pricing.
- [In-Store Kiosk (Flutter)](examples/instorekiosk/README.md) – A cross-platform kiosk application built with Flutter.
