package com.ensureback.developer.dto;

public record IntegrationWizardUpdateRequest(
        Boolean stripeConnected,
        Boolean webhookRegistered,
        Boolean testChargePassed
) {
}
