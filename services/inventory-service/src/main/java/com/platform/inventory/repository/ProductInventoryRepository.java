package com.platform.inventory.repository;

import com.platform.inventory.entity.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {

    Optional<ProductInventory> findBySku(String sku);

    @Modifying
    @Query("UPDATE ProductInventory p SET p.availableQuantity = p.availableQuantity - :quantity, " +
           "p.reservedQuantity = p.reservedQuantity + :quantity " +
           "WHERE p.productId = :productId AND p.availableQuantity >= :quantity")
    int reserveStock(@Param("productId") UUID productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ProductInventory p SET p.availableQuantity = p.availableQuantity + :quantity, " +
           "p.reservedQuantity = p.reservedQuantity - :quantity " +
           "WHERE p.productId = :productId AND p.reservedQuantity >= :quantity")
    int releaseReservedStock(@Param("productId") UUID productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ProductInventory p SET p.reservedQuantity = p.reservedQuantity - :quantity " +
           "WHERE p.productId = :productId AND p.reservedQuantity >= :quantity")
    int commitReservedStock(@Param("productId") UUID productId, @Param("quantity") int quantity);
}
