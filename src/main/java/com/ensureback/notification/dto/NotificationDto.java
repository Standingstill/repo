package com.ensureback.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

public record NotificationDto(
        String notificationId,
        String eventType,
        JsonNode payload,
        boolean delivered,
        OffsetDateTime deliveredAt,
        OffsetDateTime createdAt
) {
}