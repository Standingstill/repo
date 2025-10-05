package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyCreateResponse(
        UUID id,
        String apiKey,
        OffsetDateTime createdAt
) {
}
