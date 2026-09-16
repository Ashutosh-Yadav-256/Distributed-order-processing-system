package com.platform.payment.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.event.PaymentCompletedEvent;
import com.platform.common.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent for orderId: {}, paymentId: {}",
                event.getOrderId(), event.getPaymentId());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.PAYMENT_EXCHANGE,
                RabbitMQConstants.PAYMENT_COMPLETED_KEY,
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.warn("Publishing PaymentFailedEvent for orderId: {}, reason: {}",
                event.getOrderId(), event.getReason());
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.PAYMENT_EXCHANGE,
                RabbitMQConstants.PAYMENT_FAILED_KEY,
                event
        );
    }
}
