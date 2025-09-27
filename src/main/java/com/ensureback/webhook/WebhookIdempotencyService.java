package com.ensureback.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIdempotencyService.class);

    private final WebhookIdempotencyRepository repository;

    public WebhookIdempotencyService(WebhookIdempotencyRepository repository) {
        this.repository = repository;
    }

    /**
     * Registers a webhook invocation and returns whether it should be processed.
     * Subsequent calls with the same source/key combination return {@code false}.
     */
    @Transactional
    public boolean registerInvocation(String source, String providedKey, String payload) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must be provided");
        }

        String payloadHash = hashPayload(payload);
        String effectiveKey = normalizeKey(providedKey, payloadHash);
        if (effectiveKey == null) {
            log.debug("No idempotency key available for source {} - allowing processing", source);
            return true;
        }

        WebhookIdempotencyRecord record = new WebhookIdempotencyRecord(
            UUID.randomUUID(),
            source,
            effectiveKey,
            payloadHash,
            OffsetDateTime.now()
        );
        try {
            repository.save(record);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate webhook invocation ignored for source {} and key {}", source, effectiveKey);
            return false;
        }
    }

    private String normalizeKey(String providedKey, String payloadHash) {
        if (providedKey != null && !providedKey.isBlank()) {
            return providedKey;
        }
        if (payloadHash != null && !payloadHash.isBlank()) {
            return payloadHash;
        }
        return null;
    }

    private String hashPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
