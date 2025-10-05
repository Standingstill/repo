package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IntegrationWizardStatusResponse(
        UUID merchantId,
        IntegrationWizardStepDto stripeConnect,
        IntegrationWizardStepDto apiKey,
        IntegrationWizardStepDto webhook,
        IntegrationWizardStepDto verification,
        boolean complete,
        OffsetDateTime updatedAt,
        StripeStatusDto stripeStatus,
        WebhookStatusDto webhookStatus,
        List<ApiKeyDto> apiKeys
) {
}
