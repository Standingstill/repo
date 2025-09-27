package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationChecklistDto(
        UUID merchantId,
        boolean connectedStripe,
        boolean webhookConfigured,
        boolean aftershipConfigured,
        boolean testChargeDone,
        OffsetDateTime lastCheckedAt
) {
}