package com.platform.payment.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.payment.entity.Payment;
import com.platform.payment.service.PaymentService;
import com.platform.payment.simulator.PaymentProcessorSimulator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for payment tracking and processor simulation")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentProcessorSimulator simulator;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Retrieves payment transaction status for an order")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByOrderId(@PathVariable("orderId") UUID orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(payment));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulatePaymentRequest {
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate payment transaction", description = "Test endpoint to simulate gateway response")
    public ResponseEntity<ApiResponse<PaymentProcessorSimulator.ProcessingResult>> simulatePayment(
            @RequestBody SimulatePaymentRequest request) {
        PaymentProcessorSimulator.ProcessingResult result = simulator.process(
                request.getAmount() != null ? request.getAmount() : new BigDecimal("100.00"),
                request.getCurrency() != null ? request.getCurrency() : "USD",
                request.getPaymentMethod() != null ? request.getPaymentMethod() : "CREDIT_CARD"
        );
        return ResponseEntity.ok(ApiResponse.ok(result, "Simulation executed"));
    }
}
