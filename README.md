# Price Provider Service and Price Manager App

This repository contains the backend service and frontend application for the Price Provider project.

## Price Provider Service
location: `services/platform/commons/**` and `services/applications/priceprovider/**`

A Java/Spring Boot backend that provides a RESTful API for managing and retrieving price information. The backend is split into a shared commons module and the priceprovider application module. For more details, see the [priceprovider README](services/applications/priceprovider/README.md).

## Price Manager App
location: `app/**`

An Angular frontend application that consumes the price provider API to display and manage pricing data. For more details, see the [app README](app/README.md).

## Module and Application Overview

This repository is organized into a clean, decoupled architecture separating the frontend client application from backend service and reusable platform modules:

### 1. Frontend Client Application
- **`app/`** (Price Manager App): A standalone, modern web application built with Angular 22, TypeScript 6, and Bootstrap/Tailwind CSS. Consumes the backend API to provide a comprehensive management interface for pricing operations. Contains an end-to-end test suite implemented with Playwright following the Page Object Model (POM) pattern.

### 2. Backend Modules & Service Applications (`services/`)
Structured as a multi-module platform-to-application design to promote code reusability, modularity, and future support for multiple separated services:

- **Platform Layer (`platform/`)**: Reusable components, plugins, and libraries that form the foundational stack for all application services:
  - **`platform/cdf-plugin/`**: A custom Gradle plugin providing Common Definition Format (CDF) code generation. Generates JPA entities and schemas from build-time definition files.
  - **`platform/commons/`**: Shared core library containing foundational utility classes, exception definitions, abstract mapper base classes, and request/response interceptors.
  - **`platform/corebusinessentities/`**: Encapsulates core business master data (e.g., `Unit`, `Currency`, `TaxClass`, `Group`, `Organization`, and `Language`) including their persistence mappings, business validations, mapper classes, and REST endpoint controllers.
  - **`platform/coreserviceapp/`**: Standard security module implementing application-level authentication, authorization (roles & permissions config), and endpoint security.

- **Application Layer (`applications/`)**: Deployable service applications that bundle reusable platform capabilities with specific business logic:
  - **`applications/priceprovider/`**: A fully functional, standalone Spring Boot backend service. It implements specific pricing domain models (like `PriceRowEntity`), handles bulk create/update operations, secures endpoints, and exposes administrative and public pricing APIs. Dependending on `platform/` modules, it compiles and runs as an independent microservice.

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
- [In-Store Kiosk (Flutter, experimental)](examples/instorekiosk/README.md) – A cross-platform kiosk application built with Flutter. (Linux, Windows, Mac?)
