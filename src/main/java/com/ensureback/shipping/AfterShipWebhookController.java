package com.ensureback.shipping;

import com.ensureback.webhook.WebhookIdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/aftership")
public class AfterShipWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AfterShipWebhookController.class);

    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;
    private final WebhookIdempotencyService webhookIdempotencyService;

    public AfterShipWebhookController(ShippingService shippingService,
                                      ObjectMapper objectMapper,
                                      WebhookIdempotencyService webhookIdempotencyService) {
        this.shippingService = shippingService;
        this.objectMapper = objectMapper;
        this.webhookIdempotencyService = webhookIdempotencyService;
    }

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody JsonNode payload,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                       @RequestHeader(value = "X-Aftership-Id", required = false) String afterShipIdHeader)
        throws JsonProcessingException {
        JsonNode tracking = payload.path("data").path("tracking");
        if (tracking.isMissingNode()) {
            log.warn("AfterShip webhook without tracking payload");
            return ResponseEntity.ok().build();
        }

        String trackingNumber = tracking.path("tracking_number").asText(null);
        if (trackingNumber == null) {
            log.warn("AfterShip webhook missing tracking number");
            return ResponseEntity.ok().build();
        }

        String rawPayload = objectMapper.writeValueAsString(payload);
        String trackingId = tracking.path("id").asText(null);
        String updatedAt = tracking.path("updated_at").asText("");
        String fallbackKey = trackingNumber + ":" + updatedAt;
        String effectiveKey = firstNonBlank(idempotencyKey, afterShipIdHeader, trackingId, fallbackKey);
        if (!webhookIdempotencyService.registerInvocation("aftership", effectiveKey, rawPayload)) {
            log.debug("Ignoring AfterShip webhook for tracking {} due to idempotency key {}", trackingNumber, effectiveKey);
            return ResponseEntity.ok().build();
        }

        String tag = tracking.path("tag").asText("");
        if ("Delivered".equalsIgnoreCase(tag)) {
            String deliveredAtStr = tracking.path("delivery_time").asText(null);
            if (deliveredAtStr == null) {
                deliveredAtStr = tracking.path("updated_at").asText(null);
            }
            OffsetDateTime deliveredAt = parseDate(deliveredAtStr);
            if (deliveredAt == null) {
                deliveredAt = OffsetDateTime.now();
            }
            shippingService.handleDeliveredEvent(trackingNumber, deliveredAt, rawPayload);
        }

        return ResponseEntity.ok().build();
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            log.warn("Unable to parse deliveredAt value: {}", value);
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}