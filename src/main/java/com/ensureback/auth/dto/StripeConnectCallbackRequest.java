package com.ensureback.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record StripeConnectCallbackRequest(@NotBlank String state,
                                           String code,
                                           String error,
                                           String errorDescription) {
}
