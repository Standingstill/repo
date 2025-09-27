package com.ensureback.user.dto;

import java.time.OffsetDateTime;

public record UserDto(
        String userId,
        String email,
        String role,
        boolean stripeAccountLinked,
        boolean passwordSet,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}