package com.ensureback.developer.dto;

import java.time.OffsetDateTime;

public record IntegrationChecklistUpdateRequest(
        Boolean connectedStripe,
        Boolean webhookConfigured,
        Boolean aftershipConfigured,
        Boolean testChargeDone,
        OffsetDateTime lastCheckedAt
) {
}