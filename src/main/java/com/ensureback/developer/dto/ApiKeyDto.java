package com.ensureback.developer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeyDto(
        UUID id,
        UUID merchantId,
        String status,
        OffsetDateTime createdAt
) {
}