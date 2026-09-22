package com.platform.inventory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.inventory.InventoryApplication;
import com.platform.inventory.entity.ProductInventory;
import com.platform.inventory.repository.ProductInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = InventoryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductInventoryRepository productRepository;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End: POST /api/v1/inventory saves product and GET /api/v1/inventory/{id} reads it back")
    void shouldCreateProductAndRetrieveItSuccessfully() throws Exception {
        ProductInventory newProduct = ProductInventory.builder()
                .sku("KEYBOARD-PRO-RGB")
                .name("RGB Mechanical Gaming Keyboard")
                .unitPrice(new BigDecimal("149.99"))
                .availableQuantity(250)
                .reservedQuantity(0)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productId", notNullValue()))
                .andExpect(jsonPath("$.data.sku", is("KEYBOARD-PRO-RGB")))
                .andExpect(jsonPath("$.data.availableQuantity", is(250)))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID productId = UUID.fromString(rootNode.path("data").path("productId").asText());

        assertThat(productRepository.findById(productId)).isPresent();
        assertThat(productRepository.findById(productId).get().getName()).isEqualTo("RGB Mechanical Gaming Keyboard");

        mockMvc.perform(get("/api/v1/inventory/{productId}", productId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productId", is(productId.toString())))
                .andExpect(jsonPath("$.data.name", is("RGB Mechanical Gaming Keyboard")))
                .andExpect(jsonPath("$.data.availableQuantity", is(250)))
                .andExpect(jsonPath("$.data.unitPrice", is(149.99)));
    }

    @Test
    @DisplayName("GET /api/v1/inventory returns all products in catalog")
    void shouldReturnAllProducts() throws Exception {
        ProductInventory p1 = ProductInventory.builder()
                .productId(UUID.randomUUID())
                .sku("SKU-1")
                .name("Item One")
                .unitPrice(new BigDecimal("10.00"))
                .availableQuantity(100)
                .reservedQuantity(0)
                .build();
        ProductInventory p2 = ProductInventory.builder()
                .productId(UUID.randomUUID())
                .sku("SKU-2")
                .name("Item Two")
                .unitPrice(new BigDecimal("20.00"))
                .availableQuantity(200)
                .reservedQuantity(0)
                .build();

        productRepository.save(p1);
        productRepository.save(p2);

        mockMvc.perform(get("/api/v1/inventory")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{productId} returns 404 when product is missing")
    void shouldReturn404WhenProductNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/inventory/{productId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Product inventory not found")));
    }
}
