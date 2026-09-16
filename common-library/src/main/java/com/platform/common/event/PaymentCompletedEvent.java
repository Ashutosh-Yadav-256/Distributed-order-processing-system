package com.platform.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent implements Serializable {
    private UUID eventId;
    private UUID orderId;
    private UUID paymentId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private Instant completedAt;
}
