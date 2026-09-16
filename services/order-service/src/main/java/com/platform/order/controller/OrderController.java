package com.platform.order.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.order.dto.CreateOrderRequest;
import com.platform.order.dto.OrderResponse;
import com.platform.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for creating, managing, and tracking orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Validates products, initializes order in PENDING status, and initiates the distributed Saga")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received request to create order for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Order created successfully. Saga workflow initiated."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details including current status and item list")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable("id") UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer ID", description = "Lists order history for a specific customer")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomer(@PathVariable("customerId") UUID customerId) {
        List<OrderResponse> responses = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels an order and emits compensation events to release reserved stock")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable("id") UUID id,
            @RequestParam(name = "reason", defaultValue = "Cancelled by user") String reason) {
        log.info("Received cancellation request for order: {}", id);
        OrderResponse response = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order cancellation requested"));
    }
}
