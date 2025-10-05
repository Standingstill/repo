package com.ensureback.auth.dto;

public record StripeConnectStartRequest(String role,
                                        String returnPath) {
}
