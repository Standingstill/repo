package com.ensureback.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StripeConnectStartRequest(@Email @NotBlank String email) {
}
