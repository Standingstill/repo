package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyDto(
        UUID id,
        OffsetDateTime createdAt,
        boolean revoked
) {
}
