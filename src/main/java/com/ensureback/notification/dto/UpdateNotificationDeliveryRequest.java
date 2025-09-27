package com.ensureback.notification.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationDeliveryRequest(@NotNull Boolean delivered) {
}