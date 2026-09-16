# ADR-0005: Idempotent Consumer Pattern via Processed Events Store

## Status
**Accepted** (2026-09-16)

## Context
RabbitMQ delivers messages with **at-least-once** delivery guarantees. Network blips, consumer restart during ACK processing, or TCP connection resets can cause the broker to redeliver a message that was already executed.

In a payment or inventory system, duplicate message execution would cause catastrophic business failures:
- Charging a customer's credit card twice for the same order.
- Decrementing stock twice, leading to incorrect inventory counts.

## Decision
We implemented the **Idempotent Consumer Pattern** backed by a relational `processed_events` table in `common-library` and each service's database.

1. **Schema**:
   ```sql
   CREATE TABLE processed_events (
       id UUID PRIMARY KEY,
       event_id UUID NOT NULL,
       consumer_name VARCHAR(100) NOT NULL,
       event_type VARCHAR(100) NOT NULL,
       processed_at TIMESTAMP NOT NULL,
       CONSTRAINT uq_event_consumer UNIQUE (event_id, consumer_name)
   );
   ```

2. **Execution Flow**:
   - Before executing business logic, the consumer queries:
     `processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName)`
   - If `true`: The consumer logs a warning (`"Duplicate event detected, skipping execution"`), acknowledges the AMQP message, and exits cleanly.
   - If `false`: The business logic executes, and the `ProcessedEvent` record is inserted within the **same local database transaction** as the business entity changes.

## Consequences

### Positive
- **Guaranteed Exactly-Once Processing Semantics**: Side-effects occur exactly once regardless of network redeliveries or consumer crashes.
- **Transactional Atomicity**: Because the `processed_events` insert shares the same database transaction as the entity mutation, either both commit or both roll back together.

### Negative / Trade-offs Accepted
- **Storage Overhead**: Requires periodic cleanup/purging of old event records (e.g. 30-day retention job).
- **Index Lookup Cost**: An indexed unique key lookup is incurred on every message consumption.

## Verification
- Entity defined in `common-library/src/main/java/com/platform/common/idempotency/ProcessedEvent.java`.
- Verified in `scripts/test-failure-injection.ps1` (Scenario 5: Idempotency & Replay Resiliency Check).
