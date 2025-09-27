package com.ensureback.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_idempotency",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_webhook_idempotency_source_key", columnNames = {"source", "idempotency_key"})
       })
public class WebhookIdempotencyRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected WebhookIdempotencyRecord() {
        // JPA constructor
    }

    public WebhookIdempotencyRecord(UUID id,
                                    String source,
                                    String idempotencyKey,
                                    String payloadHash,
                                    OffsetDateTime createdAt) {
        this.id = id;
        this.source = source;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}