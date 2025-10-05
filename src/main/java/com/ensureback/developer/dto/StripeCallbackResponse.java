package com.ensureback.developer.dto;

public record StripeCallbackResponse(
        boolean connected,
        String stripeAccountId,
        boolean checklistUpdated,
        boolean integrated,
        String nextStep,
        String message,
        String redirectUrl
) {
}
