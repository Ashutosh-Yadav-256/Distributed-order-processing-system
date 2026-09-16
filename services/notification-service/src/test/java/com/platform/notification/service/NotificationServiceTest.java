package com.platform.notification.service;

import com.platform.common.enums.NotificationChannel;
import com.platform.common.enums.NotificationStatus;
import com.platform.notification.entity.NotificationLog;
import com.platform.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void shouldSendNotificationAndSaveLog() {
        when(notificationRepository.save(any(NotificationLog.class))).thenAnswer(i -> {
            NotificationLog log = i.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        NotificationLog result = notificationService.sendNotification(
                orderId,
                customerId,
                "user@example.com",
                NotificationChannel.EMAIL,
                "Order Update",
                "Your order has shipped."
        );

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getRecipient()).isEqualTo("user@example.com");
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(notificationRepository, times(1)).save(any(NotificationLog.class));
    }

    @Test
    void shouldFindNotificationsByOrderId() {
        NotificationLog mockLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .recipient("user@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Order Placed")
                .content("Order #123")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findAllByOrderIdOrderBySentAtDesc(orderId))
                .thenReturn(List.of(mockLog));

        List<NotificationLog> logs = notificationService.getNotificationsByOrderId(orderId);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOrderId()).isEqualTo(orderId);
        verify(notificationRepository, times(1)).findAllByOrderIdOrderBySentAtDesc(orderId);
    }
}
