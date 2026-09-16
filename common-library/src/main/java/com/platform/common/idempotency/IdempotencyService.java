package com.platform.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(UUID eventId, String consumerName) {
        boolean processed = processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName);
        if (processed) {
            log.warn("Event {} has already been processed by consumer {}. Skipping duplicate message.", eventId, consumerName);
        }
        return processed;
    }

    @Transactional
    public void markAsProcessed(UUID eventId, String eventType, String consumerName) {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .consumerName(consumerName)
                .processedAt(Instant.now())
                .build();
        processedEventRepository.save(event);
        log.info("Marked event {} ({}) as processed by {}", eventId, eventType, consumerName);
    }
}
