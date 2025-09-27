package com.ensureback.dispute;

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
@Table(name = "dispute_messages")
public class DisputeMessage {

    public enum AuthorRole {
        BUYER,
        SELLER,
        ADMIN
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false)
    private AuthorRole authorRole;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "evidence_urls")
    private String evidenceUrls;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public DisputeMessage() {
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

    public Dispute getDispute() {
        return dispute;
    }

    public void setDispute(Dispute dispute) {
        this.dispute = dispute;
    }

    public AuthorRole getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(AuthorRole authorRole) {
        this.authorRole = authorRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(String evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
