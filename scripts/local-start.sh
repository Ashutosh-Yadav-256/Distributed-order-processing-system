#!/bin/bash
set -e

echo "========================================================"
echo " Distributed Order Processing System - Local Bootstrapper"
echo "========================================================"

if ! docker info > /dev/null 2>&1; then
    echo "✗ Docker is not running. Please start Docker and re-run."
    exit 1
fi

echo "Compiling and packaging all microservices..."
./mvnw clean package -DskipTests

echo "Starting Docker Compose containers..."
docker compose -f infrastructure/docker/docker-compose.yml up -d --build

echo "========================================================"
echo " All Services Started!"
echo "--------------------------------------------------------"
echo " API Gateway:          http://localhost:8080"
echo " Order Service:        http://localhost:8081/swagger-ui.html"
echo " Inventory Service:    http://localhost:8082/swagger-ui.html"
echo " Payment Service:      http://localhost:8083/swagger-ui.html"
echo " Notification Service: http://localhost:8084/swagger-ui.html"
echo " RabbitMQ Management:  http://localhost:15672 (guest/guest)"
echo " Prometheus:           http://localhost:9090"
echo " Grafana:              http://localhost:3000 (admin/admin)"
echo "========================================================"
