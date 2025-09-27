package com.ensureback.notification.dto;

import java.util.UUID;

public record NotificationPublishRequest(
        String userId,
        String recipientEmail,
        UUID merchantId,
        boolean notifyAdmins,
        String eventType,
        Object payload
) {
}