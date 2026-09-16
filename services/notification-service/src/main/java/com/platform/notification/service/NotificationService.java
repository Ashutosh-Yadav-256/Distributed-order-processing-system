package com.platform.notification.service;

import com.platform.common.enums.NotificationChannel;
import com.platform.common.enums.NotificationStatus;
import com.platform.notification.entity.NotificationLog;
import com.platform.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationRepository;

    @Transactional
    public NotificationLog sendNotification(
            UUID orderId,
            UUID customerId,
            String recipient,
            NotificationChannel channel,
            String subject,
            String content) {

        log.info("""
                ============================================================
                [NOTIFICATION DISPATCH - {}]
                To: {}
                Order: {} | Customer: {}
                Subject: {}
                Body:
                {}
                ============================================================""",
                channel, recipient, orderId, customerId, subject, content);

        NotificationLog notification = NotificationLog.builder()
                .orderId(orderId)
                .customerId(customerId)
                .recipient(recipient)
                .channel(channel)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.SENT)
                .sentAt(Instant.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findAllByOrderIdOrderBySentAtDesc(orderId);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsByCustomerId(UUID customerId) {
        return notificationRepository.findAllByCustomerIdOrderBySentAtDesc(customerId);
    }
}
