package com.ensureback.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record MagicLinkValidationResponse(boolean valid, UUID orderId, String buyerEmail, Instant expiresAt, String message) {
}
