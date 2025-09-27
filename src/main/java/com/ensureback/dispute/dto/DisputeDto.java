package com.ensureback.dispute.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DisputeDto(
        String disputeId,
        String orderNumber,
        String buyerEmail,
        String status,
        boolean returnRequired,
        OffsetDateTime createdAt,
        OffsetDateTime escalationAt,
        List<DisputeMessageDto> messages,
        PartialRefundOfferDto partialRefundOffer
) {
}