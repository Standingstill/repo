package com.ensureback.developer.dto;

public record ApiKeyCreateResult(
        ApiKeyCreateResponse apiKey,
        IntegrationWizardStatusResponse status
) {
}
