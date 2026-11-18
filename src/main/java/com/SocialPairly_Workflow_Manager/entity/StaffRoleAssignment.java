package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_role_assignments", indexes = {
        @Index(name = "idx_staff_role_code_active", columnList = "role_code, is_active"),
        @Index(name = "idx_staff_role_user_id", columnList = "user_id")
})
public class StaffRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String email;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_code", nullable = false, length = 40)
    private String roleCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "assignment_source", nullable = false, length = 30)
    private String assignmentSource = "MANUAL";

    @Column(length = 500)
    private String notes;

    @Column(name = "assigned_by_email", length = 120)
    private String assignedByEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getAssignmentSource() { return assignmentSource; }
    public void setAssignmentSource(String assignmentSource) { this.assignmentSource = assignmentSource; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAssignedByEmail() { return assignedByEmail; }
    public void setAssignedByEmail(String assignedByEmail) { this.assignedByEmail = assignedByEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
