package com.ensureback.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String stripeAccountId,
                                @NotBlank String role) {
}
