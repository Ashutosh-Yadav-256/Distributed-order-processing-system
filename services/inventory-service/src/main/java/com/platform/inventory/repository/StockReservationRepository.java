package com.platform.inventory.repository;

import com.platform.common.enums.ReservationStatus;
import com.platform.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
    List<StockReservation> findAllByOrderId(UUID orderId);
    List<StockReservation> findAllByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
