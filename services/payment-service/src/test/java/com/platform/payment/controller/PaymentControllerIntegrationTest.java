package com.platform.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.enums.PaymentStatus;
import com.platform.payment.PaymentApplication;
import com.platform.payment.entity.Payment;
import com.platform.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PaymentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/payments/simulate: returns success and transactionId for valid payment")
    void shouldSimulateSuccessfulPayment() throws Exception {
        PaymentController.SimulatePaymentRequest request = PaymentController.SimulatePaymentRequest.builder()
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        mockMvc.perform(post("/api/v1/payments/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.success", is(true)))
                .andExpect(jsonPath("$.data.transactionId", startsWith("txn_")));
    }

    @Test
    @DisplayName("POST /api/v1/payments/simulate: simulates card decline on FAIL payment method token")
    void shouldSimulatePaymentDeclineOnFailToken() throws Exception {
        PaymentController.SimulatePaymentRequest request = PaymentController.SimulatePaymentRequest.builder()
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .paymentMethod("CARD_FAIL_DECLINED")
                .build();

        mockMvc.perform(post("/api/v1/payments/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success", is(false)))
                .andExpect(jsonPath("$.data.errorMessage", containsString("Payment declined")));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId}: reads back persisted payment record")
    void shouldRetrievePaymentByOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(new BigDecimal("99.50"))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn_test_abc123")
                .build();

        paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is(orderId.toString())))
                .andExpect(jsonPath("$.data.status", is(PaymentStatus.SUCCESS.name())))
                .andExpect(jsonPath("$.data.transactionId", is("txn_test_abc123")))
                .andExpect(jsonPath("$.data.amount", is(99.50)));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId}: returns 404 when payment record does not exist")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        UUID randomOrderId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", randomOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Payment not found")));
    }
}
