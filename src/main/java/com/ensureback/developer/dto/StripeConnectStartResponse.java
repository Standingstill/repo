package com.ensureback.developer.dto;

public record StripeConnectStartResponse(
        boolean alreadyConnected,
        String redirectUrl,
        String state
) {
}
