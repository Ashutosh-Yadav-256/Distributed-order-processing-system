package com.platform.order.client;

import com.platform.common.dto.ApiResponse;
import com.platform.order.dto.PaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumer client for inter-service communication with payment-service.
 * Validated via Pact consumer-driven contract testing.
 */
@Component
public class PaymentClient {

    private final RestClient restClient;

    @Autowired
    public PaymentClient(@Value("${services.payment.url:http://localhost:8083}") String paymentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }

    public PaymentClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ApiResponse<PaymentDto> getPaymentByOrderId(UUID orderId) {
        return restClient.get()
                .uri("/api/v1/payments/order/{orderId}", orderId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<PaymentDto>>() {});
    }

    public ApiResponse<Map<String, Object>> simulatePayment(BigDecimal amount, String currency, String paymentMethod) {
        return restClient.post()
                .uri("/api/v1/payments/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "amount", amount,
                        "currency", currency,
                        "paymentMethod", paymentMethod
                ))
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
    }
}
