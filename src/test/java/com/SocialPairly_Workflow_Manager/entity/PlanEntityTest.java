package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PlanEntityTest {

    @Test
    @DisplayName("Should verify default field values on instantiation")
    void shouldHaveDefaultValuesOnInstantiation() {
        // Act
        Plan plan = new Plan();

        // Assert
        assertThat(plan.isActive()).isTrue();
        assertThat(plan.isFeatured()).isFalse();
        assertThat(plan.getId()).isNull();
        assertThat(plan.getPlanName()).isNull();
        assertThat(plan.getPlanType()).isNull();
        assertThat(plan.getDurationDays()).isNull();
        assertThat(plan.getAmount()).isNull();
        assertThat(plan.getDescription()).isNull();
        assertThat(plan.getTokensIncluded()).isNull();
        assertThat(plan.getCommunityPosts()).isNull();
        assertThat(plan.getSubBadgeTag()).isNull();
        assertThat(plan.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("Should correctly set and get all entity properties")
    void shouldSetAndGetPropertiesCorrectly() {
        // Arrange
        Plan plan = new Plan();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = new BigDecimal("49.99");

        // Act
        plan.setId(10L);
        plan.setPlanName("Pro Plan");
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(30);
        plan.setAmount(amount);
        plan.setDescription("All features unlocked");
        plan.setTokensIncluded("10000");
        plan.setCommunityPosts("UNLIMITED");
        plan.setSubBadgeTag("PRO_BADGE");
        plan.setFeatured(true);
        plan.setActive(false);
        plan.setCreatedAt(now);

        // Assert
        assertThat(plan.getId()).isEqualTo(10L);
        assertThat(plan.getPlanName()).isEqualTo("Pro Plan");
        assertThat(plan.getPlanType()).isEqualTo("MONTHLY");
        assertThat(plan.getDurationDays()).isEqualTo(30);
        assertThat(plan.getAmount()).isEqualTo(amount);
        assertThat(plan.getDescription()).isEqualTo("All features unlocked");
        assertThat(plan.getTokensIncluded()).isEqualTo("10000");
        assertThat(plan.getCommunityPosts()).isEqualTo("UNLIMITED");
        assertThat(plan.getSubBadgeTag()).isEqualTo("PRO_BADGE");
        assertThat(plan.isFeatured()).isTrue();
        assertThat(plan.isActive()).isFalse();
        assertThat(plan.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("onCreate() should populate createdAt with current timestamp")
    void onCreateShouldPopulateCreatedAt() {
        // Arrange
        Plan plan = new Plan();
        LocalDateTime beforeExecution = LocalDateTime.now();

        // Act
        plan.onCreate();

        // Assert
        LocalDateTime afterExecution = LocalDateTime.now();
        assertThat(plan.getCreatedAt()).isNotNull();
        assertThat(plan.getCreatedAt()).isAfterOrEqualTo(beforeExecution);
        assertThat(plan.getCreatedAt()).isBeforeOrEqualTo(afterExecution);
    }
}