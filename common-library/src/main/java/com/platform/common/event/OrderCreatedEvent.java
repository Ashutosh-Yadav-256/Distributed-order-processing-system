package com.platform.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private UUID eventId;
    private UUID orderId;
    private UUID customerId;
    private String customerEmail;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentMethod;
    private Instant createdAt;
}
