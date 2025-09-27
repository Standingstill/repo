package com.ensureback.dispute.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateDisputeRequest(
        @NotNull UUID orderId,
        @NotBlank @Email String buyerEmail,
        @NotBlank String reason,
        @NotBlank String message,
        List<String> evidenceUrls
) {
}
