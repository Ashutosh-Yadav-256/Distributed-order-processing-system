# Local bootstrap script for Distributed Order Processing System

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Distributed Order Processing System - Local Bootstrapper" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Check if Docker daemon is running
try {
    docker info 2>&1 | Out-Null
    Write-Host "✓ Docker daemon is active." -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not running. Please start Docker Desktop and re-run." -ForegroundColor Red
    exit 1
}

# 2. Package Java artifacts if needed
$jarsExist = (Test-Path "services\api-gateway\target\*.jar") -and
             (Test-Path "services\order-service\target\*.jar") -and
             (Test-Path "services\inventory-service\target\*.jar") -and
             (Test-Path "services\payment-service\target\*.jar") -and
             (Test-Path "services\notification-service\target\*.jar")

if (-not $jarsExist) {
    Write-Host "`nCompiling and packaging all microservices..." -ForegroundColor Yellow
    .\mvnw.cmd clean package -DskipTests
}

# 3. Spin up Docker Compose
Write-Host "`nStarting Docker Compose containers..." -ForegroundColor Yellow
docker compose -f infrastructure\docker\docker-compose.yml up -d --build

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " All Services Started!" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host " API Gateway:          http://localhost:8080"
Write-Host " Order Service:        http://localhost:8081/swagger-ui.html"
Write-Host " Inventory Service:    http://localhost:8082/swagger-ui.html"
Write-Host " Payment Service:      http://localhost:8083/swagger-ui.html"
Write-Host " Notification Service: http://localhost:8084/swagger-ui.html"
Write-Host " RabbitMQ Management:  http://localhost:15672 (guest/guest)"
Write-Host " Prometheus:           http://localhost:9090"
Write-Host " Grafana:              http://localhost:3000 (admin/admin)"
Write-Host "========================================================`n" -ForegroundColor Cyan

Write-Host "Run .\scripts\seed-data.ps1 to populate catalog data." -ForegroundColor Yellow
Write-Host "Run .\scripts\test-saga.ps1 to execute distributed Saga tests." -ForegroundColor Yellow
