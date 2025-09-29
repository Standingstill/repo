package com.ensureback.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stripe_connect_sessions")
public class StripeConnectSession {

    @Id
    @Column(name = "state", nullable = false)
    private UUID state;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StripeConnectSession() {
        // JPA
    }

    public StripeConnectSession(UUID state, UUID userId, OffsetDateTime createdAt) {
        this.state = state;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public UUID getState() {
        return state;
    }

    public void setState(UUID state) {
        this.state = state;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
