package com.platform.notification.messaging;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.enums.NotificationChannel;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.OrderConfirmedEvent;
import com.platform.common.event.OrderCreatedEvent;
import com.platform.common.event.PaymentFailedEvent;
import com.platform.common.idempotency.IdempotencyService;
import com.platform.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;
    private final com.platform.notification.service.S3InvoiceArchiverService s3InvoiceArchiverService;

    private static final String CONSUMER_NAME = "NotificationEventConsumer";

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_EVENTS_QUEUE)
    public void handleNotificationEvents(Object event) {
        if (event instanceof OrderCreatedEvent orderCreated) {
            if (idempotencyService.isAlreadyProcessed(orderCreated.getEventId(), CONSUMER_NAME)) {
                return;
            }

            notificationService.sendNotification(
                    orderCreated.getOrderId(),
                    orderCreated.getCustomerId(),
                    orderCreated.getCustomerEmail() != null ? orderCreated.getCustomerEmail() : "customer@example.com",
                    NotificationChannel.EMAIL,
                    "Order Received: #" + orderCreated.getOrderId(),
                    "Thank you for your order! We are reserving inventory and preparing your shipment."
            );
            idempotencyService.markAsProcessed(orderCreated.getEventId(), "OrderCreatedEvent", CONSUMER_NAME);

        } else if (event instanceof OrderConfirmedEvent orderConfirmed) {
            if (idempotencyService.isAlreadyProcessed(orderConfirmed.getEventId(), CONSUMER_NAME)) {
                return;
            }

            String customerEmail = orderConfirmed.getCustomerEmail() != null ? orderConfirmed.getCustomerEmail() : "customer@example.com";
            notificationService.sendNotification(
                    orderConfirmed.getOrderId(),
                    orderConfirmed.getCustomerId(),
                    customerEmail,
                    NotificationChannel.EMAIL,
                    "Order Confirmed & Payment Succeeded: #" + orderConfirmed.getOrderId(),
                    String.format("Payment of %s %s was authorized! Your order has been officially confirmed and will ship soon.",
                            orderConfirmed.getTotalAmount(), orderConfirmed.getCurrency())
            );

            try {
                s3InvoiceArchiverService.archiveInvoice(
                        orderConfirmed.getOrderId() != null ? orderConfirmed.getOrderId().toString() : "",
                        orderConfirmed.getCustomerId() != null ? orderConfirmed.getCustomerId().toString() : "",
                        customerEmail,
                        orderConfirmed.getTotalAmount(),
                        orderConfirmed.getCurrency()
                );
            } catch (Exception e) {
                log.warn("Non-blocking S3 archival exception: {}", e.getMessage());
            }

            idempotencyService.markAsProcessed(orderConfirmed.getEventId(), "OrderConfirmedEvent", CONSUMER_NAME);

        } else if (event instanceof OrderCancelledEvent orderCancelled) {

            if (idempotencyService.isAlreadyProcessed(orderCancelled.getEventId(), CONSUMER_NAME)) {
                return;
            }

            notificationService.sendNotification(
                    orderCancelled.getOrderId(),
                    orderCancelled.getCustomerId(),
                    "customer@example.com",
                    NotificationChannel.EMAIL,
                    "Order Cancelled: #" + orderCancelled.getOrderId(),
                    "Your order has been cancelled. Reason: " + orderCancelled.getReason()
            );
            idempotencyService.markAsProcessed(orderCancelled.getEventId(), "OrderCancelledEvent", CONSUMER_NAME);

        } else if (event instanceof PaymentFailedEvent paymentFailed) {
            if (idempotencyService.isAlreadyProcessed(paymentFailed.getEventId(), CONSUMER_NAME)) {
                return;
            }

            notificationService.sendNotification(
                    paymentFailed.getOrderId(),
                    paymentFailed.getOrderId(),
                    "customer@example.com",
                    NotificationChannel.EMAIL,
                    "Payment Alert for Order #" + paymentFailed.getOrderId(),
                    "Payment authorization failed for your order. Reason: " + paymentFailed.getReason()
            );
            idempotencyService.markAsProcessed(paymentFailed.getEventId(), "PaymentFailedEvent", CONSUMER_NAME);
        } else {
            log.warn("Unknown event type received on notification queue: {}", event);
        }
    }
}
