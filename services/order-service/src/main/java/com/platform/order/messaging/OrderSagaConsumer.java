package com.platform.order.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.event.InventoryReservationFailedEvent;
import com.platform.common.event.InventoryReservedEvent;
import com.platform.common.event.PaymentCompletedEvent;
import com.platform.common.event.PaymentFailedEvent;
import com.platform.common.idempotency.IdempotencyService;
import com.platform.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaConsumer {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    private static final String CONSUMER_NAME = "OrderSagaConsumer";

    @RabbitListener(queues = RabbitMQConstants.ORDER_INVENTORY_RESPONSE_QUEUE)
    public void handleInventoryResponse(Object event) {
        if (event instanceof InventoryReservedEvent reservedEvent) {
            if (idempotencyService.isAlreadyProcessed(reservedEvent.getEventId(), CONSUMER_NAME)) {
                return;
            }
            log.info("Received InventoryReservedEvent for orderId: {}", reservedEvent.getOrderId());
            orderService.handleInventoryReserved(reservedEvent.getOrderId());
            idempotencyService.markAsProcessed(reservedEvent.getEventId(), "InventoryReservedEvent", CONSUMER_NAME);

        } else if (event instanceof InventoryReservationFailedEvent failedEvent) {
            if (idempotencyService.isAlreadyProcessed(failedEvent.getEventId(), CONSUMER_NAME)) {
                return;
            }
            log.warn("Received InventoryReservationFailedEvent for orderId: {}, reason: {}",
                    failedEvent.getOrderId(), failedEvent.getReason());
            orderService.handleInventoryReservationFailed(failedEvent.getOrderId(), failedEvent.getReason());
            idempotencyService.markAsProcessed(failedEvent.getEventId(), "InventoryReservationFailedEvent", CONSUMER_NAME);
        } else {
            log.warn("Unknown event received on inventory response queue: {}", event);
        }
    }

    @RabbitListener(queues = RabbitMQConstants.ORDER_PAYMENT_RESPONSE_QUEUE)
    public void handlePaymentResponse(Object event) {
        if (event instanceof PaymentCompletedEvent completedEvent) {
            if (idempotencyService.isAlreadyProcessed(completedEvent.getEventId(), CONSUMER_NAME)) {
                return;
            }
            log.info("Received PaymentCompletedEvent for orderId: {}", completedEvent.getOrderId());
            orderService.handlePaymentCompleted(completedEvent.getOrderId());
            idempotencyService.markAsProcessed(completedEvent.getEventId(), "PaymentCompletedEvent", CONSUMER_NAME);

        } else if (event instanceof PaymentFailedEvent failedEvent) {
            if (idempotencyService.isAlreadyProcessed(failedEvent.getEventId(), CONSUMER_NAME)) {
                return;
            }
            log.warn("Received PaymentFailedEvent for orderId: {}, reason: {}",
                    failedEvent.getOrderId(), failedEvent.getReason());
            orderService.handlePaymentFailed(failedEvent.getOrderId(), failedEvent.getReason());
            idempotencyService.markAsProcessed(failedEvent.getEventId(), "PaymentFailedEvent", CONSUMER_NAME);
        } else {
            log.warn("Unknown event received on payment response queue: {}", event);
        }
    }
}
