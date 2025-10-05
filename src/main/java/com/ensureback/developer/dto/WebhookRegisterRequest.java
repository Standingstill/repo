package com.ensureback.developer.dto;

import jakarta.validation.constraints.NotBlank;

public record WebhookRegisterRequest(
        @NotBlank String url
) {
}
