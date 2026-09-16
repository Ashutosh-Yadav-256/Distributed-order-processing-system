package com.platform.common.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_processed_events_unique", columnList = "eventId, consumerName", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String consumerName;

    @Column(nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();
}
