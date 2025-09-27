package com.ensureback.auth.dto;

import java.time.Instant;

public record MagicLinkResponse(String token, Instant expiresAt) {
}
