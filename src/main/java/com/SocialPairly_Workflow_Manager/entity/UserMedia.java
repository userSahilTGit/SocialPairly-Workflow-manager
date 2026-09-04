package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "media_data", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] mediaData; // 👈 Stores file binary data directly in MySQL

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "file_size_kb", nullable = false)
    private Double fileSizeKb;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MediaStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "display_order")
    private Integer displayOrder;

    // 👈 NEW: Categorizes media purpose (e.g., 'PRIMARY', 'LIFESTYLE', 'HOBBY')
    // Allows the frontend to request and map specific photos to dedicated profile slots
    @Column(name = "media_category", length = 50)
    private String mediaCategory;

    // 👈 NEW: Visibility permission (e.g., 'PUBLIC', 'MUTUAL_ONLY')
    // API query logic checks this field before returning media payloads to browsing users
    @Column(name = "privacy_mode", length = 20)
    private String privacyMode;

    // 👈 NEW: Flag set to true for the default photo used on profile preview cards
    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = false;

    // 👈 NEW: Video prompt text for video responses (e.g., "Introduce yourself in 30 seconds")
    @Column(name = "prompt_text", length = 255)
    private String promptText;

    // 👈 NEW: Requires access approval - hides video until user approves viewer request
    @Column(name = "requires_access_approval", nullable = false)
    private Boolean requiresAccessApproval = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Default to APPROVED so uploads appear instantly on user's profile
        if (this.status == null) this.status = MediaStatus.APPROVED;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}