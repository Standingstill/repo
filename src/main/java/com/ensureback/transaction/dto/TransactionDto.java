package com.ensureback.transaction.dto;

import java.time.OffsetDateTime;

public record TransactionDto(
        String transactionId,
        String orderNumber,
        String escrowStatus,
        String captureMode,
        int grossAmountCents,
        int ensurebackFeeCents,
        int netAmountCents,
        String currency,
        OffsetDateTime createdAt
) {
}