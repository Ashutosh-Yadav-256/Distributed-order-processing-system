package com.platform.gateway.controller;

import com.platform.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();
    }

    @Test
    @DisplayName("GET /health: returns UP status and api-gateway service identifier")
    void shouldReturnHealthyStatus() {
        Mono<ApiResponse<Map<String, Object>>> responseMono = healthController.checkHealth();

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData()).isNotNull();
                    assertThat(response.getData().get("status")).isEqualTo("UP");
                    assertThat(response.getData().get("service")).isEqualTo("api-gateway");
                    assertThat(response.getData().get("timestamp")).isNotNull();
                })
                .verifyComplete();
    }
}
