package com.platform.order.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.OrderConfirmedEvent;
import com.platform.common.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_EXCHANGE,
                RabbitMQConstants.ORDER_CREATED_KEY,
                event
        );
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent for orderId: {}, compensationRequired: {}",
                event.getOrderId(), event.isCompensationRequired());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_EXCHANGE,
                RabbitMQConstants.ORDER_CANCELLED_KEY,
                event
        );
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publishing OrderConfirmedEvent for orderId: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ORDER_EXCHANGE,
                RabbitMQConstants.ORDER_CONFIRMED_KEY,
                event
        );
    }
}
