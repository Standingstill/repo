package com.ensureback.developer;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    List<WebhookEvent> findTop10ByWebhookEndpoint_IdOrderByTimestampDesc(UUID webhookId);
}
