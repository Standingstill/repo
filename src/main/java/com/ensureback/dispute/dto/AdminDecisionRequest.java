package com.ensureback.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminDecisionRequest(
        @NotNull Resolution resolution,
        Integer amountCents,
        @NotBlank String reason,
        Boolean requireReturn
) {
    public enum Resolution {
        REFUND,
        RELEASE
    }
}
