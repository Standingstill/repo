package com.ensureback.transaction;

import com.ensureback.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum EscrowStatus {
        HELD,
        RELEASED,
        REFUNDED
    }

    public enum CaptureMode {
        IMMEDIATE_CAPTURE_AND_HOLD_TRANSFER
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "stripe_payment_intent_id", nullable = false)
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    @Column(name = "platform_charge_id", nullable = false)
    private String platformChargeId;

    @Column(name = "transfer_group")
    private String transferGroup;

    @Column(name = "ensureback_fee_cents", nullable = false)
    private Integer ensurebackFeeCents;

    @Column(name = "gross_amount_cents", nullable = false)
    private Integer grossAmountCents;

    @Column(name = "net_amount_cents", nullable = false)
    private Integer netAmountCents;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", nullable = false)
    private EscrowStatus escrowStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_mode", nullable = false)
    private CaptureMode captureMode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Transaction() {
        // JPA
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeChargeId() {
        return stripeChargeId;
    }

    public void setStripeChargeId(String stripeChargeId) {
        this.stripeChargeId = stripeChargeId;
    }

    public String getPlatformChargeId() {
        return platformChargeId;
    }

    public void setPlatformChargeId(String platformChargeId) {
        this.platformChargeId = platformChargeId;
    }

    public String getTransferGroup() {
        return transferGroup;
    }

    public void setTransferGroup(String transferGroup) {
        this.transferGroup = transferGroup;
    }

    public Integer getEnsurebackFeeCents() {
        return ensurebackFeeCents;
    }

    public void setEnsurebackFeeCents(Integer ensurebackFeeCents) {
        this.ensurebackFeeCents = ensurebackFeeCents;
    }

    public Integer getGrossAmountCents() {
        return grossAmountCents;
    }

    public void setGrossAmountCents(Integer grossAmountCents) {
        this.grossAmountCents = grossAmountCents;
    }

    public Integer getNetAmountCents() {
        return netAmountCents;
    }

    public void setNetAmountCents(Integer netAmountCents) {
        this.netAmountCents = netAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public EscrowStatus getEscrowStatus() {
        return escrowStatus;
    }

    public void setEscrowStatus(EscrowStatus escrowStatus) {
        this.escrowStatus = escrowStatus;
    }

    public CaptureMode getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(CaptureMode captureMode) {
        this.captureMode = captureMode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
