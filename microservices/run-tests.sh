#!/bin/bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# Keep test volumes separate from the normal development Compose project.
compose=(docker compose --project-name "smart-energy-tests-$$" --file compose.yml)
cleanup() {
  status=$?
  trap - EXIT
  if [ "$status" -ne 0 ]; then
    "${compose[@]}" logs --no-color || true
  fi
  "${compose[@]}" down --volumes --remove-orphans || true
  exit "$status"
}

docker info > /dev/null
command -v curl > /dev/null
trap cleanup EXIT

for service in rates iot reactive; do
  (cd "$service" && ./gradlew build --no-daemon)
done

"${compose[@]}" up --build --wait --wait-timeout 180

# Compose waits for infrastructure health; wait for the HTTP APIs as well.
for url in http://localhost:8082/ http://localhost:8080/rooms http://localhost:8081/components; do
  ready=false
  for ((attempt = 0; attempt < 60; attempt++)); do
    if curl --fail --silent --output /dev/null --max-time 5 "$url"; then
      ready=true
      break
    fi
    sleep 2
  done
  if [ "$ready" != true ]; then
    echo "API did not become ready: $url" >&2
    exit 1
  fi
done

(cd end2end-tests && ./gradlew test --no-daemon)
