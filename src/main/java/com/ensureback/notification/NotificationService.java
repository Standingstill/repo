package com.ensureback.notification;

import com.ensureback.notification.dto.NotificationDto;
import com.ensureback.notification.dto.NotificationPublishRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {

    List<NotificationDto> listByRecipient(String recipientEmail);

    List<NotificationDto> listByUser(String userId);

    Optional<NotificationDto> findById(UUID notificationId);

    Optional<NotificationDto> updateDeliveryStatus(UUID notificationId, boolean delivered);

    long countUnread(String userId);

    NotificationDto publish(NotificationPublishRequest request);
}