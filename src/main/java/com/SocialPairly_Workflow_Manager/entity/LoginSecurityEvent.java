package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_security_events", indexes = {
        @Index(name = "idx_login_sec_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_login_sec_type_created", columnList = "event_type, created_at"),
        @Index(name = "idx_login_sec_identifier", columnList = "identifier, created_at")
})
public class LoginSecurityEvent {

    public static final String FAILED_LOGIN = "FAILED_LOGIN";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOCK_EXPIRED_CLEARED = "LOCK_EXPIRED_CLEARED";

    public static final String TIER_UNSUBSCRIBED_UNVERIFIED = "UNSUBSCRIBED_UNVERIFIED";
    public static final String TIER_UNSUBSCRIBED_VERIFIED = "UNSUBSCRIBED_VERIFIED";
    public static final String TIER_SUBSCRIBED = "SUBSCRIBED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "identifier", nullable = false, length = 120)
    private String identifier;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "security_tier", length = 40)
    private String securityTier;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSecurityTier() { return securityTier; }
    public void setSecurityTier(String securityTier) { this.securityTier = securityTier; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
