package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_inbox_notifications", indexes = {
        @Index(name = "idx_staff_inbox_recipient_read", columnList = "recipient_email, is_read, created_at"),
        @Index(name = "idx_staff_inbox_role_read", columnList = "recipient_role_code, is_read, created_at")
})
public class StaffInboxNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_email", nullable = false, length = 120)
    private String recipientEmail;

    @Column(name = "recipient_role_code", nullable = false, length = 40)
    private String recipientRoleCode;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "related_callback_id")
    private Long relatedCallbackId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.recipientEmail != null) {
            this.recipientEmail = this.recipientEmail.trim().toLowerCase();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientRoleCode() { return recipientRoleCode; }
    public void setRecipientRoleCode(String recipientRoleCode) { this.recipientRoleCode = recipientRoleCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getRelatedCallbackId() { return relatedCallbackId; }
    public void setRelatedCallbackId(Long relatedCallbackId) { this.relatedCallbackId = relatedCallbackId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
