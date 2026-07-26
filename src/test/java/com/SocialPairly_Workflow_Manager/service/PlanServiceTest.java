package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.PlanDto;
import com.SocialPairly_Workflow_Manager.dto.PlanRequest;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private Plan createSamplePlan(Long id, String name, int days, boolean active) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setPlanName(name);
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(days);
        plan.setAmount(new BigDecimal("29.99"));
        plan.setTokensIncluded("1000");
        plan.setCommunityPosts("50");
        plan.setSubBadgeTag("PRO");
        plan.setDescription("Sample plan description");
        plan.setActive(active);
        plan.setFeatured(true);
        return plan;
    }

    private PlanRequest createSampleRequest(Boolean isActive, Boolean isFeatured) {
        return new PlanRequest(
                "Pro Plan",
                "MONTHLY",
                30,
                new BigDecimal("29.99"),
                "1000",
                "50",
                "PRO",
                "Sample plan description",
                isActive,
                isFeatured
        );
    }

    // --- getActivePlans ---

    @Test
    @DisplayName("getActivePlans should return list of active PlanDto objects")
    void getActivePlans_ShouldReturnListOfActivePlans() {
        // Arrange
        Plan plan = createSamplePlan(1L, "Basic Plan", 30, true);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));

        // Act
        List<PlanDto> result = planService.getActivePlans();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).planName()).isEqualTo("Basic Plan");
        verify(planRepository, times(1)).findByIsActiveTrueOrderByDurationDaysAsc();
    }

    @Test
    @DisplayName("getActivePlans should return empty list when no active plans exist")
    void getActivePlans_ShouldReturnEmptyList_WhenNoActivePlans() {
        // Arrange
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(Collections.emptyList());

        // Act
        List<PlanDto> result = planService.getActivePlans();

        // Assert
        assertThat(result).isEmpty();
        verify(planRepository, times(1)).findByIsActiveTrueOrderByDurationDaysAsc();
    }

    // --- getAllPlans ---

    @Test
    @DisplayName("getAllPlans should return list of all PlanDto objects")
    void getAllPlans_ShouldReturnListOfAllPlans() {
        // Arrange
        Plan plan1 = createSamplePlan(1L, "Monthly", 30, true);
        Plan plan2 = createSamplePlan(2L, "Yearly", 365, false);
        when(planRepository.findAllByOrderByDurationDaysAsc()).thenReturn(List.of(plan1, plan2));

        // Act
        List<PlanDto> result = planService.getAllPlans();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).planName()).isEqualTo("Monthly");
        assertThat(result.get(1).planName()).isEqualTo("Yearly");
        verify(planRepository, times(1)).findAllByOrderByDurationDaysAsc();
    }

    // --- createPlan ---

    @Test
    @DisplayName("createPlan should map request, save plan, and return Dto with explicit boolean flags")
    void createPlan_ShouldSaveAndReturnDto_WhenExplicitBooleansProvided() {
        // Arrange
        PlanRequest request = createSampleRequest(true, true);
        Plan savedPlan = createSamplePlan(10L, "Pro Plan", 30, true);

        when(planRepository.save(any(Plan.class))).thenReturn(savedPlan);

        // Act
        PlanDto result = planService.createPlan(request);

        // Assert
        ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository).save(captor.capture());

        Plan capturedPlan = captor.getValue();
        assertThat(capturedPlan.getPlanName()).isEqualTo("Pro Plan");
        assertThat(capturedPlan.isActive()).isTrue();
        assertThat(capturedPlan.isFeatured()).isTrue();

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("createPlan should fall back to defaults when isActive and isFeatured are null")
    void createPlan_ShouldUseDefaultBooleans_WhenNullProvided() {
        // Arrange
        PlanRequest request = createSampleRequest(null, null);
        Plan savedPlan = createSamplePlan(11L, "Pro Plan", 30, true);
        savedPlan.setFeatured(false);

        when(planRepository.save(any(Plan.class))).thenReturn(savedPlan);

        // Act
        PlanDto result = planService.createPlan(request);

        // Assert
        ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository).save(captor.capture());

        Plan capturedPlan = captor.getValue();
        assertThat(capturedPlan.isActive()).isTrue();    // Default fallback
        assertThat(capturedPlan.isFeatured()).isFalse(); // Default fallback
        assertThat(result).isNotNull();
    }

    // --- updatePlan ---

    @Test
    @DisplayName("updatePlan should update existing plan with provided fields")
    void updatePlan_ShouldUpdateAndReturnDto_WhenPlanExists() {
        // Arrange
        Long planId = 1L;
        Plan existingPlan = createSamplePlan(planId, "Old Name", 15, false);
        PlanRequest request = createSampleRequest(false, true);

        when(planRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PlanDto result = planService.updatePlan(planId, request);

        // Assert
        verify(planRepository).findById(planId);
        verify(planRepository).save(existingPlan);

        assertThat(result.planName()).isEqualTo("Pro Plan");
        assertThat(result.isActive()).isFalse();
        assertThat(result.isFeatured()).isTrue();
    }

    @Test
    @DisplayName("updatePlan should fall back to default boolean values when isActive and isFeatured are null")
    void updatePlan_ShouldUseDefaultBooleans_WhenNullProvided() {
        // Arrange
        Long planId = 1L;
        Plan existingPlan = createSamplePlan(planId, "Old Name", 15, false);
        PlanRequest request = createSampleRequest(null, null);

        when(planRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PlanDto result = planService.updatePlan(planId, request);

        // Assert
        verify(planRepository).save(existingPlan);
        assertThat(existingPlan.isActive()).isTrue();
        assertThat(existingPlan.isFeatured()).isFalse();
    }

    @Test
    @DisplayName("updatePlan should throw ResourceNotFoundException when plan ID does not exist")
    void updatePlan_ShouldThrowException_WhenPlanNotFound() {
        // Arrange
        Long planId = 99L;
        PlanRequest request = createSampleRequest(true, false);
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.updatePlan(planId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found with id: 99");

        verify(planRepository, never()).save(any());
    }

    // --- deletePlan ---

    @Test
    @DisplayName("deletePlan should call repository delete when plan exists")
    void deletePlan_ShouldDelete_WhenPlanExists() {
        // Arrange
        Long planId = 1L;
        Plan existingPlan = createSamplePlan(planId, "Basic", 30, true);
        when(planRepository.findById(planId)).thenReturn(Optional.of(existingPlan));

        // Act
        planService.deletePlan(planId);

        // Assert
        verify(planRepository).findById(planId);
        verify(planRepository).delete(existingPlan);
    }

    @Test
    @DisplayName("deletePlan should throw ResourceNotFoundException when plan ID does not exist")
    void deletePlan_ShouldThrowException_WhenPlanNotFound() {
        // Arrange
        Long planId = 99L;
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.deletePlan(planId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found with id: 99");

        verify(planRepository, never()).delete(any());
    }
}