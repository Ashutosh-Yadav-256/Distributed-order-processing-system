package com.platform.payment.service;

import com.platform.common.enums.PaymentStatus;
import com.platform.common.event.InventoryReservedEvent;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.PaymentCompletedEvent;
import com.platform.common.event.PaymentFailedEvent;
import com.platform.payment.entity.Payment;
import com.platform.payment.messaging.PaymentEventPublisher;
import com.platform.payment.repository.PaymentRepository;
import com.platform.payment.simulator.PaymentProcessorSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessorSimulator paymentSimulator;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("150.00"))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .reservedAt(Instant.now())
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentSimulator.process(any(), any(), any()))
                .thenReturn(PaymentProcessorSimulator.ProcessingResult.builder()
                        .success(true)
                        .transactionId("txn_test_123")
                        .build());

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        Payment payment = paymentService.processPayment(event);

        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("txn_test_123");

        verify(eventPublisher, times(1)).publishPaymentCompleted(any(PaymentCompletedEvent.class));
        verify(eventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    void shouldHandlePaymentFailure() {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("999.99"))
                .currency("USD")
                .paymentMethod("CREDIT_CARD_FAIL")
                .reservedAt(Instant.now())
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentSimulator.process(any(), any(), any()))
                .thenReturn(PaymentProcessorSimulator.ProcessingResult.builder()
                        .success(false)
                        .errorMessage("Card declined: Insufficient funds")
                        .build());

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        Payment payment = paymentService.processPayment(event);

        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getErrorMessage()).contains("Card declined");

        verify(eventPublisher, times(1)).publishPaymentFailed(any(PaymentFailedEvent.class));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void shouldRefundPaymentOnOrderCancelled() {
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .transactionId("txn_test_456")
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .reason("Customer cancellation")
                .build();

        paymentService.handleOrderCancelled(event);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
