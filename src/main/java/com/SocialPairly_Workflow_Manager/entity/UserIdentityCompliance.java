package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_identity_compliance")
public class UserIdentityCompliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider = "INTERNAL_V1";

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "overall_status", nullable = false, length = 30)
    private String overallStatus = "NOT_STARTED";

    @Column(name = "name_status", nullable = false, length = 30)
    private String nameStatus = "NOT_STARTED";

    @Column(name = "age_status", nullable = false, length = 30)
    private String ageStatus = "NOT_STARTED";

    @Column(name = "photo_status", nullable = false, length = 30)
    private String photoStatus = "NOT_STARTED";

    @Column(name = "ssn", length = 11)
    private String ssn;

    @Column(name = "id_document_type", length = 40)
    private String idDocumentType;

    @Column(name = "id_document_number", length = 100)
    private String idDocumentNumber;

    @Column(name = "dl_front_blob_id")
    private Long dlFrontBlobId;

    @Column(name = "dl_back_blob_id")
    private Long dlBackBlobId;

    @Column(name = "last_error_code", length = 60)
    private String lastErrorCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "documents_meta", columnDefinition = "json")
    private String documentsMeta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consents", columnDefinition = "json")
    private String consents;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_events", columnDefinition = "json")
    private String auditEvents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getNameStatus() { return nameStatus; }
    public void setNameStatus(String nameStatus) { this.nameStatus = nameStatus; }

    public String getAgeStatus() { return ageStatus; }
    public void setAgeStatus(String ageStatus) { this.ageStatus = ageStatus; }

    public String getPhotoStatus() { return photoStatus; }
    public void setPhotoStatus(String photoStatus) { this.photoStatus = photoStatus; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public String getIdDocumentType() { return idDocumentType; }
    public void setIdDocumentType(String idDocumentType) { this.idDocumentType = idDocumentType; }

    public String getIdDocumentNumber() { return idDocumentNumber; }
    public void setIdDocumentNumber(String idDocumentNumber) { this.idDocumentNumber = idDocumentNumber; }

    public Long getDlFrontBlobId() { return dlFrontBlobId; }
    public void setDlFrontBlobId(Long dlFrontBlobId) { this.dlFrontBlobId = dlFrontBlobId; }

    public Long getDlBackBlobId() { return dlBackBlobId; }
    public void setDlBackBlobId(Long dlBackBlobId) { this.dlBackBlobId = dlBackBlobId; }

    public String getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }

    public String getDocumentsMeta() { return documentsMeta; }
    public void setDocumentsMeta(String documentsMeta) { this.documentsMeta = documentsMeta; }

    public String getConsents() { return consents; }
    public void setConsents(String consents) { this.consents = consents; }

    public String getAuditEvents() { return auditEvents; }
    public void setAuditEvents(String auditEvents) { this.auditEvents = auditEvents; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
