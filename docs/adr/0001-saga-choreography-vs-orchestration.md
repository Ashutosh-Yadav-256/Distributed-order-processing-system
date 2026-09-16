# ADR-0001: Saga Choreography vs. Centralized Orchestration

## Status
**Accepted** (2026-09-16)

## Context
In a distributed e-commerce architecture, placing an order requires mutating data across three distinct bounded contexts:
1. **Order Service**: Persisting order entities and tracking lifecycle state (`PENDING`, `CONFIRMED`, `CANCELLED`, `FAILED`).
2. **Inventory Service**: Checking SKU availability and placing atomic holds on reserved quantities.
3. **Payment Service**: Authorizing payments with third-party gateways and generating transaction receipts.

Two-Phase Commit (2PC) is ruled out due to:
- Synchronous blocking protocols that reduce availability (CAP theorem trade-off: favoring availability and partition tolerance over strict consistency).
- Performance degradation and distributed deadlocks across high-latency network boundaries.

We evaluated two architectural patterns for managing distributed transactions:
- **Option A: Centralized Orchestration**: A dedicated coordinator service directs each participant using command/reply messaging.
- **Option B: Distributed Choreography**: Services react autonomously to domain events published to a message broker, publishing subsequent events upon success or failure.

## Decision
We chose **Distributed Saga Choreography** using RabbitMQ topic exchanges.

Each service:
1. Listens for upstream domain events (`order.created`, `inventory.reserved`, `payment.completed`, `payment.failed`, `order.cancelled`).
2. Executes its local database transaction within an atomic `@Transactional` boundary.
3. Emits domain events or compensating failure events.
4. Compensating actions are self-contained:
   - If inventory fails reservation -> emits `inventory.reservation.failed` -> Order Service cancels order.
   - If payment fails -> emits `payment.failed` -> Order Service marks `CANCELLED` and emits `order.cancelled` with `compensationRequired=true` -> Inventory Service releases reserved stock back to available pool.

## Consequences

### Positive
- **Loose Coupling**: Services have zero direct knowledge of downstream participants; Order Service does not know how Inventory decrements stock or how Payment talks to Stripe/Adyen.
- **No Single Point of Failure**: Eliminates the orchestrator bottleneck and single point of coordination.
- **Independent Deployability**: Services can be updated, scaled, and deployed independently without modifying an orchestration workflow engine.

### Negative / Trade-offs Accepted
- **Complexity in Event Tracing**: Requires distributed tracing (OpenTelemetry / Micrometer Tracing with W3C traceparent headers) to reconstruct cross-service execution flows.
- **Eventual Consistency**: There is a brief window (typically 100-300ms) where an order is `PENDING` while background events transit the message broker.

## Verification
- Unit test: `services/order-service/src/test/java/com/platform/order/service/OrderServiceTest.java` verifies compensating event publication.
- End-to-end automated test: `scripts/test-saga.ps1` and `scripts/test-failure-injection.ps1` execute full compensation rollback cycles and assert exact stock replenishment.
