# Price Provider Service and Price Manager App

This repository contains the backend service and frontend application for the Price Provider project.

## Price Provider Service
location: `service/**`

A Java/Spring Boot backend that provides a RESTful API for managing and retrieving price information. For more details, see the [service README](service/README.md).

## Price Manager App
location: `app/**`

An Angular frontend application that consumes the price provider API to display and manage pricing data. For more details, see the [app README](app/README.md).

For development guidelines, architectural conventions, and project-specific information, please refer to the `AGENTS.md` file in the root directory and in each subproject directory.

## Run the Stack

This repo includes convenience scripts at the repository root to start and stop the stack with `docker-compose`:

- `docker-compose-up.bat` (Windows / cmd)
- `docker-compose-down.bat` (Windows / cmd)
- `docker-compose-up.sh` (Linux/macOS / bash)
- `docker-compose-down.sh` (Linux/macOS / bash)

General behaviour:
- The scripts accept an optional version as the first argument. The script treats the first argument as a version when it contains a dot (for example `1.2.3` or `0.0.1-SNAPSHOT`). If a version is provided it will be exported as the environment variable `VERSION` so that `docker-compose` can substitute it into image tags in `docker-compose.yml`.
- After the optional version you may pass zero or more service names. If no service names are provided the scripts operate on all services defined in the compose file.
- For the `down` scripts the optional version is accepted but ignored (down does not need a version).

**IMPORTANT:** These helper scripts DO NOT pull images from a registry. They require the referenced Docker images to exist locally. Before starting, the `up` scripts check for the presence of the required images and will abort with an error if any are missing. This avoids unexpected network pulls during automated runs.
Call `docker-compose up db -d` manually to pull the postgres version. Build the other images locally using the provided Dockerfile scripts in the `service/` and `app/` directories.

Examples

Windows (cmd.exe):

Start all services (default version):

```cmd
docker-compose-up.bat
```

Start all services with a specific version:

```cmd
docker-compose-up.bat 1.2.3
```

Start only specific services:

```cmd
docker-compose-up.bat db service
```

Start specific services with a version:

```cmd
docker-compose-up.bat 1.2.3 db service
```

Bring down all services:

```cmd
docker-compose-down.bat
```

Stop and remove only the `service` container(s):

```cmd
docker-compose-down.bat service
```

Linux / macOS (bash):

Start all services:

```bash
./docker-compose-up.sh
```

Start specific services with version:

```bash
./docker-compose-up.sh 1.2.3 db service
```

Bring down everything:

```bash
./docker-compose-down.sh
```

Notes and caveats

- The detection of a version is heuristic: the first argument is considered a version if it contains a dot `.`. Avoid using service names that contain a dot as the first argument.
- The scripts set a sensible default `VERSION` value of `0.0.0-SNAPSHOT` if none is provided. Ensure that your `docker-compose.yml` uses `${VERSION}` where appropriate for image tags.
- Make sure `docker-compose` is installed and available on your PATH.
- The `up` scripts will not pull or build images. They check local images with `docker image inspect` and abort if an image is missing. If you want automatic pulling/building behavior, let me know and I can add a safe flag (for example `--pull` or `--build`) to opt into that.

