package com.platform.payment.simulator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class PaymentProcessorSimulator {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingResult {
        private boolean success;
        private String transactionId;
        private String errorMessage;
    }

    public ProcessingResult process(BigDecimal amount, String currency, String paymentMethod) {
        log.info("Simulating payment processing for amount: {} {}, method: {}", amount, currency, paymentMethod);

        // Deterministic failure triggers for automated test suites
        if (paymentMethod != null && paymentMethod.toUpperCase().contains("FAIL")) {
            log.warn("Simulated payment failed due to FAIL flag in payment method: {}", paymentMethod);
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Payment declined: simulated failure token encountered")
                    .build();
        }

        if (amount != null && amount.compareTo(new BigDecimal("999.99")) == 0) {
            log.warn("Simulated payment failed for magic amount: 999.99");
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Card declined: Insufficient funds (Simulated)")
                    .build();
        }

        // Simulate successful charge
        String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Simulated payment SUCCESS. Generated transactionId: {}", transactionId);

        return ProcessingResult.builder()
                .success(true)
                .transactionId(transactionId)
                .build();
    }
}
