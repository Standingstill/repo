package com.ensureback.order.dto;

import java.time.OffsetDateTime;

public record OrderDto(
        String orderNumber,
        String buyerEmail,
        String productName,
        String productDescription,
        int quantity,
        int unitPriceCents,
        int totalAmountCents,
        String currency,
        boolean digital,
        String status,
        OffsetDateTime expectedDeliveryAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime createdAt
) {
}