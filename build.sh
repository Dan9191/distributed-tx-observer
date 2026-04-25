#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Building observer-service JAR..."
cd "$SCRIPT_DIR"
./gradlew :observer-service:bootJar -x test

echo "==> Building demo-order-service JAR..."
cd "$SCRIPT_DIR"
./gradlew :demo-services:order-service:bootJar -x test

echo "==> Building demo-payment-service JAR..."
cd "$SCRIPT_DIR"
./gradlew :demo-services:payment-service:bootJar -x test

echo "==> Building demo-inventory-service JAR..."
cd "$SCRIPT_DIR"
./gradlew :demo-services:inventory-service:bootJar -x test

echo "==> Building demo-notification-service JAR..."
cd "$SCRIPT_DIR"
./gradlew :demo-services:notification-service:bootJar -x test

echo "==> Building observer-frontend..."
cd "$SCRIPT_DIR/observer-frontend"
npm ci
npm run build

echo "==> Starting environment..."
cd "$SCRIPT_DIR/test-stand"
docker compose up -d --build

echo ""
echo "Done. Services:"
echo "  Frontend:                   http://localhost:5173"
echo "  Observer Service:           http://localhost:8033"
echo "  Demo Order Service:         http://localhost:8082"
echo "  Demo Payment Service:       http://localhost:8083"
echo "  Demo Inventory Service:     http://localhost:8084"
echo "  Demo Notification Service:  http://localhost:8085"
echo "  Loki:                       http://localhost:3100"
