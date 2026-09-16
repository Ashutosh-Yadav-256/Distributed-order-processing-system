# ADR-0002: Database-per-Service Isolation Pattern

## Status
**Accepted** (2026-09-16)

## Context
Microservices architectures risk becoming a "distributed monolith" if multiple services read and write to a shared monolithic database. Shared database anti-patterns include:
- Cross-domain table joins that couple schemas.
- Inability to evolve or migrate a single service's schema without risking breaking other services.
- Resource contention, lock escalation, and shared connection pool exhaustion.

## Decision
We adopted the **Database-per-Service** pattern.
- `order-service` connects exclusively to `order_db`
- `inventory-service` connects exclusively to `inventory_db`
- `payment-service` connects exclusively to `payment_db`

In local development and Docker Compose, each service accesses its own isolated PostgreSQL database instance / logical database with dedicated credentials. No service has direct network or JDBC access to another service's data store.

Cross-service data aggregation is strictly prohibited at the database tier and is achieved solely via:
1. Event-driven data propagation (e.g. Order Service passing essential customer/item metadata within the event payload).
2. API composition via API Gateway or synchronous REST read endpoints where appropriate.

## Consequences

### Positive
- **Schema Autonomy**: Changes to the `orders` or `order_items` tables can be released and migrated with zero impact on `product_inventory` or `payments`.
- **Fault Isolation**: A slow query or table lock in `inventory_db` cannot starve database connections or CPU cycles needed by `payment_service`.
- **Targeted Scaling**: High-write databases can be provisioned with faster NVMe storage or read-replicas independently.

### Negative / Trade-offs Accepted
- **No ACID Cross-Service Transactions**: ACID guarantees are confined to a single service boundary. Cross-service consistency requires Sagas and compensating transactions (see ADR-0001).
- **Data Duplication**: Basic attributes (e.g., product name and unit price at time of order) are duplicated in `order_items` to preserve historical integrity.

## Verification
- Docker Compose configuration (`docker-compose.yml`) provisions isolated PostgreSQL databases (`order_db`, `inventory_db`, `payment_db`).
- In-memory integration tests (`application-test.yml`) configure unique in-memory H2 databases (`jdbc:h2:mem:order_db`, `jdbc:h2:mem:inventory_db`, `jdbc:h2:mem:payment_db`) ensuring zero shared state.
