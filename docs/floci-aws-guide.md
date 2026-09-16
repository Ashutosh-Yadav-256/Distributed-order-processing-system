# Floci Local AWS Cloud Emulator Guide

This guide describes how to run and use **[Floci (`floci/floci`)](https://github.com/floci-io/floci)** as a lightweight local AWS cloud emulator for the **Distributed Order Processing System**.

---

## What is Floci?

[Floci](https://github.com/floci-io/floci) is a high-speed, open-source local cloud emulator written in **Quarkus Native**, designed as an ultra-fast drop-in alternative to LocalStack:
* **~24ms Startup Time** (vs. 15–45s for LocalStack).
* **~13 MiB Idle Memory** (vs. 1.5–2 GB for LocalStack).
* **Zero Licensing Restrictions**: Always free, open-source, no auth tokens, no pro-tier paywalls.
* **Standard AWS Wire Protocol**: Emulates AWS services on the standard entry point **port `4566`**.

---

## Emulated AWS Services in This Project

In our platform, Floci provides local emulation for:
1. **Amazon S3**:
   - Bucket: `s3://ecommerce-order-invoices`
   - Purpose: Archiving confirmed order receipt JSON/PDF documents.
2. **Amazon SQS**:
   - Queues: `order-created-queue`, `payment-processed-queue`, `order-dlq`
   - Purpose: Local testing of AWS queue-based event workers.
3. **Amazon SNS**:
   - Topic: `order-events-topic`
   - Purpose: Fanout event notification topic.
4. **AWS Secrets Manager**:
   - Secret: `ecommerce/production/secrets`
   - Purpose: Storing simulated JWT secrets, DB credentials, and payment API keys.

---

## Quick Start

### 1. Start Floci with Docker Compose
To spin up Floci alongside PostgreSQL, Redis, and RabbitMQ:
```bash
docker compose up -d floci-aws
```

Verify Floci is responding on port 4566:
```bash
curl http://localhost:4566
```

### 2. Initialize AWS Resources
Run the automated initialization script:

**On Windows (PowerShell):**
```powershell
.\infrastructure\floci\init-aws.ps1
```

**On Linux / macOS (Bash):**
```bash
chmod +x ./infrastructure/floci/init-aws.sh
./infrastructure/floci/init-aws.sh
```

---

## Using AWS CLI with Floci

Standard AWS CLI commands work seamlessly by passing `--endpoint-url=http://localhost:4566`:

### 1. Amazon S3
```bash
# List buckets
aws --endpoint-url=http://localhost:4566 s3 ls

# List archived order invoices
aws --endpoint-url=http://localhost:4566 s3 ls s3://ecommerce-order-invoices/invoices/

# Download an invoice
aws --endpoint-url=http://localhost:4566 s3 cp s3://ecommerce-order-invoices/invoices/order-xxxx.json .
```

### 2. AWS Secrets Manager
```bash
# Get secrets
aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value --secret-id ecommerce/production/secrets
```

### 3. Amazon SQS
```bash
# List queues
aws --endpoint-url=http://localhost:4566 sqs list-queues
```

---

## Spring Boot Integration

In `services/notification-service`, the AWS Java SDK v2 is configured via [`AwsS3Config.java`](file:///c:/Desktop/CODING%20_IS_LIFE/1%20ANTI%20GRAVITY/Distributed%20Order%20Processing%20System/services/notification-service/src/main/java/com/platform/notification/config/AwsS3Config.java):

```java
@Bean
public S3Client s3Client() {
    return S3Client.builder()
            .endpointOverride(URI.create(endpointUrl)) // http://localhost:4566
            .region(Region.of(region))                // us-east-1
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(true)                      // Required for S3 emulators
            .build();
}
```

Whenever an `OrderConfirmedEvent` is received by [`NotificationEventConsumer`](file:///c:/Desktop/CODING%20_IS_LIFE/1%20ANTI%20GRAVITY/Distributed%20Order%20Processing%20System/services/notification-service/src/main/java/com/platform/notification/messaging/NotificationEventConsumer.java), [`S3InvoiceArchiverService`](file:///c:/Desktop/CODING%20_IS_LIFE/1%20ANTI%20GRAVITY/Distributed%20Order%20Processing%20System/services/notification-service/src/main/java/com/platform/notification/service/S3InvoiceArchiverService.java) automatically formats an invoice JSON payload and uploads it to `s3://ecommerce-order-invoices/invoices/order-{orderId}.json`.

---

## Interactive QA Console Telemetry
The Senior QA Testing Console at `http://localhost:4000` features:
1. **Floci AWS Emulator Health Card**: Live telemetry and status of S3, SQS, SNS, and Secrets Manager.
2. **Amazon S3 Invoice & Cloud Storage Inspector**: Live table displaying all archived invoices stored in the S3 bucket with download keys and amounts.
