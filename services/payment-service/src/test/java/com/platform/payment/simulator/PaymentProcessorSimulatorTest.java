package com.platform.payment.simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProcessorSimulatorTest {

    private PaymentProcessorSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new PaymentProcessorSimulator();
    }

    @Test
    @DisplayName("Should approve valid payment method and non-magic amount")
    void shouldApproveValidPayment() {
        PaymentProcessorSimulator.ProcessingResult result =
                simulator.process(new BigDecimal("49.99"), "USD", "CREDIT_CARD");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isNotNull().startsWith("txn_");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should decline payment when payment method contains 'FAIL'")
    void shouldDeclineWhenPaymentMethodContainsFailToken() {
        PaymentProcessorSimulator.ProcessingResult result =
                simulator.process(new BigDecimal("100.00"), "USD", "CARD_FAIL_DECLINED");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionId()).isNull();
        assertThat(result.getErrorMessage()).contains("simulated failure token");
    }

    @Test
    @DisplayName("Should decline payment when magic amount 999.99 is submitted")
    void shouldDeclineWhenMagicAmountSubmitted() {
        PaymentProcessorSimulator.ProcessingResult result =
                simulator.process(new BigDecimal("999.99"), "USD", "CREDIT_CARD");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionId()).isNull();
        assertThat(result.getErrorMessage()).contains("Insufficient funds");
    }
}
