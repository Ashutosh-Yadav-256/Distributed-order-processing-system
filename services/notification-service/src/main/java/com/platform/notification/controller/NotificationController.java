package com.platform.notification.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.notification.entity.NotificationLog;
import com.platform.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Endpoints for viewing dispatched notifications and delivery audits")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get notifications for an order", description = "Fetches all notification logs (email, SMS) triggered for a specific order")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getNotificationsByOrderId(@PathVariable("orderId") UUID orderId) {
        List<NotificationLog> notifications = notificationService.getNotificationsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get notifications for a customer", description = "Fetches notification history for a specific customer")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getNotificationsByCustomerId(@PathVariable("customerId") UUID customerId) {
        List<NotificationLog> notifications = notificationService.getNotificationsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }
}
