package com.ensureback.integration.dto;

public record IntegrationChecklistResponse(
        IntegrationStepStatus stripeConnection,
        IntegrationStepStatus webhook,
        IntegrationStepStatus aftership,
        IntegrationStepStatus testCharge
) {
}