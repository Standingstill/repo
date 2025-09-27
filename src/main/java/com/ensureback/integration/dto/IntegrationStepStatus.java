package com.ensureback.integration.dto;

import java.time.OffsetDateTime;

public record IntegrationStepStatus(boolean completed, String message, OffsetDateTime updatedAt) {
}