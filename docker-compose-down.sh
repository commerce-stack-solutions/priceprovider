#!/bin/bash
# docker-compose-down.sh [version] [services...]
# If first parameter contains a dot (e.g. 1.2.3) it is treated as VERSION and ignored for down; remaining params are service names.

# Drop leading version arg if present
if [[ -n "$1" && "$1" == *.* ]]; then
  shift
fi

if [ $# -eq 0 ]; then
  echo "Stopping and removing all services..."
  docker-compose down
else
  # Use printf with "$*" to avoid 'mixes string and array' errors and to present services as a single string
  printf 'Stopping services: %s\n' "$*"
  docker-compose stop "$@"
  printf 'Removing stopped services: %s\n' "$*"
  docker-compose rm -f "$@"
fi

echo "Done."
