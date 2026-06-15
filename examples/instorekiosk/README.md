# In-Store Kiosk Demo (Flutter)

A minimal Flutter-based in-store kiosk example that consumes the Price Provider public API.

## Features

- Product selection (SKU selector)
- Quantity input with dynamic price update
- Clean separation of concerns (Models, Repositories, Services, Widgets)
- Keycloak login for organization-scoped pricing (Web only)
- Simple and clean Material UI

## Prerequisites

- [Flutter SDK](https://docs.flutter.dev/get-started/install)
- Running Price Provider service (localhost:8080)
- Running Keycloak (localhost:8081)

## Running the Web Demo

You can run this demo as a web application:

```bash
flutter run -d chrome --web-port 3002
```

Alternatively, use the provided Node.js server to serve a production build:

```bash
# Build the web version
flutter build web

# Install dependencies and start server
npm install
npm start
```

Then open [http://localhost:3002](http://localhost:3002) in your browser.

## Project Structure

- `lib/models/`: Data models for Product and Price.
- `lib/repositories/`: Data access layer for calling the Price Provider API.
- `lib/services/`: Business logic and state management using `ChangeNotifier` and `provider`.
- `lib/pages/`: UI pages and main layout.
- `lib/widgets/`: Reusable UI components.
