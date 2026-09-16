# Architecture Decision Records (ADRs)

This directory documents the significant architectural and design decisions made in the **Distributed Order Processing System**, following the [Michael Nygard ADR format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

Each ADR captures:
- **Context**: The business and technical problem, constraints, and forces at play.
- **Decision**: The architectural choice made and its concrete implementation details.
- **Consequences**: Positive outcomes, operational trade-offs, and negative consequences accepted.
- **Compliance & Verification**: How the decision is verified and tested in the codebase.

---

## Decision Log

| ID | Title | Status | Date | Primary Driver |
|:---|:------|:-------|:-----|:---------------|
| [ADR-0001](0001-saga-choreography-vs-orchestration.md) | Saga Choreography vs. Centralized Orchestration | **Accepted** | 2026-09-16 | Decentralization & Loose Coupling |
| [ADR-0002](0002-database-per-service-pattern.md) | Database-per-Service Isolation Pattern | **Accepted** | 2026-09-16 | Domain Boundary Autonomy & Blast Radius Reduction |
| [ADR-0003](0003-rabbitmq-topic-exchanges-and-dlq.md) | RabbitMQ Topic Exchanges with Dead-Letter Queues | **Accepted** | 2026-09-16 | Reliable At-Least-Once Delivery & Poison Pill Isolation |
| [ADR-0004](0004-redis-read-through-caching-strategy.md) | Redis Read-Through Caching & Targeted Eviction | **Accepted** | 2026-09-16 | High-Throughput Read Latency (15x Speedup) |
| [ADR-0005](0005-idempotent-consumer-deduplication.md) | Idempotent Consumer Pattern via Processed Events Store | **Accepted** | 2026-09-16 | Exactly-Once Processing Semantics over AMQP |
| [ADR-0006](0006-resilience4j-fault-tolerance-and-rate-limiting.md) | Resilience4j Circuit Breakers and Graceful Degradation | **Accepted** | 2026-09-16 | Third-Party Failure Containment & Gateway Defense |
