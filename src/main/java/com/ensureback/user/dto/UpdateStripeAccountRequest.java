package com.ensureback.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStripeAccountRequest(@NotBlank String stripeAccountId) {
}
