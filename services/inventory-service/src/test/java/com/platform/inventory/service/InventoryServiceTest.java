package com.platform.inventory.service;

import com.platform.common.enums.ReservationStatus;
import com.platform.common.event.*;
import com.platform.inventory.cache.InventoryCacheService;
import com.platform.inventory.entity.StockReservation;
import com.platform.inventory.messaging.InventoryEventPublisher;
import com.platform.inventory.repository.ProductInventoryRepository;
import com.platform.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductInventoryRepository productRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private InventoryCacheService cacheService;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID orderId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void shouldReserveStockSuccessfully() {
        OrderItemDto item = OrderItemDto.builder()
                .productId(productId)
                .productName("Mechanical Keyboard")
                .quantity(3)
                .unitPrice(new BigDecimal("120.00"))
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .items(List.of(item))
                .totalAmount(new BigDecimal("360.00"))
                .currency("USD")
                .createdAt(Instant.now())
                .build();

        when(productRepository.reserveStock(eq(productId), eq(3))).thenReturn(1);

        boolean result = inventoryService.reserveStock(event);

        assertThat(result).isTrue();
        verify(reservationRepository, times(1)).save(any(StockReservation.class));
        verify(cacheService, times(1)).evict(productId);
        verify(eventPublisher, times(1)).publishInventoryReserved(any(InventoryReservedEvent.class));
    }

    @Test
    void shouldRollbackAndPublishFailureWhenOutOfStock() {
        OrderItemDto item = OrderItemDto.builder()
                .productId(productId)
                .productName("Mechanical Keyboard")
                .quantity(50)
                .unitPrice(new BigDecimal("120.00"))
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .items(List.of(item))
                .totalAmount(new BigDecimal("6000.00"))
                .currency("USD")
                .createdAt(Instant.now())
                .build();

        // Simulate zero rows updated (insufficient stock)
        when(productRepository.reserveStock(eq(productId), eq(50))).thenReturn(0);

        boolean result = inventoryService.reserveStock(event);

        assertThat(result).isFalse();
        verify(eventPublisher, times(1)).publishInventoryReservationFailed(any(InventoryReservationFailedEvent.class));
        verify(eventPublisher, never()).publishInventoryReserved(any());
    }

    @Test
    void shouldReleaseStockOnCompensation() {
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId)
                .quantity(2)
                .status(ReservationStatus.RESERVED)
                .build();

        when(reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .compensationRequired(true)
                .build();

        inventoryService.releaseStock(event);

        verify(productRepository, times(1)).releaseReservedStock(productId, 2);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(cacheService, times(1)).evict(productId);
    }
}
