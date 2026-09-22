#!/usr/bin/env bash

set -euo pipefail

ENDPOINT_URL="http://localhost:4566"
REGION="us-east-1"

export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="${REGION}"

echo "========================================================================"
echo "Initializing Floci Local AWS Resources (Endpoint: ${ENDPOINT_URL})"
echo "========================================================================"

echo "[1/5] Waiting for Floci AWS emulator on port 4566..."
until curl -s "${ENDPOINT_URL}/_floci/health" > /dev/null 2>&1 || curl -s "${ENDPOINT_URL}" > /dev/null 2>&1; do
    echo "  Floci not yet responding, waiting 1s..."
    sleep 1
done
echo "[OK] Floci is responding!"

echo "[2/5] Creating Amazon S3 bucket: ecommerce-order-invoices..."
aws --endpoint-url="${ENDPOINT_URL}" s3 mb s3://ecommerce-order-invoices --region "${REGION}" || true
aws --endpoint-url="${ENDPOINT_URL}" s3api put-bucket-cors --bucket ecommerce-order-invoices --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "HEAD"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3000
    }
  ]
}' || true
echo "[OK] S3 bucket s3://ecommerce-order-invoices created and configured."

echo "[3/5] Creating Amazon SQS Queues..."
aws --endpoint-url="${ENDPOINT_URL}" sqs create-queue --queue-name order-dlq --region "${REGION}" || true
aws --endpoint-url="${ENDPOINT_URL}" sqs create-queue --queue-name order-created-queue --region "${REGION}" || true
aws --endpoint-url="${ENDPOINT_URL}" sqs create-queue --queue-name payment-processed-queue --region "${REGION}" || true
echo "[OK] SQS queues created: order-created-queue, payment-processed-queue, order-dlq"

echo "[4/5] Creating Amazon SNS Topics..."
aws --endpoint-url="${ENDPOINT_URL}" sns create-topic --name order-events-topic --region "${REGION}" || true
echo "[OK] SNS topic created: order-events-topic"

echo "[5/5] Creating AWS Secrets Manager Secret: ecommerce/production/secrets..."
aws --endpoint-url="${ENDPOINT_URL}" secretsmanager create-secret \
    --name "ecommerce/production/secrets" \
    --description "Simulated platform secrets in Floci" \
    --secret-string '{"JWT_SECRET":"404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970","DB_PASSWORD":"production_strong_password_123","PAYMENT_GATEWAY_KEY":"live_sk_test_floci_emulator"}' \
    --region "${REGION}" || true
echo "[OK] Secrets Manager secret created."

echo "========================================================================"
echo "[OK] Floci Local AWS Environment Initialized Successfully!"
echo "========================================================================"
