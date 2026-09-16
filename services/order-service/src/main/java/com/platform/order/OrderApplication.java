package com.platform.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.platform")
@EntityScan(basePackages = {"com.platform.order.entity", "com.platform.common.idempotency"})
@EnableJpaRepositories(basePackages = {"com.platform.order.repository", "com.platform.common.idempotency"})
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
