# Distributed Saga Pattern & Failure Compensation

In this system, distributed transactions are managed through a **Choreography-based Saga with Order Service state tracking**.

## 1. Happy Path Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Gateway as API Gateway
    participant Order as Order Service
    participant Rabbit as RabbitMQ
    participant Inventory as Inventory Service
    participant Payment as Payment Service
    participant Notification as Notification Service

    Customer->>Gateway: POST /api/v1/orders
    Gateway->>Order: Forward Request
    Order->>Order: Save Order (PENDING)
    Order->>Rabbit: Publish OrderCreatedEvent
    
    par Async Processing
        Rabbit->>Inventory: Deliver to inventory.order-created.queue
        Rabbit->>Notification: Deliver OrderCreated notification
    end

    Inventory->>Inventory: Atomically reserve stock in DB & evict Redis
    Inventory->>Rabbit: Publish InventoryReservedEvent

    par Downstream Notification & Payment
        Rabbit->>Order: Update state -> INVENTORY_RESERVED
        Rabbit->>Payment: Deliver to payment.inventory-reserved.queue
    end

    Payment->>Payment: Simulate Gateway Authorization
    Payment->>Rabbit: Publish PaymentCompletedEvent

    par Confirmations
        Rabbit->>Order: Update state -> CONFIRMED
        Order->>Rabbit: Publish OrderConfirmedEvent
        Rabbit->>Inventory: Commit reservation
        Rabbit->>Notification: Send Receipt Email/SMS
    end
```

---

## 2. Failure Scenarios & Compensating Transactions

### Scenario 1: Insufficient Inventory
If requested quantity exceeds available stock:
1. `InventoryService` detects shortage (`availableQuantity < requestedQuantity`).
2. Rolls back any partial reservations in the batch.
3. Emits `InventoryReservationFailedEvent`.
4. `OrderService` transitions order status to `FAILED`.
5. No payment is ever attempted.

### Scenario 2: Payment Declined & Inventory Compensation
If payment authorization fails (card declined / network error):
1. `PaymentService` emits `PaymentFailedEvent`.
2. `OrderService` marks order status as `CANCELLED`.
3. `OrderService` emits `OrderCancelledEvent` with `compensationRequired = true`.
4. `InventoryService` consumes `OrderCancelledEvent`:
   - Runs compensating SQL: `UPDATE product_inventory SET available_quantity = available_quantity + :qty, reserved_quantity = reserved_quantity - :qty`.
   - Sets `StockReservation` status to `RELEASED`.
   - Evicts Redis cache to restore real-time availability.
5. `NotificationService` alerts customer of declined transaction.
