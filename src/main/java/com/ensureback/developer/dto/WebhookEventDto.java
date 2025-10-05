package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebhookEventDto(
        UUID id,
        String eventType,
        OffsetDateTime timestamp,
        boolean delivered,
        String payload
) {
}
