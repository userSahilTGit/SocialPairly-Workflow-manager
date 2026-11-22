package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_callback_requests", indexes = {
        @Index(name = "idx_support_callback_status_created", columnList = "status, created_at"),
        @Index(name = "idx_support_callback_user", columnList = "user_id"),
        @Index(name = "idx_support_callback_schedule", columnList = "preferred_date, time_slot")
})
public class SupportCallbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_name", nullable = false, length = 120)
    private String userName;

    @Column(name = "user_email", nullable = false, length = 120)
    private String userEmail;

    @Column(name = "phone_number", nullable = false, length = 40)
    private String phoneNumber;

    @Column(length = 500)
    private String note;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "preferred_day", length = 20)
    private String preferredDay;

    @Column(name = "time_slot", length = 40)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportCallbackStatus status = SupportCallbackStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by_email", length = 120)
    private String resolvedByEmail;

    /** Staff member who last changed {@link #status} (contacted / closed / reopened). */
    @Column(name = "status_updated_by_user_id")
    private Long statusUpdatedByUserId;

    @Column(name = "status_updated_by_name", length = 120)
    private String statusUpdatedByName;

    @Column(name = "status_updated_by_email", length = 120)
    private String statusUpdatedByEmail;

    @Column(name = "status_updated_by_role", length = 40)
    private String statusUpdatedByRole;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SupportCallbackStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }

    public String getPreferredDay() { return preferredDay; }
    public void setPreferredDay(String preferredDay) { this.preferredDay = preferredDay; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public SupportCallbackStatus getStatus() { return status; }
    public void setStatus(SupportCallbackStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedByEmail() { return resolvedByEmail; }
    public void setResolvedByEmail(String resolvedByEmail) { this.resolvedByEmail = resolvedByEmail; }

    public Long getStatusUpdatedByUserId() { return statusUpdatedByUserId; }
    public void setStatusUpdatedByUserId(Long statusUpdatedByUserId) { this.statusUpdatedByUserId = statusUpdatedByUserId; }

    public String getStatusUpdatedByName() { return statusUpdatedByName; }
    public void setStatusUpdatedByName(String statusUpdatedByName) { this.statusUpdatedByName = statusUpdatedByName; }

    public String getStatusUpdatedByEmail() { return statusUpdatedByEmail; }
    public void setStatusUpdatedByEmail(String statusUpdatedByEmail) { this.statusUpdatedByEmail = statusUpdatedByEmail; }

    public String getStatusUpdatedByRole() { return statusUpdatedByRole; }
    public void setStatusUpdatedByRole(String statusUpdatedByRole) { this.statusUpdatedByRole = statusUpdatedByRole; }

    public LocalDateTime getStatusUpdatedAt() { return statusUpdatedAt; }
    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
}
