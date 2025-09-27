package com.ensureback.developer;

import com.ensureback.merchant.Merchant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_checklist")
public class IntegrationChecklist {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private Merchant merchant;

    @Column(name = "connected_stripe", nullable = false)
    private boolean connectedStripe = false;

    @Column(name = "webhook_configured", nullable = false)
    private boolean webhookConfigured = false;

    @Column(name = "aftership_configured", nullable = false)
    private boolean aftershipConfigured = false;

    @Column(name = "test_charge_done", nullable = false)
    private boolean testChargeDone = false;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "aftership_api_key")
    private String aftershipApiKey;

    public IntegrationChecklist() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public boolean isConnectedStripe() {
        return connectedStripe;
    }

    public void setConnectedStripe(boolean connectedStripe) {
        this.connectedStripe = connectedStripe;
    }

    public boolean isWebhookConfigured() {
        return webhookConfigured;
    }

    public void setWebhookConfigured(boolean webhookConfigured) {
        this.webhookConfigured = webhookConfigured;
    }

    public boolean isAftershipConfigured() {
        return aftershipConfigured;
    }

    public void setAftershipConfigured(boolean aftershipConfigured) {
        this.aftershipConfigured = aftershipConfigured;
    }

    public boolean isTestChargeDone() {
        return testChargeDone;
    }

    public void setTestChargeDone(boolean testChargeDone) {
        this.testChargeDone = testChargeDone;
    }

    public OffsetDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public String getAftershipApiKey() {
        return aftershipApiKey;
    }

    public void setAftershipApiKey(String aftershipApiKey) {
        this.aftershipApiKey = aftershipApiKey;
    }
}