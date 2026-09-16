package com.platform.order.repository;

import com.platform.common.enums.OrderStatus;
import com.platform.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<Order> findAllByStatus(OrderStatus status);
}
