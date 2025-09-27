package com.ensureback.dispute.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DisputeMessageResponse(
        UUID id,
        String authorRole,
        String message,
        List<String> evidenceUrls,
        OffsetDateTime createdAt
) {
}
