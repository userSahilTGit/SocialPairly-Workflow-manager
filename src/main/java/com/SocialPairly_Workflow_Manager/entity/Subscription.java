package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "stripe_customer_id", nullable = false, length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", nullable = false, unique = true, length = 255)
    private String stripeSubscriptionId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

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

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public LocalDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
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
}
