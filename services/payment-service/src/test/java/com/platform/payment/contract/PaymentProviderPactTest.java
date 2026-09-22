package com.platform.payment.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.platform.common.enums.PaymentStatus;
import com.platform.payment.PaymentApplication;
import com.platform.payment.entity.Payment;
import com.platform.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pact Provider Test: payment-service
 * Validates that payment-service satisfies the contract defined by order-service.
 */
@SpringBootTest(classes = PaymentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("payment-service")
@PactFolder("pacts")
@ActiveProfiles("test")
public class PaymentProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setup(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    @DisplayName("Verify interactions in Pact contract")
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @State("a payment exists for order")
    void toPaymentExistsState() {
        UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID paymentId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        paymentRepository.deleteAll();
        paymentRepository.save(Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .transactionId("TXN-PACT-12345")
                .amount(new BigDecimal("199.99"))
                .currency("USD")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod("CREDIT_CARD")
                .build());
    }
}
