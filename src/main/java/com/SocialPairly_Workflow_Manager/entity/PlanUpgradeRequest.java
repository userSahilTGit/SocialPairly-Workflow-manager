package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_upgrade_request")
public class PlanUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "current_plan", nullable = false, length = 50)
    private String currentPlan;

    @Column(name = "upgrade_plan", nullable = false, length = 50)
    private String upgradePlan;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Convert(converter = PlanUpgradeStatusConverter.class)
    @Column(nullable = false, length = 20)
    private PlanUpgradeStatus status = PlanUpgradeStatus.Started;

    @Convert(converter = PlanUpgradeActionConverter.class)
    @Column(nullable = false, length = 20)
    private PlanUpgradeAction action = PlanUpgradeAction.Requested;

    @Column(name = "extra_token")
    private Integer extraToken = 0;

    @Column(name = "extra_amount", precision = 10, scale = 2)
    private BigDecimal extraAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PlanUpgradeStatus.Started;
        }
        if (this.action == null) {
            this.action = PlanUpgradeAction.Requested;
        }
        if (this.extraToken == null) {
            this.extraToken = 0;
        }
        if (this.extraAmount == null) {
            this.extraAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(String currentPlan) {
        this.currentPlan = currentPlan;
    }

    public String getUpgradePlan() {
        return upgradePlan;
    }

    public void setUpgradePlan(String upgradePlan) {
        this.upgradePlan = upgradePlan;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public PlanUpgradeStatus getStatus() {
        return status;
    }

    public void setStatus(PlanUpgradeStatus status) {
        this.status = status;
    }

    public PlanUpgradeAction getAction() {
        return action;
    }

    public void setAction(PlanUpgradeAction action) {
        this.action = action;
    }

    public Integer getExtraToken() {
        return extraToken;
    }

    public void setExtraToken(Integer extraToken) {
        this.extraToken = extraToken;
    }

    public BigDecimal getExtraAmount() {
        return extraAmount;
    }

    public void setExtraAmount(BigDecimal extraAmount) {
        this.extraAmount = extraAmount;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedUpgradeId() {
        return "UPG-" + id;
    }
}
