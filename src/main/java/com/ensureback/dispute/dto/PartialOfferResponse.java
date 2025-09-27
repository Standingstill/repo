package com.ensureback.dispute.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PartialOfferResponse(
        UUID id,
        int amountCents,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime decidedAt
) {
}
