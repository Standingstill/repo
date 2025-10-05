package com.ensureback.auth;

import com.ensureback.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stripe_connect_sessions")
public class StripeConnectSession {

    @Id
    @Column(name = "state", nullable = false)
    private UUID state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 32)
    private User.Role targetRole;

    @Column(name = "return_path", length = 512)
    private String returnPath;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StripeConnectSession() {
        // JPA
    }

    public StripeConnectSession(UUID state,
                                User user,
                                User.Role targetRole,
                                String returnPath,
                                OffsetDateTime createdAt) {
        this.state = state;
        this.user = user;
        this.targetRole = targetRole;
        this.returnPath = returnPath;
        this.createdAt = createdAt;
    }

    public UUID getState() {
        return state;
    }

    public void setState(UUID state) {
        this.state = state;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User.Role getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(User.Role targetRole) {
        this.targetRole = targetRole;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
