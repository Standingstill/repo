package com.ensureback.transaction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransactionChargeRequest(
        @NotNull UUID orderId,
        @NotNull UUID merchantId,
        @Min(1) int amountCents,
        @NotBlank String currency
) {
}
