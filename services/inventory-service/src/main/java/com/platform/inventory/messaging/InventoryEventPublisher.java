package com.platform.inventory.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.event.InventoryReservationFailedEvent;
import com.platform.common.event.InventoryReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishInventoryReserved(InventoryReservedEvent event) {
        log.info("Publishing InventoryReservedEvent for orderId: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.INVENTORY_EXCHANGE,
                RabbitMQConstants.INVENTORY_RESERVED_KEY,
                event
        );
    }

    public void publishInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.warn("Publishing InventoryReservationFailedEvent for orderId: {}, reason: {}",
                event.getOrderId(), event.getReason());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.INVENTORY_EXCHANGE,
                RabbitMQConstants.INVENTORY_FAILED_KEY,
                event
        );
    }
}
