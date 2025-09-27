package com.ensureback.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID merchantId,
        @NotBlank @Email String buyerEmail,
        @NotBlank String productName,
        String productDescription,
        @Min(1) int quantity,
        @Min(0) int unitPriceCents,
        @NotBlank String currency,
        Boolean digital,
        Integer expectedDeliveryDays
) {
}
