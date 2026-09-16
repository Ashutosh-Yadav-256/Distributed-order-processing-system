package com.platform.inventory.service;

import com.platform.common.enums.ReservationStatus;
import com.platform.common.event.*;
import com.platform.common.exception.ResourceNotFoundException;
import com.platform.inventory.cache.InventoryCacheService;
import com.platform.inventory.entity.ProductInventory;
import com.platform.inventory.entity.StockReservation;
import com.platform.inventory.messaging.InventoryEventPublisher;
import com.platform.inventory.repository.ProductInventoryRepository;
import com.platform.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductInventoryRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryCacheService cacheService;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    public boolean reserveStock(OrderCreatedEvent event) {
        UUID orderId = event.getOrderId();
        log.info("Attempting to reserve inventory for orderId: {} with {} items", orderId, event.getItems().size());

        List<StockReservation> successfulReservations = new ArrayList<>();

        for (OrderItemDto item : event.getItems()) {
            int updated = productRepository.reserveStock(item.getProductId(), item.getQuantity());

            if (updated == 0) {
                // Insufficient stock or product missing -> Roll back partial reservations
                log.warn("Insufficient stock for product: {} (requested: {}). Rolling back reservations for order: {}",
                        item.getProductId(), item.getQuantity(), orderId);

                for (StockReservation rollbackItem : successfulReservations) {
                    productRepository.releaseReservedStock(rollbackItem.getProductId(), rollbackItem.getQuantity());
                    reservationRepository.delete(rollbackItem);
                    cacheService.evict(rollbackItem.getProductId());
                }

                // Publish failure event to RabbitMQ
                InventoryReservationFailedEvent failedEvent = InventoryReservationFailedEvent.builder()
                        .eventId(UUID.randomUUID())
                        .orderId(orderId)
                        .customerId(event.getCustomerId())
                        .reason("Out of stock for product: " + item.getProductName())
                        .failedProductId(item.getProductId())
                        .failedAt(Instant.now())
                        .build();
                eventPublisher.publishInventoryReservationFailed(failedEvent);

                return false;
            }

            // Save reservation record
            StockReservation reservation = StockReservation.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            reservationRepository.save(reservation);
            successfulReservations.add(reservation);

            // Invalidate/update cache
            cacheService.evict(item.getProductId());
        }

        log.info("Successfully reserved all items for orderId: {}", orderId);

        // Publish success event to RabbitMQ
        InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(event.getCustomerId())
                .items(event.getItems())
                .totalAmount(event.getTotalAmount())
                .currency(event.getCurrency())
                .paymentMethod(event.getPaymentMethod())
                .reservedAt(Instant.now())
                .build();
        eventPublisher.publishInventoryReserved(reservedEvent);

        return true;
    }

    @Transactional
    public void releaseStock(OrderCancelledEvent event) {
        UUID orderId = event.getOrderId();
        log.info("Executing compensation: Releasing stock for cancelled orderId: {}", orderId);

        List<StockReservation> reservations = reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (StockReservation res : reservations) {
            productRepository.releaseReservedStock(res.getProductId(), res.getQuantity());
            res.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(res);
            cacheService.evict(res.getProductId());
            log.info("Released {} units of product {} for orderId: {}", res.getQuantity(), res.getProductId(), orderId);
        }
    }

    @Transactional
    public void commitStock(UUID orderId) {
        log.info("Committing stock reservations for confirmed orderId: {}", orderId);
        List<StockReservation> reservations = reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (StockReservation res : reservations) {
            productRepository.commitReservedStock(res.getProductId(), res.getQuantity());
            res.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(res);
            cacheService.evict(res.getProductId());
        }
    }

    @Transactional(readOnly = true)
    public ProductInventory getProductInventory(UUID productId) {
        // Read-through cache check: return from Redis if present, else fallback to PostgreSQL
        return cacheService.getCachedProduct(productId).orElseGet(() -> {
            ProductInventory inventory = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product inventory not found for id: " + productId));
            cacheService.setCachedProduct(productId, inventory);
            return inventory;
        });
    }

    @Transactional(readOnly = true)
    public List<ProductInventory> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public ProductInventory saveProduct(ProductInventory product) {
        if (product.getProductId() == null) {
            product.setProductId(UUID.randomUUID());
        }
        ProductInventory saved = productRepository.save(product);
        cacheService.setCachedProduct(saved.getProductId(), saved);
        return saved;
    }
}
