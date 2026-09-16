# Microservices & Distributed Systems

Use this guide to discuss key architectural tradeoffs and design decisions implemented in this repository during technical interviews.

---

### 1. Why Saga Pattern instead of Two-Phase Commit (2PC)?
* **The Problem with 2PC**: Two-Phase Commit requires distributed locking across all participating databases (e.g. holding locks on both `orders` and `product_inventory`). In a cloud environment, network latency, partitions, or coordinator failure leads to high lock contention, cascading timeouts, and availability crashes (violating the CAP theorem).
* **The Saga Solution**: Each microservice commits its local ACID transaction immediately. If a downstream step fails (e.g. payment decline), the system issues a **compensating transaction** (e.g. `ReleaseStock`) to semantically undo earlier work. This provides high availability and fault isolation.

---

### 2. How is Idempotency guaranteed across RabbitMQ consumers?
* **The Risk**: RabbitMQ guarantees "at-least-once" delivery. Network glitches or consumer restarts right before an ACK can cause duplicate message redeliveries. Processing an event twice could double-charge a customer or reserve duplicate inventory.
* **Our Solution**: 
  - Every published domain event carries a unique `eventId` (UUID).
  - Each microservice maintains a `processed_events` table indexed by `(eventId, consumerName)`.
  - When an event arrives, the consumer checks `existsByEventIdAndConsumerName(eventId, consumerName)`. If already recorded, the message is immediately acknowledged and discarded without re-execution.

---

### 3. How do you prevent Redis cache inconsistency with PostgreSQL?
* **The Risk**: If stock is decremented in Redis first and the database transaction fails, the cache has phantom stock. If stock is updated in DB but cache update fails, users see stale stock.
* **Our Solution**:
  - **PostgreSQL is authoritative**: Inventory reservations execute an atomic decrement on PostgreSQL (`UPDATE product_inventory SET available_quantity = available_quantity - :qty WHERE available_quantity >= :qty`).
  - **Cache Eviction over Cache Update**: Rather than calculating and writing the new quantity into Redis, we **evict** (`DEL inventory:product:{id}`). The next incoming read triggers a cache-miss that loads the authoritative number from PostgreSQL with a short TTL (60s). This eliminates race conditions between concurrent updates.

---

### 4. What is the Dead Letter Queue (DLQ) & Retry strategy?
* **Transient Errors**: Network timeouts or database connection pool exhaustion are retried up to 3 times using RabbitMQ retry queues with backoff (`x-message-ttl`).
* **Non-Transient Errors**: Unparseable payloads, malformed JSON, or permanent processing errors are routed to `ecommerce.dlq.exchange` and into `*.dlq`.
* **Operational Benefit**: Prevents "poison pill" messages from blocking queues or causing infinite crash loops. Operations teams can inspect the DLQ, fix underlying issues, and replay messages.

---

### 5. Why Resilience4j Circuit Breakers?
* **The Problem**: If a downstream dependency slows down or becomes unresponsive, client requests pile up, exhausting thread pools and causing cascading system failure.
* **Resilience4j Behavior**:
  - **CLOSED**: Requests proceed normally.
  - **OPEN**: If 50% of recent requests fail, the circuit breaker opens, failing fast immediately without waiting for timeouts.
  - **HALF-OPEN**: Periodically sends probe requests to test whether the downstream service has recovered before switching back to CLOSED.
