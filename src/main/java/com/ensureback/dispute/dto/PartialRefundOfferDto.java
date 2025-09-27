package com.ensureback.dispute.dto;

import java.time.OffsetDateTime;

public record PartialRefundOfferDto(
        int amountCents,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime decidedAt
) {
}