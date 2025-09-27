package com.ensureback.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStripeAccountRequest(@NotNull Boolean stripeAccountLinked) {
}