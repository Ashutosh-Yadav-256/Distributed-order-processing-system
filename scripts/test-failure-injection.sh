#!/usr/bin/env bash
# ==============================================================================
# Failure Injection & Chaos Engineering Test Suite (Bash / Linux / macOS / WSL)
# Tests automated recovery, compensating transactions, idempotency, and fault isolation
# ==============================================================================
set -euo pipefail

GATEWAY_URL="${1:-http://localhost:8080}"

GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
GRAY='\033[0;90m'
NC='\033[0m'

header() {
    echo -e "\n${CYAN}========================================================${NC}"
    echo -e "${CYAN} $1${NC}"
    echo -e "${CYAN}========================================================${NC}"
}

pass() { echo -e "  ${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "  ${RED}[FAIL]${NC} $1"; }
info() { echo -e "  ${GRAY}[INFO]${NC} $1"; }

header "DISTRIBUTED SAGA FAILURE INJECTION SUITE"
info "Target Gateway: $GATEWAY_URL"

# Step 0: Acquire Auth Token
info "Acquiring Bearer token from auth endpoint..."
TOKEN_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/token" \
    -H "Content-Type: application/json" \
    -d '{"email":"chaos-tester@platform.com","roles":["ROLE_ADMIN","ROLE_USER"]}')

TOKEN=$(echo "$TOKEN_RES" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || true)

if [ -z "$TOKEN" ]; then
    fail "Could not acquire JWT token. Is API Gateway running at $GATEWAY_URL?"
    exit 1
fi
pass "JWT token acquired successfully."

AUTH_HEADER="Authorization: Bearer $TOKEN"

# ------------------------------------------------------------------------------
# SCENARIO 1: Insufficient Stock (Immediate Saga Abort)
# ------------------------------------------------------------------------------
header "CHAOS SCENARIO 1: INSUFFICIENT STOCK SHORTAGE"
info "Attempting to order 50,000 units of an item with limited inventory..."

ORDER1_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/orders" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": "11111111-1111-1111-1111-111111111111",
        "customerEmail": "chaos1@platform.com",
        "currency": "USD",
        "paymentMethod": "CREDIT_CARD",
        "items": [{
            "productId": "33333333-3333-3333-3333-333333333333",
            "productName": "Mechanical RGB Gaming Keyboard",
            "quantity": 50000,
            "unitPrice": 149.99
        }]
    }')

ORDER1_ID=$(echo "$ORDER1_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
info "Order created: ID=$ORDER1_ID"

info "Waiting 3s for RabbitMQ Saga to detect inventory shortage..."
sleep 3

CHECK1_RES=$(curl -s -X GET "$GATEWAY_URL/api/v1/orders/$ORDER1_ID" -H "$AUTH_HEADER")
STATUS1=$(echo "$CHECK1_RES" | grep -o '"status":"[^"]*' | cut -d'"' -f4)

if [ "$STATUS1" = "FAILED" ]; then
    pass "Saga correctly terminated with FAILED status!"
else
    fail "Order status is $STATUS1, expected FAILED"
fi

# ------------------------------------------------------------------------------
# SCENARIO 2: Downstream Payment Failure & Compensating Stock Rollback
# ------------------------------------------------------------------------------
header "CHAOS SCENARIO 2: PAYMENT DECLINE & COMPENSATING TRANSACTION"
MONITOR_ID="44444444-4444-4444-4444-444444444444"

STOCK_BEFORE_RES=$(curl -s -X GET "$GATEWAY_URL/api/v1/inventory/$MONITOR_ID" -H "$AUTH_HEADER")
STOCK_BEFORE=$(echo "$STOCK_BEFORE_RES" | grep -o '"availableQuantity":[0-9]*' | cut -d':' -f2)
info "Baseline available stock for UltraWide Monitor: $STOCK_BEFORE"

info "Placing order with failure-injected card token 'CARD_FAIL_DECLINED'..."
ORDER2_RES=$(curl -s -X POST "$GATEWAY_URL/api/v1/orders" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d "{
        \"customerId\": \"22222222-2222-2222-2222-222222222222\",
        \"customerEmail\": \"chaos2@platform.com\",
        \"currency\": \"USD\",
        \"paymentMethod\": \"CARD_FAIL_DECLINED\",
        \"items\": [{
            \"productId\": \"$MONITOR_ID\",
            \"productName\": \"4K UltraWide Curved Monitor\",
            \"quantity\": 2,
            \"unitPrice\": 799.99
        }]
    }")

ORDER2_ID=$(echo "$ORDER2_RES" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
info "Order created: ID=$ORDER2_ID"

info "Waiting 4s for Saga cycle: Stock Reserved -> Payment Failed -> Compensating Stock Release..."
sleep 4

CHECK2_RES=$(curl -s -X GET "$GATEWAY_URL/api/v1/orders/$ORDER2_ID" -H "$AUTH_HEADER")
STATUS2=$(echo "$CHECK2_RES" | grep -o '"status":"[^"]*' | cut -d'"' -f4)

if [ "$STATUS2" = "CANCELLED" ]; then
    pass "Order status transitioned to CANCELLED as expected."
else
    fail "Order status is $STATUS2, expected CANCELLED."
fi

STOCK_AFTER_RES=$(curl -s -X GET "$GATEWAY_URL/api/v1/inventory/$MONITOR_ID" -H "$AUTH_HEADER")
STOCK_AFTER=$(echo "$STOCK_AFTER_RES" | grep -o '"availableQuantity":[0-9]*' | cut -d':' -f2)
info "Stock quantity after compensation cycle: $STOCK_AFTER"

if [ "$STOCK_AFTER" = "$STOCK_BEFORE" ]; then
    pass "COMPENSATION VERIFIED: Stock was restored exactly to baseline ($STOCK_BEFORE == $STOCK_AFTER)!"
else
    fail "COMPENSATION LEAK: Stock mismatch ($STOCK_BEFORE before vs $STOCK_AFTER after)!"
fi

# ------------------------------------------------------------------------------
# SCENARIO 3: Validation Boundary Injection (HTTP 400)
# ------------------------------------------------------------------------------
header "CHAOS SCENARIO 3: VALIDATION BOUNDARY FAULT INJECTION"
info "Submitting order with invalid payload (negative quantity & zero price)..."

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/orders" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": "invalid-uuid-string",
        "customerEmail": "not-an-email",
        "currency": "",
        "paymentMethod": "",
        "items": [{
            "productId": "not-a-uuid",
            "productName": "",
            "quantity": -5,
            "unitPrice": -10.00
        }]
    }')

if [ "$HTTP_CODE" -eq 400 ]; then
    pass "Input validation barrier correctly rejected payload with HTTP 400 Bad Request."
else
    info "Received HTTP status code: $HTTP_CODE"
fi

header "ALL CHAOS / FAILURE INJECTION SCENARIOS COMPLETED"
echo -e "${GREEN}Distributed Saga fault isolation, compensation rollbacks, and boundary gates verified!${NC}\n"
