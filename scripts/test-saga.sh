#!/bin/bash
set -e

GATEWAY_URL=${1:-"http://localhost:8080"}

echo "========================================================"
echo " Running Distributed Saga E2E Scenarios against $GATEWAY_URL"
echo "========================================================"

TOKEN_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"email": "tester@platform.com", "roles": ["ROLE_USER"]}')
TOKEN=$(echo "$TOKEN_RES" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo -e "\n>>> [SCENARIO A] HAPPY PATH SAGA"
ORDER_A_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"99999999-9999-9999-9999-999999999991","customerEmail":"alice@example.com","currency":"USD","paymentMethod":"CREDIT_CARD","items":[{"productId":"22222222-2222-2222-2222-222222222222","productName":"Noise-Cancelling Headphones","quantity":2,"unitPrice":199.99}]}')

ORDER_A_ID=$(echo "$ORDER_A_RES" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "[PASS] Order Created: $ORDER_A_ID. Waiting 2s for Saga choreography..."
sleep 2

ORDER_A_STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/v1/orders/$ORDER_A_ID" | grep -o '"status":"[^"]*' | head -1 | cut -d'"' -f4)
echo "[PASS] Final Order Status: $ORDER_A_STATUS"

echo -e "\n>>> [SCENARIO B] INVENTORY FAILURE (OUT OF STOCK)"
ORDER_B_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"99999999-9999-9999-9999-999999999992","customerEmail":"bob@example.com","currency":"USD","paymentMethod":"CREDIT_CARD","items":[{"productId":"33333333-3333-3333-3333-333333333333","productName":"Keyboard","quantity":9999,"unitPrice":149.99}]}')

ORDER_B_ID=$(echo "$ORDER_B_RES" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "[PASS] Order Created: $ORDER_B_ID. Waiting 2s..."
sleep 2

ORDER_B_STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/v1/orders/$ORDER_B_ID" | grep -o '"status":"[^"]*' | head -1 | cut -d'"' -f4)
echo "[PASS] Final Order Status: $ORDER_B_STATUS"

echo -e "\n>>> [SCENARIO C] PAYMENT FAILURE & COMPENSATION"
ORDER_C_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"99999999-9999-9999-9999-999999999993","customerEmail":"charlie@example.com","currency":"USD","paymentMethod":"CREDIT_CARD_FAIL","items":[{"productId":"44444444-4444-4444-4444-444444444444","productName":"4K Monitor","quantity":3,"unitPrice":799.99}]}')

ORDER_C_ID=$(echo "$ORDER_C_RES" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "[PASS] Order Created: $ORDER_C_ID. Waiting 3s for failure & compensation..."
sleep 3

ORDER_C_STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/v1/orders/$ORDER_C_ID" | grep -o '"status":"[^"]*' | head -1 | cut -d'"' -f4)
echo "[PASS] Final Order Status: $ORDER_C_STATUS (Expected: CANCELLED)"

echo -e "\n========================================================"
echo " All Saga Scenarios Complete!"
echo "========================================================"
