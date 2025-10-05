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

    @Column(name = "stripe_connected", nullable = false)
    private boolean stripeConnected = false;

    @Column(name = "webhook_registered", nullable = false)
    private boolean webhookRegistered = false;

    @Column(name = "test_charge_passed", nullable = false)
    private boolean testChargePassed = false;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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

    public boolean isStripeConnected() {
        return stripeConnected;
    }

    public void setStripeConnected(boolean stripeConnected) {
        this.stripeConnected = stripeConnected;
    }

    public boolean isWebhookRegistered() {
        return webhookRegistered;
    }

    public void setWebhookRegistered(boolean webhookRegistered) {
        this.webhookRegistered = webhookRegistered;
    }

    public boolean isTestChargePassed() {
        return testChargePassed;
    }

    public void setTestChargePassed(boolean testChargePassed) {
        this.testChargePassed = testChargePassed;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
