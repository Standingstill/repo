package com.ensureback.dispute.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DisputeMessageDto(
        String authorRole,
        String message,
        List<String> evidenceUrls,
        OffsetDateTime createdAt
) {
}