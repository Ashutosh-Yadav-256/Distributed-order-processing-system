#!/usr/bin/env bash
# Turnkey deployment script for AWS EC2 instance
# Builds and starts the distributed order processing system containers.

set -euo pipefail

echo "================================================================"
echo " Distributed Order Processing System - AWS EC2 Deployer"
echo "================================================================"

# Check Docker
if ! docker info > /dev/null 2>&1; then
    echo "[FAIL] Docker is not running or current user lacks permissions."
    echo "  Try: sudo systemctl start docker && sudo usermod -aG docker \$USER"
    exit 1
fi

echo "1. Building multi-module jars via Maven wrapper..."
./mvnw clean package -DskipTests -T 1C

echo "2. Starting core microservices and data backends via Docker Compose..."
docker compose -f infrastructure/docker/docker-compose.yml up -d --build

echo "3. Waiting 15 seconds for databases & message broker to initialize..."
sleep 15

echo "4. Checking container health..."
docker compose -f infrastructure/docker/docker-compose.yml ps

PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 || curl -s ifconfig.me || echo "localhost")

echo "================================================================"
echo " Deployment Successfully Finished!"
echo " Public Access Endpoints for Recruiters & Hiring Managers:"
echo "----------------------------------------------------------------"
echo "  Live API Gateway:       http://${PUBLIC_IP}:8080"
echo "  Order Service Swagger:  http://${PUBLIC_IP}:8081/swagger-ui.html"
echo "  Inventory Swagger:      http://${PUBLIC_IP}:8082/swagger-ui.html"
echo "  RabbitMQ Management:    http://${PUBLIC_IP}:15672 (guest/guest)"
echo "  Grafana Dashboard:      http://${PUBLIC_IP}:3000 (admin/admin)"
echo "================================================================"
