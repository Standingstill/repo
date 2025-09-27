package com.ensureback.order.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID merchantId,
        String orderNumber,
        String buyerEmail,
        String status,
        int quantity,
        int unitPriceCents,
        int totalAmountCents,
        String currency,
        boolean digital,
        OffsetDateTime expectedDeliveryAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime createdAt
) {
}
