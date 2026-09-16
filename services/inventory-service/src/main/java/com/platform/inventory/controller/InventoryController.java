package com.platform.inventory.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.inventory.entity.ProductInventory;
import com.platform.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Endpoints for querying stock and managing product inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get product inventory", description = "Fetches available stock with Redis caching")
    public ResponseEntity<ApiResponse<ProductInventory>> getProductInventory(@PathVariable("productId") UUID productId) {
        ProductInventory inventory = inventoryService.getProductInventory(productId);
        return ResponseEntity.ok(ApiResponse.ok(inventory));
    }

    @GetMapping
    @Operation(summary = "List all inventory items", description = "Lists all products and their stock levels")
    public ResponseEntity<ApiResponse<List<ProductInventory>>> getAllProducts() {
        List<ProductInventory> products = inventoryService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @PostMapping
    @Operation(summary = "Create or restock a product", description = "Admin endpoint to add new product stock or update inventory")
    public ResponseEntity<ApiResponse<ProductInventory>> saveProduct(@RequestBody ProductInventory product) {
        log.info("Saving product inventory: {}", product.getName());
        ProductInventory saved = inventoryService.saveProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Product inventory updated successfully"));
    }
}
