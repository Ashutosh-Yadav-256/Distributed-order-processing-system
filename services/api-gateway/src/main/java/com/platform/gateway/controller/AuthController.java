package com.platform.gateway.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.gateway.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequest {
        private UUID userId;
        private String email;
        private List<String> roles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String token;
        private String tokenType;
        private UUID userId;
        private String email;
    }

    @PostMapping("/token")
    public Mono<ApiResponse<TokenResponse>> generateToken(@RequestBody(required = false) TokenRequest request) {
        UUID userId = (request != null && request.getUserId() != null) ? request.getUserId() : UUID.randomUUID();
        String email = (request != null && request.getEmail() != null) ? request.getEmail() : "customer@example.com";
        List<String> roles = (request != null && request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles()
                : List.of("ROLE_USER");

        String token = jwtTokenProvider.generateToken(userId, email, roles);

        TokenResponse response = TokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userId)
                .email(email)
                .build();

        return Mono.just(ApiResponse.ok(response, "JWT token generated successfully"));
    }

    @GetMapping("/health")
    public Mono<ApiResponse<String>> healthCheck() {
        return Mono.just(ApiResponse.ok("API Gateway is running and healthy"));
    }
}
