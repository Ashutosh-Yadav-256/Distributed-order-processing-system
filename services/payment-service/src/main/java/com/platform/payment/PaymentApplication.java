package com.platform.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.platform")
@EntityScan(basePackages = {"com.platform.payment.entity", "com.platform.common.idempotency"})
@EnableJpaRepositories(basePackages = {"com.platform.payment.repository", "com.platform.common.idempotency"})
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
