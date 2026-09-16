package com.platform.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.platform")
@EntityScan(basePackages = {"com.platform.inventory.entity", "com.platform.common.idempotency"})
@EnableJpaRepositories(basePackages = {"com.platform.inventory.repository", "com.platform.common.idempotency"})
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
