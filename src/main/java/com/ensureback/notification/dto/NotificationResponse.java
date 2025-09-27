package com.ensureback.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventType,
        JsonNode payload,
        boolean delivered,
        OffsetDateTime deliveredAt,
        OffsetDateTime createdAt
) {
}
