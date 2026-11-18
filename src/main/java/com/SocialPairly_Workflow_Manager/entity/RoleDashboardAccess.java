package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_dashboard_access",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_dashboard", columnNames = {"role_code", "dashboard_key"}))
public class RoleDashboardAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 40)
    private String roleCode;

    @Column(name = "dashboard_key", nullable = false, length = 60)
    private String dashboardKey;

    @Column(name = "can_access", nullable = false)
    private boolean canAccess = true;

    @Column(name = "updated_by_email", length = 120)
    private String updatedByEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getDashboardKey() { return dashboardKey; }
    public void setDashboardKey(String dashboardKey) { this.dashboardKey = dashboardKey; }

    public boolean isCanAccess() { return canAccess; }
    public void setCanAccess(boolean canAccess) { this.canAccess = canAccess; }

    public String getUpdatedByEmail() { return updatedByEmail; }
    public void setUpdatedByEmail(String updatedByEmail) { this.updatedByEmail = updatedByEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
