package com.platform.payment.service;

import com.platform.common.enums.PaymentStatus;
import com.platform.common.event.InventoryReservedEvent;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.PaymentCompletedEvent;
import com.platform.common.event.PaymentFailedEvent;
import com.platform.common.exception.ResourceNotFoundException;
import com.platform.payment.entity.Payment;
import com.platform.payment.messaging.PaymentEventPublisher;
import com.platform.payment.repository.PaymentRepository;
import com.platform.payment.simulator.PaymentProcessorSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessorSimulator paymentSimulator;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public Payment processPayment(InventoryReservedEvent event) {
        UUID orderId = event.getOrderId();
        log.info("Processing payment for orderId: {}, amount: {} {}", orderId, event.getTotalAmount(), event.getCurrency());

        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            log.warn("Payment record already exists for orderId: {}. Status: {}", orderId, existingPayment.get().getStatus());
            return existingPayment.get();
        }

        PaymentProcessorSimulator.ProcessingResult result = paymentSimulator.process(
                event.getTotalAmount(),
                event.getCurrency(),
                event.getPaymentMethod()
        );

        if (result.isSuccess()) {
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .transactionId(result.getTransactionId())
                    .amount(event.getTotalAmount())
                    .currency(event.getCurrency())
                    .status(PaymentStatus.SUCCESS)
                    .paymentMethod(event.getPaymentMethod() != null ? event.getPaymentMethod() : "CREDIT_CARD")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment saved successfully for orderId: {}, txnId: {}", orderId, result.getTransactionId());

            PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(orderId)
                    .paymentId(savedPayment.getId())
                    .transactionId(savedPayment.getTransactionId())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .paymentMethod(savedPayment.getPaymentMethod())
                    .completedAt(Instant.now())
                    .build();

            eventPublisher.publishPaymentCompleted(completedEvent);
            return savedPayment;

        } else {
            Payment failedPayment = Payment.builder()
                    .orderId(orderId)
                    .transactionId("failed_" + UUID.randomUUID())
                    .amount(event.getTotalAmount())
                    .currency(event.getCurrency())
                    .status(PaymentStatus.FAILED)
                    .paymentMethod(event.getPaymentMethod() != null ? event.getPaymentMethod() : "CREDIT_CARD")
                    .errorMessage(result.getErrorMessage())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Payment savedFailedPayment = paymentRepository.save(failedPayment);
            log.warn("Payment FAILED for orderId: {}, reason: {}", orderId, result.getErrorMessage());

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(orderId)
                    .amount(event.getTotalAmount())
                    .reason(result.getErrorMessage())
                    .failedAt(Instant.now())
                    .build();

            eventPublisher.publishPaymentFailed(failedEvent);
            return savedFailedPayment;
        }
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        UUID orderId = event.getOrderId();
        log.info("Handling order cancellation for payment on orderId: {}", orderId);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setErrorMessage("Refunded due to order cancellation: " + event.getReason());
                paymentRepository.save(payment);
                log.info("Payment for orderId: {} has been marked as REFUNDED", orderId);
            }
        });
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for orderId: " + orderId));
    }
}
