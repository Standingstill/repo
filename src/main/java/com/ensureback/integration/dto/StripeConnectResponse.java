package com.ensureback.integration.dto;

public record StripeConnectResponse(String accountLinkUrl, IntegrationChecklistResponse checklist) {
}