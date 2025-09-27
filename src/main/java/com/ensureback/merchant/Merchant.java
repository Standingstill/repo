package com.ensureback.merchant;

import com.ensureback.developer.IntegrationChecklist;
import com.ensureback.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "support_email", nullable = false)
    private String supportEmail;

    @Column(name = "dispute_window_hours")
    private Integer disputeWindowHours;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToOne(mappedBy = "merchant", fetch = FetchType.LAZY)
    private IntegrationChecklist integrationChecklist;

    protected Merchant() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public Integer getDisputeWindowHours() {
        return disputeWindowHours;
    }

    public void setDisputeWindowHours(Integer disputeWindowHours) {
        this.disputeWindowHours = disputeWindowHours;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public IntegrationChecklist getIntegrationChecklist() {
        return integrationChecklist;
    }

    public void setIntegrationChecklist(IntegrationChecklist integrationChecklist) {
        this.integrationChecklist = integrationChecklist;
    }
}
