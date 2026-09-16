# System Architecture

The **Distributed Order Processing System** is designed as a resilient, event-driven microservices platform handling end-to-end e-commerce order lifecycles.

```mermaid
graph TD
    Client[Web / Mobile / API Client] -->|HTTPS| GW[API Gateway :8080]
    
    subgraph Core Services
        GW -->|Route /api/v1/orders| OS[Order Service :8081]
        GW -->|Route /api/v1/inventory| IS[Inventory Service :8082]
        GW -->|Route /api/v1/payments| PS[Payment Service :8083]
        GW -->|Route /api/v1/notifications| NS[Notification Service :8084]
    end

    subgraph Messaging Backbone - RabbitMQ
        OS -->|order.created / order.cancelled| EX_O[order.events.exchange]
        IS -->|inventory.reserved / failed| EX_I[inventory.events.exchange]
        PS -->|payment.completed / failed| EX_P[payment.events.exchange]
        
        EX_O --> Q_IC[inventory.order-created.queue]
        EX_O --> Q_NC[notification.events.queue]
        EX_I --> Q_PR[payment.inventory-reserved.queue]
        EX_I --> Q_OR[order.inventory-response.queue]
        EX_P --> Q_OP[order.payment-response.queue]
        EX_P --> Q_NC
    end

    subgraph Data & Cache Layer
        OS --> DB_O[(order_db)]
        IS --> DB_I[(inventory_db)]
        IS --> REDIS[(Redis Hot Inventory)]
        PS --> DB_P[(payment_db)]
        NS --> DB_N[(notification_db)]
    end
```

## Architectural Principles
1. **Database-per-service**: Microservices do not query each other's databases directly. They communicate asynchronously via RabbitMQ events and synchronously via REST through the API Gateway.
2. **Authoritative Consistency**: PostgreSQL holds durable state with ACID guarantees; Redis serves as a read-through cache for low-latency inventory reads.
3. **Eventual Consistency with Sagas**: Cross-service workflows use the Saga pattern with automatic compensation rather than fragile two-phase commit (2PC) locks.
4. **Idempotency**: All consumers implement message deduplication using a `processed_events` table to protect against network retransmissions and RabbitMQ duplicate deliveries.
