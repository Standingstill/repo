package com.ensureback.integration.dto;

import jakarta.validation.constraints.NotBlank;

public record AftershipKeyRequest(@NotBlank String apiKey) {
}