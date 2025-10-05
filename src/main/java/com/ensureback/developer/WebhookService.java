package com.ensureback.developer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Transactional
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final String SIGNATURE_HEADER = "ensureback-signature";
    private static final String TIMESTAMP_HEADER = "ensureback-timestamp";

    private final WebhookEventRepository webhookEventRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookEventRepository webhookEventRepository,
                          WebClient.Builder webClientBuilder,
                          ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public WebhookDeliveryResult deliverEvent(WebhookEndpoint endpoint,
                                              String eventType,
                                              Object payload,
                                              String signingSecret) {
        String serialized = serialize(payload);
        Instant now = Instant.now();
        String timestamp = String.valueOf(now.getEpochSecond());
        String signature = sign(signingSecret, timestamp + "." + serialized);

        boolean delivered = false;
        try {
            ResponseEntity<Void> response = webClient.post()
                    .uri(endpoint.getUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(SIGNATURE_HEADER, signature)
                    .header(TIMESTAMP_HEADER, timestamp)
                    .bodyValue(serialized)
                    .retrieve()
                    .toBodilessEntity()
                    .block(REQUEST_TIMEOUT);
            delivered = response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.warn("Webhook delivery to {} failed: {}", endpoint.getUrl(), ex.getMessage());
        }

        WebhookEvent event = new WebhookEvent();
        event.setId(UUID.randomUUID());
        event.setWebhookEndpoint(endpoint);
        event.setEventType(eventType);
        event.setPayload(serialized);
        event.setDelivered(delivered);
        event.setTimestamp(OffsetDateTime.now());
        webhookEventRepository.save(event);

        return new WebhookDeliveryResult(event, signature, now, delivered);
    }

    public boolean verifySignature(String signingSecret, String payload, Instant timestamp, String providedSignature) {
        if (!StringUtils.hasText(signingSecret) || !StringUtils.hasText(providedSignature) || timestamp == null) {
            return false;
        }
        String message = timestamp.getEpochSecond() + "." + (payload != null ? payload : "");
        String expected = sign(signingSecret, message);
        return constantTimeEquals(expected, providedSignature);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize webhook payload", ex);
        }
    }

    private String sign(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute webhook signature", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }

    public record WebhookDeliveryResult(WebhookEvent event, String signature, Instant timestamp, boolean delivered) {
    }
}



