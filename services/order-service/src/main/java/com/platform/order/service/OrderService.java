package com.platform.order.service;

import com.platform.common.enums.OrderStatus;
import com.platform.common.event.OrderCancelledEvent;
import com.platform.common.event.OrderConfirmedEvent;
import com.platform.common.event.OrderCreatedEvent;
import com.platform.common.event.OrderItemDto;
import com.platform.common.exception.ResourceNotFoundException;
import com.platform.order.dto.CreateOrderRequest;
import com.platform.order.dto.OrderResponse;
import com.platform.order.entity.Order;
import com.platform.order.entity.OrderItem;
import com.platform.order.messaging.OrderEventPublisher;
import com.platform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        for (var itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .subtotal(itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                    .build();
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} created with status PENDING. Total: {} {}", savedOrder.getId(), totalAmount, savedOrder.getCurrency());

        List<OrderItemDto> itemDtos = savedOrder.getItems().stream()
                .map(i -> OrderItemDto.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .customerEmail(savedOrder.getCustomerEmail())
                .items(itemDtos)
                .totalAmount(savedOrder.getTotalAmount())
                .currency(savedOrder.getCurrency())
                .paymentMethod(savedOrder.getPaymentMethod())
                .createdAt(Instant.now())
                .build();

        eventPublisher.publishOrderCreated(event);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Cannot cancel order {} currently in state {}", orderId, order.getStatus());
            return OrderResponse.fromEntity(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason(reason);
        Order updatedOrder = orderRepository.save(order);

        List<OrderItemDto> itemDtos = updatedOrder.getItems().stream()
                .map(i -> OrderItemDto.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(updatedOrder.getId())
                .customerId(updatedOrder.getCustomerId())
                .reason(reason)
                .compensationRequired(true)
                .items(itemDtos)
                .cancelledAt(Instant.now())
                .build();

        eventPublisher.publishOrderCancelled(event);

        return OrderResponse.fromEntity(updatedOrder);
    }

    @Transactional
    public void handleInventoryReserved(UUID orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                orderRepository.save(order);
                log.info("Order {} transitioned to INVENTORY_RESERVED", orderId);
            }
        });
    }

    @Transactional
    public void handleInventoryReservationFailed(UUID orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("Inventory reservation failed: " + reason);
            orderRepository.save(order);
            log.warn("Order {} transitioned to FAILED due to out of stock", orderId);
        });
    }

    @Transactional
    public void handlePaymentCompleted(UUID orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            Order confirmedOrder = orderRepository.save(order);
            log.info("Order {} transitioned to CONFIRMED. Publishing OrderConfirmedEvent.", orderId);

            List<OrderItemDto> itemDtos = confirmedOrder.getItems().stream()
                    .map(i -> OrderItemDto.builder()
                            .productId(i.getProductId())
                            .productName(i.getProductName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .build())
                    .collect(Collectors.toList());

            OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(confirmedOrder.getId())
                    .customerId(confirmedOrder.getCustomerId())
                    .customerEmail(confirmedOrder.getCustomerEmail())
                    .items(itemDtos)
                    .totalAmount(confirmedOrder.getTotalAmount())
                    .currency(confirmedOrder.getCurrency())
                    .confirmedAt(Instant.now())
                    .build();

            eventPublisher.publishOrderConfirmed(event);
        });
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setFailureReason("Payment failed: " + reason);
            Order cancelledOrder = orderRepository.save(order);
            log.warn("Order {} transitioned to CANCELLED due to payment failure. Triggering compensation.", orderId);

            List<OrderItemDto> itemDtos = cancelledOrder.getItems().stream()
                    .map(i -> OrderItemDto.builder()
                            .productId(i.getProductId())
                            .productName(i.getProductName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .build())
                    .collect(Collectors.toList());

            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(cancelledOrder.getId())
                    .customerId(cancelledOrder.getCustomerId())
                    .reason("Payment failure: " + reason)
                    .compensationRequired(true)
                    .items(itemDtos)
                    .cancelledAt(Instant.now())
                    .build();

            eventPublisher.publishOrderCancelled(event);
        });
    }
}
