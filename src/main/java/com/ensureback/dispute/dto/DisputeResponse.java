package com.ensureback.dispute.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID orderId,
        String buyerEmail,
        String status,
        String reason,
        Integer partialRefundAmountCents,
        OffsetDateTime createdAt,
        OffsetDateTime escalationAt,
        List<DisputeMessageResponse> messages
) {
}
