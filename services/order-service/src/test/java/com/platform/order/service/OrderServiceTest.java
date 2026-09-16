package com.platform.order.service;

import com.platform.common.enums.OrderStatus;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.OrderCreatedEvent;
import com.platform.order.dto.CreateOrderRequest;
import com.platform.order.dto.OrderItemRequest;
import com.platform.order.dto.OrderResponse;
import com.platform.order.entity.Order;
import com.platform.order.messaging.OrderEventPublisher;
import com.platform.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void shouldCreateOrderAndPublishOrderCreatedEvent() {
        OrderItemRequest item = OrderItemRequest.builder()
                .productId(productId)
                .productName("Wireless Headphones")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(customerId)
                .customerEmail("user@example.com")
                .items(List.of(item))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("199.98"));
        assertThat(response.getItems()).hasSize(1);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void shouldCancelOrderAndPublishCompensatingEvent() {
        UUID orderId = UUID.randomUUID();
        Order existingOrder = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .customerEmail("user@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, "Customer requested cancellation");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getFailureReason()).isEqualTo("Customer requested cancellation");

        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishOrderCancelled(captor.capture());
        assertThat(captor.getValue().isCompensationRequired()).isTrue();
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
    }
}
