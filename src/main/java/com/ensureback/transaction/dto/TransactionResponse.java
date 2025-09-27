package com.ensureback.transaction.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID orderId,
        String stripePaymentIntentId,
        String stripeChargeId,
        String platformChargeId,
        String escrowStatus,
        int grossAmountCents,
        int ensurebackFeeCents,
        int netAmountCents,
        String currency,
        OffsetDateTime createdAt
) {
}
