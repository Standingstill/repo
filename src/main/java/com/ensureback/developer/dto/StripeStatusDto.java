package com.ensureback.developer.dto;

import java.time.OffsetDateTime;

public record StripeStatusDto(
        boolean connected,
        String accountId,
        OffsetDateTime connectedAt
) {
}
