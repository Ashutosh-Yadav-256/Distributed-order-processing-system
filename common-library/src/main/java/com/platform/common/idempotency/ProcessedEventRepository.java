package com.platform.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);
    Optional<ProcessedEvent> findByEventIdAndConsumerName(UUID eventId, String consumerName);
}
