package com.platform.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.enums.OrderStatus;
import com.platform.order.OrderApplication;
import com.platform.order.dto.CreateOrderRequest;
import com.platform.order.dto.OrderItemRequest;
import com.platform.order.messaging.OrderEventPublisher;
import com.platform.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = OrderApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End: POST /api/v1/orders saves row to DB and GET /api/v1/orders/{id} reads it back")
    void shouldCreateOrderAndRetrieveItSuccessfully() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemRequest item = OrderItemRequest.builder()
                .productId(productId)
                .productName("Mechanical Keyboard Pro")
                .quantity(2)
                .unitPrice(new BigDecimal("129.99"))
                .build();

        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .customerId(customerId)
                .customerEmail("engineer@platform.com")
                .items(List.of(item))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.customerId", is(customerId.toString())))
                .andExpect(jsonPath("$.data.status", is(OrderStatus.PENDING.name())))
                .andExpect(jsonPath("$.data.totalAmount", is(259.98)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName", is("Mechanical Keyboard Pro")))
                .andReturn();

        verify(orderEventPublisher, times(1)).publishOrderCreated(any());

        JsonNode rootNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String orderIdStr = rootNode.path("data").path("id").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(orderRepository.findById(orderId).get().getItems()).hasSize(1);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(orderId.toString())))
                .andExpect(jsonPath("$.data.customerId", is(customerId.toString())))
                .andExpect(jsonPath("$.data.customerEmail", is("engineer@platform.com")))
                .andExpect(jsonPath("$.data.status", is(OrderStatus.PENDING.name())))
                .andExpect(jsonPath("$.data.totalAmount", is(259.98)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} returns 404 when order does not exist")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/orders/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Order not found")));
    }
}
