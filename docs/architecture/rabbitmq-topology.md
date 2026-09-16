# RabbitMQ Topology & Dead Letter Queue (DLQ)

The platform utilizes a structured topic exchange architecture with dedicated retry and dead-letter queues.

## Topology Table

| Exchange | Type | Queues Bound | Routing Key | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `order.events.exchange` | Topic | `inventory.order-created.queue` | `order.created` | Triggers stock reservation |
| `order.events.exchange` | Topic | `inventory.order-cancelled.queue` | `order.cancelled` | Triggers stock release compensation |
| `order.events.exchange` | Topic | `payment.order-cancelled.queue` | `order.cancelled` | Triggers refund |
| `order.events.exchange` | Topic | `notification.events.queue` | `order.*` | Dispatches customer emails |
| `inventory.events.exchange` | Topic | `payment.inventory-reserved.queue` | `inventory.reserved` | Initiates payment flow |
| `inventory.events.exchange` | Topic | `order.inventory-response.queue` | `inventory.*` | Updates Order status |
| `payment.events.exchange` | Topic | `order.payment-response.queue` | `payment.*` | Confirms or cancels order |
| `ecommerce.dlq.exchange` | Topic | `order.dlq`, `inventory.dlq`, `payment.dlq` | `dlq.#` | Dead letter triage |

## Retry & Dead Letter Handling Flow

```text
               Main Queue
                   │
                   ▼
                Consumer
                /      \
            SUCCESS    FAILURE
              │           │
              ▼           ▼
             ACK     Retry Queue (x-message-ttl: 5000ms)
                          │
                          ▼  (after TTL expiry)
                     Main Queue
                          │
               (After max 3 retries)
                          ▼
                  ecommerce.dlq.exchange
                          │
                          ▼
                     Service DLQ
```
