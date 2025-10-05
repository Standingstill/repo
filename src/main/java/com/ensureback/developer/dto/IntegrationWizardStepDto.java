package com.ensureback.developer.dto;

import java.time.OffsetDateTime;

public record IntegrationWizardStepDto(
        String id,
        String label,
        boolean completed,
        OffsetDateTime completedAt,
        String description
) {
}
