package com.ensureback.dispute;

import com.ensureback.order.Order;
import com.ensureback.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class Dispute {

    public enum Status {
        OPEN,
        SELLER_RESPONDED,
        PARTIAL_REFUND_OFFERED,
        BUYER_ACCEPTED_PARTIAL,
        ESCALATED,
        RESOLVED_BUYER,
        RESOLVED_SELLER,
        CLOSED
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id")
    private User creatorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "escalation_at")
    private OffsetDateTime escalationAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "return_required", nullable = false)
    private boolean returnRequired;

    @OneToMany(mappedBy = "dispute", fetch = FetchType.LAZY)
    private Set<DisputeMessage> messages = new HashSet<>();

    @OneToOne(mappedBy = "dispute", fetch = FetchType.LAZY)
    private PartialRefundOffer partialRefundOffer;

    public Dispute() {
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

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public User getCreatorUser() {
        return creatorUser;
    }

    public void setCreatorUser(User creatorUser) {
        this.creatorUser = creatorUser;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OffsetDateTime getEscalationAt() {
        return escalationAt;
    }

    public void setEscalationAt(OffsetDateTime escalationAt) {
        this.escalationAt = escalationAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReturnRequired() {
        return returnRequired;
    }

    public void setReturnRequired(boolean returnRequired) {
        this.returnRequired = returnRequired;
    }

    public Set<DisputeMessage> getMessages() {
        return messages;
    }

    public void setMessages(Set<DisputeMessage> messages) {
        this.messages = messages;
    }

    public PartialRefundOffer getPartialRefundOffer() {
        return partialRefundOffer;
    }

    public void setPartialRefundOffer(PartialRefundOffer partialRefundOffer) {
        this.partialRefundOffer = partialRefundOffer;
    }
}
