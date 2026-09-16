package com.platform.inventory.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.OrderCreatedEvent;
import com.platform.common.idempotency.IdempotencyService;
import com.platform.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    private static final String CONSUMER_NAME = "InventoryEventConsumer";

    @RabbitListener(queues = RabbitMQConstants.INVENTORY_ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
            return;
        }

        log.info("Received OrderCreatedEvent for orderId: {}", event.getOrderId());
        inventoryService.reserveStock(event);
        idempotencyService.markAsProcessed(event.getEventId(), "OrderCreatedEvent", CONSUMER_NAME);
    }

    @RabbitListener(queues = RabbitMQConstants.INVENTORY_ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
            return;
        }

        log.info("Received OrderCancelledEvent for orderId: {}, compensationRequired: {}",
                event.getOrderId(), event.isCompensationRequired());
        if (event.isCompensationRequired()) {
            inventoryService.releaseStock(event);
        }
        idempotencyService.markAsProcessed(event.getEventId(), "OrderCancelledEvent", CONSUMER_NAME);
    }
}
