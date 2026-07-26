package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanDtoTest {

    @Test
    @DisplayName("from() should correctly map all fields from Plan entity to PlanDto")
    void shouldMapFromPlanEntityCorrectly() {
        // Arrange
        Plan plan = new Plan();
        plan.setId(100L);
        plan.setPlanName("Pro Plan");
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(30);
        plan.setAmount(new BigDecimal("29.99"));
        plan.setTokensIncluded("5000");
        plan.setCommunityPosts("UNLIMITED");
        plan.setSubBadgeTag("PRO_BADGE");
        plan.setDescription("Access to all pro features");
        plan.setActive(true);
        plan.setFeatured(false);

        // Act
        PlanDto dto = PlanDto.from(plan);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.planName()).isEqualTo("Pro Plan");
        assertThat(dto.planType()).isEqualTo("MONTHLY");
        assertThat(dto.durationDays()).isEqualTo(30);
        assertThat(dto.amount()).isEqualTo(new BigDecimal("29.99"));
        assertThat(dto.tokensIncluded()).isEqualTo("5000");
        assertThat(dto.communityPosts()).isEqualTo("UNLIMITED");
        assertThat(dto.subBadgeTag()).isEqualTo("PRO_BADGE");
        assertThat(dto.description()).isEqualTo("Access to all pro features");
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.isFeatured()).isFalse();
    }

    @Test
    @DisplayName("PlanDto record components, equals, hashCode, and toString should work as expected")
    void verifyRecordBoilerplateMethods() {
        // Arrange
        PlanDto dto1 = new PlanDto(
                1L, "Basic", "ANNUAL", 365, new BigDecimal("99.99"),
                "1000", "10", "BASIC_BADGE", "Basic plan description", true, true
        );

        PlanDto dto2 = new PlanDto(
                1L, "Basic", "ANNUAL", 365, new BigDecimal("99.99"),
                "1000", "10", "BASIC_BADGE", "Basic plan description", true, true
        );

        PlanDto dto3 = new PlanDto(
                2L, "Enterprise", "CUSTOM", 365, new BigDecimal("499.99"),
                "50000", "UNLIMITED", "ENT_BADGE", "Enterprise description", true, true
        );

        // Assert direct accessor methods
        assertThat(dto1.id()).isEqualTo(1L);
        assertThat(dto1.planName()).isEqualTo("Basic");
        assertThat(dto1.planType()).isEqualTo("ANNUAL");
        assertThat(dto1.durationDays()).isEqualTo(365);
        assertThat(dto1.amount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(dto1.tokensIncluded()).isEqualTo("1000");
        assertThat(dto1.communityPosts()).isEqualTo("10");
        assertThat(dto1.subBadgeTag()).isEqualTo("BASIC_BADGE");
        assertThat(dto1.description()).isEqualTo("Basic plan description");
        assertThat(dto1.isActive()).isTrue();
        assertThat(dto1.isFeatured()).isTrue();

        // Assert equals and hashCode contract for Java Records
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);

        // Assert toString output contains record fields
        assertThat(dto1.toString()).contains("PlanDto", "Basic", "ANNUAL");
    }

    @Test
    @DisplayName("from() should handle uninitialized Plan entity fields gracefully")
    void shouldHandleUninitializedValuesFromPlan() {
        // Arrange
        Plan plan = new Plan(); // Default primitive values: isActive = true, isFeatured = false

        // Act
        PlanDto dto = PlanDto.from(plan);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isNull();
        assertThat(dto.planName()).isNull();
        assertThat(dto.planType()).isNull();
        assertThat(dto.durationDays()).isNull();
        assertThat(dto.amount()).isNull();
        assertThat(dto.tokensIncluded()).isNull();
        assertThat(dto.communityPosts()).isNull();
        assertThat(dto.subBadgeTag()).isNull();
        assertThat(dto.description()).isNull();
        assertThat(dto.isActive()).isTrue();   // Corrected: Plan entity defaults isActive to true
        assertThat(dto.isFeatured()).isFalse(); // Plan entity defaults isFeatured to false
    }
}