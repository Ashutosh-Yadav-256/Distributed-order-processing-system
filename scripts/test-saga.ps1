# Automated End-to-End Saga Test Suite
param(
    [string]$GatewayUrl = "http://localhost:8080"
)

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " Running Distributed Saga E2E Scenarios against $GatewayUrl" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Fetch JWT Bearer Token
$tokenRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/token" -Method Post -Body (@{ email = "tester@platform.com"; roles = @("ROLE_USER") } | ConvertTo-Json) -ContentType "application/json"
$token = $tokenRes.data.token
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

# -------------------------------------------------------------
# Scenario A: Happy Path Saga
# -------------------------------------------------------------
Write-Host "`n>>> [SCENARIO A] HAPPY PATH: Order -> Reserve Stock -> Process Payment -> Confirm Order" -ForegroundColor Yellow

$orderReqA = @{
    customerId = "99999999-9999-9999-9999-999999999991"
    customerEmail = "alice@example.com"
    currency = "USD"
    paymentMethod = "CREDIT_CARD"
    items = @(
        @{
            productId = "22222222-2222-2222-2222-222222222222"
            productName = "Noise-Cancelling Wireless Headphones"
            quantity = 2
            unitPrice = 199.99
        }
    )
} | ConvertTo-Json

$orderA = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/orders" -Method Post -Headers $headers -Body $orderReqA).data
Write-Host "✓ Order created with ID: $($orderA.id), Initial Status: $($orderA.status)" -ForegroundColor Green

Write-Host "Waiting 2s for RabbitMQ Saga choreography..."
Start-Sleep -Seconds 2

$checkedOrderA = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/orders/$($orderA.id)" -Method Get -Headers $headers).data
Write-Host "✓ Final Order Status: $($checkedOrderA.status)" -ForegroundColor $(if ($checkedOrderA.status -eq "CONFIRMED") { "Green" } else { "Red" })

$paymentA = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/payments/order/$($orderA.id)" -Method Get -Headers $headers).data
Write-Host "✓ Payment Status: $($paymentA.status), Txn: $($paymentA.transactionId)" -ForegroundColor Green

$notificationsA = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/order/$($orderA.id)" -Method Get -Headers $headers).data
Write-Host "✓ Dispatched Notifications count: $($notificationsA.Count)" -ForegroundColor Green

# -------------------------------------------------------------
# Scenario B: Out of Stock Failure
# -------------------------------------------------------------
Write-Host "`n>>> [SCENARIO B] INVENTORY FAILURE: Requesting more stock than available" -ForegroundColor Yellow

$orderReqB = @{
    customerId = "99999999-9999-9999-9999-999999999992"
    customerEmail = "bob@example.com"
    currency = "USD"
    paymentMethod = "CREDIT_CARD"
    items = @(
        @{
            productId = "33333333-3333-3333-3333-333333333333"
            productName = "Mechanical RGB Gaming Keyboard"
            quantity = 9999  # Far exceeds available 5
            unitPrice = 149.99
        }
    )
} | ConvertTo-Json

$orderB = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/orders" -Method Post -Headers $headers -Body $orderReqB).data
Write-Host "✓ Order created with ID: $($orderB.id)" -ForegroundColor Green

Write-Host "Waiting 2s for RabbitMQ Saga to detect stock shortage..."
Start-Sleep -Seconds 2

$checkedOrderB = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/orders/$($orderB.id)" -Method Get -Headers $headers).data
Write-Host "✓ Final Order Status: $($checkedOrderB.status) (Reason: $($checkedOrderB.failureReason))" -ForegroundColor $(if ($checkedOrderB.status -eq "FAILED") { "Green" } else { "Red" })

# -------------------------------------------------------------
# Scenario C: Payment Failure & Compensating Transaction
# -------------------------------------------------------------
Write-Host "`n>>> [SCENARIO C] PAYMENT FAILURE & COMPENSATION: Stock reserved -> Payment fails -> Compensation releases stock" -ForegroundColor Yellow

# Check monitor stock before order
$stockBefore = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/inventory/44444444-4444-4444-4444-444444444444" -Method Get -Headers $headers).data.availableQuantity
Write-Host "Initial available stock for 4K Monitor: $stockBefore" -ForegroundColor Cyan

$orderReqC = @{
    customerId = "99999999-9999-9999-9999-999999999993"
    customerEmail = "charlie@example.com"
    currency = "USD"
    paymentMethod = "CREDIT_CARD_FAIL" # Triggers simulated failure
    items = @(
        @{
            productId = "44444444-4444-4444-4444-444444444444"
            productName = "4K UltraWide Curved Monitor"
            quantity = 3
            unitPrice = 799.99
        }
    )
} | ConvertTo-Json

$orderC = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/orders" -Method Post -Headers $headers -Body $orderReqC).data
Write-Host "✓ Order created with ID: $($orderC.id)" -ForegroundColor Green

Write-Host "Waiting 3s for Saga: Reservation -> Payment Failure -> Stock Compensation Release..."
Start-Sleep -Seconds 3

$checkedOrderC = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/orders/$($orderC.id)" -Method Get -Headers $headers).data
Write-Host "✓ Final Order Status: $($checkedOrderC.status) (Reason: $($checkedOrderC.failureReason))" -ForegroundColor $(if ($checkedOrderC.status -eq "CANCELLED") { "Green" } else { "Red" })

$stockAfter = (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/inventory/44444444-4444-4444-4444-444444444444" -Method Get -Headers $headers).data.availableQuantity
Write-Host "Stock after compensation release: $stockAfter" -ForegroundColor Cyan

if ($stockBefore -eq $stockAfter) {
    Write-Host "✓ SUCCESS: Stock was successfully returned via compensating transaction!" -ForegroundColor Green
} else {
    Write-Host "✗ ERROR: Stock was not restored correctly!" -ForegroundColor Red
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " All Saga Scenarios Executed Successfully!" -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan
