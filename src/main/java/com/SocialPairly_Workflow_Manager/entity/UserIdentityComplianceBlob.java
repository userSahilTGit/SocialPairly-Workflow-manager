package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_identity_compliance_blobs")
public class UserIdentityComplianceBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserIdentityCompliance compliance;

    @Column(name = "doc_purpose", nullable = false, length = 40)
    private String docPurpose;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Lob
    @Column(name = "storage_blob", columnDefinition = "LONGBLOB")
    private byte[] storageBlob;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserIdentityCompliance getCompliance() { return compliance; }
    public void setCompliance(UserIdentityCompliance compliance) { this.compliance = compliance; }

    public String getDocPurpose() { return docPurpose; }
    public void setDocPurpose(String docPurpose) { this.docPurpose = docPurpose; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getFileSizeKb() { return fileSizeKb; }
    public void setFileSizeKb(Integer fileSizeKb) { this.fileSizeKb = fileSizeKb; }

    public byte[] getStorageBlob() { return storageBlob; }
    public void setStorageBlob(byte[] storageBlob) { this.storageBlob = storageBlob; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
