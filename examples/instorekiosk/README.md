# In-Store Kiosk Demo (Flutter, experimental)

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

### Linux Dependencies
For Linux builds, the `libsecret` library is required for secure storage:
```bash
sudo apt-get update
sudo apt-get install libsecret-1-dev
```

## Run The App

You can run this demo as a Linux (tested), Windows (tested) or Mac (maybe, not tested) application:

```bash
flutter run
```

## Project Structure

- `lib/models/`: Data models for Product and Price.
- `lib/repositories/`: Data access layer for calling the Price Provider API.
- `lib/services/`: Business logic and state management using `ChangeNotifier` and `provider`.
- `lib/pages/`: UI pages and main layout.
- `lib/widgets/`: Reusable UI components.
