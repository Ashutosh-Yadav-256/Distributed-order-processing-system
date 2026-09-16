package com.platform.common.constant;

public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    // Main Topic Exchanges
    public static final String ORDER_EXCHANGE = "order.events.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.events.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.events.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.events.exchange";
    public static final String DLQ_EXCHANGE = "ecommerce.dlq.exchange";

    // Routing Keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_CANCELLED_KEY = "order.cancelled";
    public static final String ORDER_CONFIRMED_KEY = "order.confirmed";

    public static final String INVENTORY_RESERVED_KEY = "inventory.reserved";
    public static final String INVENTORY_FAILED_KEY = "inventory.reservation.failed";

    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    public static final String DLQ_ROUTING_KEY_PREFIX = "dlq.";

    // Queues
    public static final String INVENTORY_ORDER_CREATED_QUEUE = "inventory.order-created.queue";
    public static final String INVENTORY_ORDER_CANCELLED_QUEUE = "inventory.order-cancelled.queue";

    public static final String PAYMENT_INVENTORY_RESERVED_QUEUE = "payment.inventory-reserved.queue";
    public static final String PAYMENT_ORDER_CANCELLED_QUEUE = "payment.order-cancelled.queue";

    public static final String ORDER_INVENTORY_RESPONSE_QUEUE = "order.inventory-response.queue";
    public static final String ORDER_PAYMENT_RESPONSE_QUEUE = "order.payment-response.queue";

    public static final String NOTIFICATION_EVENTS_QUEUE = "notification.events.queue";

    // Dead Letter & Retry Queues
    public static final String PAYMENT_RETRY_QUEUE = "payment.retry.queue";
    public static final String PAYMENT_DLQ = "payment.dlq";

    public static final String INVENTORY_RETRY_QUEUE = "inventory.retry.queue";
    public static final String INVENTORY_DLQ = "inventory.dlq";

    public static final String ORDER_DLQ = "order.dlq";
}
