package com.platform.inventory.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.inventory.entity.ProductInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "inventory:product:";
    private static final Duration TTL = Duration.ofSeconds(60);

    public Optional<ProductInventory> getCachedProduct(UUID productId) {
        try {
            String key = KEY_PREFIX + productId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Redis HIT for key: {}", key);
                return Optional.of(objectMapper.readValue(json, ProductInventory.class));
            }
            log.debug("Redis MISS for key: {}", key);
        } catch (Exception e) {
            log.warn("Redis read error for product {}: {}", productId, e.getMessage());
        }
        return Optional.empty();
    }

    public void setCachedProduct(UUID productId, ProductInventory product) {
        try {
            String key = KEY_PREFIX + productId;
            String json = objectMapper.writeValueAsString(product);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.debug("Redis SET for key: {} (TTL: {}s)", key, TTL.getSeconds());
        } catch (Exception e) {
            log.warn("Redis write error for product {}: {}", productId, e.getMessage());
        }
    }

    public Optional<Integer> getCachedQuantity(UUID productId) {
        return getCachedProduct(productId).map(ProductInventory::getAvailableQuantity);
    }

    public void setCachedQuantity(UUID productId, int quantity) {
        try {
            String key = KEY_PREFIX + "qty:" + productId;
            redisTemplate.opsForValue().set(key, String.valueOf(quantity), TTL);
        } catch (Exception e) {
            log.warn("Redis qty write error: {}", e.getMessage());
        }
    }

    public void evict(UUID productId) {
        try {
            String key = KEY_PREFIX + productId;
            redisTemplate.delete(key);
            redisTemplate.delete(KEY_PREFIX + "qty:" + productId);
            log.debug("Redis EVICT for key: {}", key);
        } catch (Exception e) {
            log.warn("Redis delete error for product {}: {}", productId, e.getMessage());
        }
    }
}
