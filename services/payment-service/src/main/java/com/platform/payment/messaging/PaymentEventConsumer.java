package com.platform.payment.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.event.InventoryReservedEvent;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.idempotency.IdempotencyService;
import com.platform.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    private static final String CONSUMER_NAME = "PaymentEventConsumer";

    @RabbitListener(queues = RabbitMQConstants.PAYMENT_INVENTORY_RESERVED_QUEUE)
    public void handleInventoryReserved(InventoryReservedEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
            return;
        }

        log.info("Received InventoryReservedEvent for orderId: {}", event.getOrderId());
        paymentService.processPayment(event);
        idempotencyService.markAsProcessed(event.getEventId(), "InventoryReservedEvent", CONSUMER_NAME);
    }

    @RabbitListener(queues = RabbitMQConstants.PAYMENT_ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
            return;
        }

        log.info("Received OrderCancelledEvent for payment on orderId: {}", event.getOrderId());
        paymentService.handleOrderCancelled(event);
        idempotencyService.markAsProcessed(event.getEventId(), "OrderCancelledEvent", CONSUMER_NAME);
    }
}
