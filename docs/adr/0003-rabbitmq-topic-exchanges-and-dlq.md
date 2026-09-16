# ADR-0003: RabbitMQ Topic Exchanges with Dead-Letter Queues (DLQ)

## Status
**Accepted** (2026-09-16)

## Context
Asynchronous event communication requires a message broker strategy that provides:
1. Flexible pub/sub routing based on event categories and routing keys.
2. Independent consumer queues so multiple downstream listeners can process the same event concurrently without competing for messages.
3. Resilience against "poison pill" messages (malformed JSON or unexpected nulls) that cause consumers to continuously fail and crash in an infinite redelivery loop.

## Decision
We implemented **RabbitMQ Topic Exchanges with Dead Letter Exchanges (DLX) & Dead Letter Queues (DLQ)**.

1. **Exchange Design**:
   - `order.exchange` (topic): routes events matching `order.created`, `order.cancelled`, `order.completed`.
   - `inventory.exchange` (topic): routes `inventory.reserved`, `inventory.reservation.failed`.
   - `payment.exchange` (topic): routes `payment.completed`, `payment.failed`.

2. **Consumer Queues**:
   - Dedicated queues per consumer domain (e.g. `order.inventory.created.queue`, `order.payment.reserved.queue`, `notification.order.queue`).

3. **Dead-Letter Handling (DLQ)**:
   - Queues are configured with `x-dead-letter-exchange: dlx.exchange` and `x-dead-letter-routing-key: <queue-name>.dlq`.
   - Failed messages exceeding retry thresholds (3 attempts with exponential backoff) are routed to the DLQ instead of blocking the main processing pipe.
   - Operators can inspect, re-queue, or alert on messages in the DLQ via RabbitMQ Management API or Grafana.

## Consequences

### Positive
- **Pub/Sub Granularity**: Topic exchanges allow services like `notification-service` to subscribe broadly (`order.*`, `payment.*`) while `payment-service` only listens to `inventory.reserved`.
- **System Stability**: Corrupt payloads do not cause infinite crash loops; they are quarantined in the DLQ.
- **Backpressure & Buffer**: Message queues decouple producers from consumers, buffering bursts of peak order traffic.

### Negative / Trade-offs Accepted
- **Broker Infrastructure Overhead**: Requires provisioning and maintaining RabbitMQ cluster nodes and monitoring disk alarms and queue lengths.

## Verification
- Spring RabbitMQ configurations (`RabbitMQConfig.java` in each service) configure explicit exchanges, queues, and bindings with `@Qualifier` declarations.
- Verified in integration tests (`OrderControllerIntegrationTest`, `InventoryControllerIntegrationTest`, `PaymentControllerIntegrationTest`).
