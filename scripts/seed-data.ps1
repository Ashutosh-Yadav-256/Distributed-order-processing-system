# PowerShell script to seed initial inventory data through the API Gateway
param(
    [string]$GatewayUrl = "http://localhost:8080"
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Seeding Inventory via API Gateway: $GatewayUrl" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Obtain test JWT token from Gateway
Write-Host "1. Fetching JWT authentication token..." -ForegroundColor Yellow
$tokenBody = @{
    email = "admin@orderplatform.com"
    roles = @("ROLE_ADMIN", "ROLE_USER")
} | ConvertTo-Json

try {
    $tokenResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/token" -Method Post -Body $tokenBody -ContentType "application/json"
    $token = $tokenResponse.data.token
    Write-Host "[PASS] Received JWT Bearer Token!" -ForegroundColor Green
} catch {
    Write-Host "Failed to obtain token from Gateway. Is Gateway running at $GatewayUrl?" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

# 2. Seed catalog products
$products = @(
    @{
        productId = "11111111-1111-1111-1111-111111111111"
        sku = "PROD-LAPTOP-001"
        name = "UltraBook Pro 16"
        availableQuantity = 50
        reservedQuantity = 0
        unitPrice = 1299.99
    },
    @{
        productId = "22222222-2222-2222-2222-222222222222"
        sku = "PROD-HEADPHONES-002"
        name = "Noise-Cancelling Wireless Headphones"
        availableQuantity = 100
        reservedQuantity = 0
        unitPrice = 199.99
    },
    @{
        productId = "33333333-3333-3333-3333-333333333333"
        sku = "PROD-KEYBOARD-003"
        name = "Mechanical RGB Gaming Keyboard"
        availableQuantity = 5
        reservedQuantity = 0
        unitPrice = 149.99
    },
    @{
        productId = "44444444-4444-4444-4444-444444444444"
        sku = "PROD-MONITOR-004"
        name = "4K UltraWide Curved Monitor"
        availableQuantity = 25
        reservedQuantity = 0
        unitPrice = 799.99
    }
)

Write-Host "`n2. Seeding inventory items..." -ForegroundColor Yellow
foreach ($prod in $products) {
    $body = $prod | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/inventory" -Method Post -Headers $headers -Body $body
        Write-Host "[PASS] Added product: $($prod.name) ($($prod.sku)) - Qty: $($prod.availableQuantity)" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] Failed to add $($prod.name): $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n[PASS] Inventory seeding completed successfully!" -ForegroundColor Cyan
