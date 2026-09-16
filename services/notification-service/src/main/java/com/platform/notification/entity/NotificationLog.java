package com.platform.notification.entity;

import com.platform.common.enums.NotificationChannel;
import com.platform.common.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notifications_order_id", columnList = "orderId"),
        @Index(name = "idx_notifications_customer_id", columnList = "customerId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.SENT;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();
}
