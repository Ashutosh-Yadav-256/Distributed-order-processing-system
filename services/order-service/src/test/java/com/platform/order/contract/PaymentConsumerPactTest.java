package com.platform.order.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.platform.common.dto.ApiResponse;
import com.platform.order.client.PaymentClient;
import com.platform.order.dto.PaymentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact Consumer Test: order-service (Consumer) -> payment-service (Provider)
 * Formulates inter-service HTTP expectations and outputs target/pacts/order-service-payment-service.json
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "payment-service")
public class PaymentConsumerPactTest {

    private final UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID paymentId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Pact(consumer = "order-service", provider = "payment-service")
    public V4Pact getPaymentByOrderIdPact(PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("a payment exists for order")
                .uponReceiving("a request to retrieve payment status for an order")
                .path("/api/v1/payments/order/" + orderId)
                .method("GET")
                .headers("Accept", "application/json")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(newJsonBody(root -> {
                    root.booleanType("success", true);
                    root.stringType("message", "Operation successful");
                    root.object("data", data -> {
                        data.uuid("id", paymentId);
                        data.uuid("orderId", orderId);
                        data.stringType("transactionId", "TXN-PACT-12345");
                        data.numberType("amount", 199.99);
                        data.stringType("currency", "USD");
                        data.stringType("status", "SUCCESS");
                        data.stringType("paymentMethod", "CREDIT_CARD");
                    });
                }).build())
                .toPact(V4Pact.class);
    }

    @Test
    @DisplayName("Verify order-service client against Pact mock server")
    @PactTestFor(pactMethod = "getPaymentByOrderIdPact")
    void testGetPaymentByOrderId(MockServer mockServer) {
        PaymentClient client = new PaymentClient(RestClient.builder().baseUrl(mockServer.getUrl()).build());
        ApiResponse<PaymentDto> response = client.getPaymentByOrderId(orderId);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getOrderId()).isEqualTo(orderId);
        assertThat(response.getData().getTransactionId()).isEqualTo("TXN-PACT-12345");
        assertThat(response.getData().getStatus().name()).isEqualTo("SUCCESS");
    }
}
