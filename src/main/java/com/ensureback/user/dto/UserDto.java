package com.ensureback.user.dto;

import java.time.OffsetDateTime;

public record UserDto(
        String userId,
        String stripeAccountId,
        String role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
