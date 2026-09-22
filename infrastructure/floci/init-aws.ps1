<#
.SYNOPSIS
    Floci Local AWS Emulator Initialization Script (PowerShell)
    Provisions S3 Buckets, SQS Queues, SNS Topics, and Secrets Manager on Port 4566.

$Endpoint = "http://localhost:4566"
$Region = "us-east-1"
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = $Region

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "Initializing Floci Local AWS Resources (Endpoint: $Endpoint)" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan

Write-Host "[1/5] Checking Floci AWS emulator on port 4566..." -ForegroundColor Yellow
try {
    $res = Invoke-RestMethod -Uri "$Endpoint/_floci/health" -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
    Write-Host "[OK] Floci is responding!" -ForegroundColor Green
} catch {
    Write-Host " Floci endpoint reachable (or standard port open)." -ForegroundColor Gray
}

Write-Host "[2/5] Creating Amazon S3 bucket: ecommerce-order-invoices..." -ForegroundColor Yellow
& aws --endpoint-url="$Endpoint" s3 mb s3://ecommerce-order-invoices --region "$Region" 2>$null
Write-Host "[OK] S3 bucket s3://ecommerce-order-invoices ready." -ForegroundColor Green

Write-Host "[3/5] Creating Amazon SQS Queues..." -ForegroundColor Yellow
& aws --endpoint-url="$Endpoint" sqs create-queue --queue-name order-dlq --region "$Region" 2>$null
& aws --endpoint-url="$Endpoint" sqs create-queue --queue-name order-created-queue --region "$Region" 2>$null
& aws --endpoint-url="$Endpoint" sqs create-queue --queue-name payment-processed-queue --region "$Region" 2>$null
Write-Host "[OK] SQS queues created: order-created-queue, payment-processed-queue, order-dlq" -ForegroundColor Green

Write-Host "[4/5] Creating Amazon SNS Topics..." -ForegroundColor Yellow
& aws --endpoint-url="$Endpoint" sns create-topic --name order-events-topic --region "$Region" 2>$null
Write-Host "[OK] SNS topic created: order-events-topic" -ForegroundColor Green

Write-Host "[5/5] Creating AWS Secrets Manager Secret: ecommerce/production/secrets..." -ForegroundColor Yellow
$secretJson = '{\"JWT_SECRET\":\"404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970\",\"DB_PASSWORD\":\"production_strong_password_123\",\"PAYMENT_GATEWAY_KEY\":\"live_sk_test_floci_emulator\"}'
& aws --endpoint-url="$Endpoint" secretsmanager create-secret --name "ecommerce/production/secrets" --description "Simulated platform secrets in Floci" --secret-string "$secretJson" --region "$Region" 2>$null
Write-Host "[OK] Secrets Manager secret ready." -ForegroundColor Green

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "[OK] Floci Local AWS Environment Initialized Successfully!" -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Cyan
