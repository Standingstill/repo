package com.ensureback.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MagicLinkRequest(
        @NotNull UUID orderId,
        @NotBlank @Email String buyerEmail
) {
}
