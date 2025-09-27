package com.ensureback.webhook;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookIdempotencyRepository extends JpaRepository<WebhookIdempotencyRecord, UUID> {

    Optional<WebhookIdempotencyRecord> findBySourceAndIdempotencyKey(String source, String idempotencyKey);
}