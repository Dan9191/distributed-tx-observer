#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Building observer-service JAR..."
cd "$SCRIPT_DIR"
./gradlew :observer-service:bootJar -x test

echo "==> Building demo-service JAR..."
cd "$SCRIPT_DIR/test-stand/demo-service"
if [ -f "./gradlew" ]; then
    ./gradlew bootJar -x test
else
    gradle bootJar -x test
fi

echo "==> Building observer-frontend..."
cd "$SCRIPT_DIR/observer-frontend"
npm ci
npm run build

echo "==> Starting environment..."
cd "$SCRIPT_DIR/test-stand"
docker compose up -d --build

echo ""
echo "Done. Services:"
echo "  Frontend:         http://localhost:5173"
echo "  Observer Service: http://localhost:8033"
echo "  Demo Service:     http://localhost:8081"
echo "  Loki:             http://localhost:3100"
