package com.platform.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.platform")
@EntityScan(basePackages = {"com.platform.notification.entity", "com.platform.common.idempotency"})
@EnableJpaRepositories(basePackages = {"com.platform.notification.repository", "com.platform.common.idempotency"})
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
