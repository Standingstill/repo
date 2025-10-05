package com.ensureback.developer.dto;

import java.util.List;

public record WebhookStatusDto(
        boolean registered,
        boolean verified,
        String url,
        List<WebhookEventDto> recentEvents
) {
}
