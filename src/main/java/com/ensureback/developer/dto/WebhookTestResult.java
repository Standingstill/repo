package com.ensureback.developer.dto;

public record WebhookTestResult(
        WebhookTestResponse delivery,
        IntegrationWizardStatusResponse status
) {
}
