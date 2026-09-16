package com.platform.gateway.controller;

import com.platform.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/health", "/api/v1/health", "/healthz"})
    public Mono<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("service", "api-gateway");
        details.put("timestamp", Instant.now().toString());
        return Mono.just(ApiResponse.ok(details, "API Gateway is running and healthy"));
    }
}
