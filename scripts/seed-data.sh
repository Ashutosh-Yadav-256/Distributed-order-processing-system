#!/bin/bash
set -e

GATEWAY_URL=${1:-"http://localhost:8080"}

echo "=================================================="
echo " Seeding Inventory via API Gateway: $GATEWAY_URL"
echo "=================================================="

echo "1. Fetching JWT authentication token..."
TOKEN_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@orderplatform.com", "roles": ["ROLE_ADMIN"]}')

TOKEN=$(echo "$TOKEN_RES" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Failed to get token from $GATEWAY_URL"
  exit 1
fi
echo "[PASS] Received JWT Token!"

echo -e "\n2. Seeding products..."
PRODUCTS=(
  '{"productId":"11111111-1111-1111-1111-111111111111","sku":"PROD-LAPTOP-001","name":"UltraBook Pro 16","availableQuantity":50,"reservedQuantity":0,"unitPrice":1299.99}'
  '{"productId":"22222222-2222-2222-2222-222222222222","sku":"PROD-HEADPHONES-002","name":"Noise-Cancelling Wireless Headphones","availableQuantity":100,"reservedQuantity":0,"unitPrice":199.99}'
  '{"productId":"33333333-3333-3333-3333-333333333333","sku":"PROD-KEYBOARD-003","name":"Mechanical RGB Gaming Keyboard","availableQuantity":5,"reservedQuantity":0,"unitPrice":149.99}'
  '{"productId":"44444444-4444-4444-4444-444444444444","sku":"PROD-MONITOR-004","name":"4K UltraWide Curved Monitor","availableQuantity":25,"reservedQuantity":0,"unitPrice":799.99}'
)

for p in "${PRODUCTS[@]}"; do
  curl -s -X POST "$GATEWAY_URL/api/v1/inventory" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$p" > /dev/null
  echo "[PASS] Seeded product: $p"
done

echo -e "\n[PASS] Finished seeding inventory!"
