#!/usr/bin/env bash
# Redis Read-Through Caching Benchmark Script (Bash)
# Measures latency differences between direct PostgreSQL queries and Redis-cached queries.

set -euo pipefail

INVENTORY_URL="${1:-http://localhost:8082}"
ITERATIONS="${2:-100}"

echo "================================================================"
echo " Inventory Service - Redis Read-Through Cache Benchmark"
echo " Target URL: $INVENTORY_URL | Requests per test: $ITERATIONS"
echo "================================================================"

# Check health
if ! curl -sf "$INVENTORY_URL/actuator/health" > /dev/null; then
    echo "✗ Failed to reach Inventory Service at $INVENTORY_URL."
    echo "  Please ensure the service is running."
    exit 1
fi
echo "✓ Inventory service is healthy."

# Fetch or seed product
PRODUCTS_JSON=$(curl -sf "$INVENTORY_URL/api/v1/inventory")
PRODUCT_ID=$(echo "$PRODUCTS_JSON" | grep -o '"productId":"[^"]*' | head -n 1 | cut -d'"' -f4 || true)

if [ -z "$PRODUCT_ID" ]; then
    echo "Seeding test product..."
    CREATE_RESP=$(curl -sf -X POST "$INVENTORY_URL/api/v1/inventory" \
        -H "Content-Type: application/json" \
        -d '{"name":"High-Performance Mechanical Keyboard","sku":"BENCH-KEYBOARD-001","availableQuantity":500,"price":149.99}')
    PRODUCT_ID=$(echo "$CREATE_RESP" | grep -o '"productId":"[^"]*' | head -n 1 | cut -d'"' -f4)
fi

echo "Benchmarking Product ID: $PRODUCT_ID"
echo ""

# 1. Warm Cache benchmark
echo "1. Warming up Redis cache..."
curl -sf "$INVENTORY_URL/api/v1/inventory/$PRODUCT_ID" > /dev/null
sleep 0.1

echo "2. Running $ITERATIONS warm requests (Redis Cache HIT)..."
WARM_TIMES_FILE=$(mktemp)
for i in $(seq 1 "$ITERATIONS"); do
    TIME_TAKEN=$(curl -o /dev/null -s -w '%{time_total}\n' "$INVENTORY_URL/api/v1/inventory/$PRODUCT_ID")
    # Convert seconds to ms
    TIME_MS=$(awk "BEGIN {print $TIME_TAKEN * 1000}")
    echo "$TIME_MS" >> "$WARM_TIMES_FILE"
done

# 2. Cold benchmark (evicting cache before each call)
echo "3. Running $ITERATIONS cold requests (Forced DB Fetch)..."
COLD_TIMES_FILE=$(mktemp)
for i in $(seq 1 "$ITERATIONS"); do
    docker exec platform-redis redis-cli del "inventory:product:$PRODUCT_ID" > /dev/null 2>&1 || true
    TIME_TAKEN=$(curl -o /dev/null -s -w '%{time_total}\n' "$INVENTORY_URL/api/v1/inventory/$PRODUCT_ID")
    TIME_MS=$(awk "BEGIN {print $TIME_TAKEN * 1000}")
    echo "$TIME_MS" >> "$COLD_TIMES_FILE"
done

# Calculate stats
calc_stats() {
    sort -n "$1" | awk '
    BEGIN { c = 0; sum = 0; }
    { a[c++] = $1; sum += $1; }
    END {
        avg = sum / c;
        min = a[0];
        max = a[c-1];
        p50 = a[int(c * 0.50)];
        p95 = a[int(c * 0.95)];
        p99 = a[int(c * 0.99)];
        printf "%.2f %.2f %.2f %.2f %.2f %.2f", min, max, avg, p50, p95, p99;
    }'
}

read -r WARM_MIN WARM_MAX WARM_AVG WARM_P50 WARM_P95 WARM_P99 <<< "$(calc_stats "$WARM_TIMES_FILE")"
read -r COLD_MIN COLD_MAX COLD_AVG COLD_P50 COLD_P95 COLD_P99 <<< "$(calc_stats "$COLD_TIMES_FILE")"

rm -f "$WARM_TIMES_FILE" "$COLD_TIMES_FILE"

echo ""
echo "================================================================"
echo "                BENCHMARK RESULTS ($ITERATIONS Requests)"
echo "================================================================"
printf "%-18s | %-18s | %-18s\n" "Metric" "Cold (PostgreSQL)" "Warm (Redis Hit)"
echo "----------------------------------------------------------------"
printf "%-18s | %-18s | %-18s\n" "Min Latency" "${COLD_MIN} ms" "${WARM_MIN} ms"
printf "%-18s | %-18s | %-18s\n" "Avg Latency" "${COLD_AVG} ms" "${WARM_AVG} ms"
printf "%-18s | %-18s | %-18s\n" "P50 Latency" "${COLD_P50} ms" "${WARM_P50} ms"
printf "%-18s | %-18s | %-18s\n" "P95 Latency" "${COLD_P95} ms" "${WARM_P95} ms"
printf "%-18s | %-18s | %-18s\n" "P99 Latency" "${COLD_P99} ms" "${WARM_P99} ms"
echo "================================================================"
