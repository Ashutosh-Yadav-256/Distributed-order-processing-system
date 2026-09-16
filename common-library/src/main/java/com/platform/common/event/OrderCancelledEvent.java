package com.platform.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements Serializable {
    private UUID eventId;
    private UUID orderId;
    private UUID customerId;
    private String reason;
    private boolean compensationRequired;
    private List<OrderItemDto> items;
    private Instant cancelledAt;
}
