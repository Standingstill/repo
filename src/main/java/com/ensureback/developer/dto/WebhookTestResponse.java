package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebhookTestResponse(
        UUID eventId,
        boolean delivered,
        OffsetDateTime timestamp
) {
}
