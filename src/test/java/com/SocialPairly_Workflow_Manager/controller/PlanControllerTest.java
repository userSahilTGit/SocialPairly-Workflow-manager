package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PlanDto;
import com.SocialPairly_Workflow_Manager.dto.PlanRequest;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.security.JwtAuthenticationFilter;
import com.SocialPairly_Workflow_Manager.service.PlanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlanService planService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private PlanDto buildSampleDto(Long id, String name, String type, int days, BigDecimal amount, boolean active) {
        return new PlanDto(
                id,
                name,
                type,
                days,
                amount,
                "1000",
                "50",
                "TAG_PRO",
                "A detailed description of the plan",
                active,
                false
        );
    }

    private PlanRequest buildSampleRequest() {
        return new PlanRequest(
                "Premium Plan",
                "MONTHLY",
                30,
                new BigDecimal("99.99"),
                "1000",
                "50",
                "TAG_PRO",
                "Premium plan with advanced features",
                true,
                false
        );
    }

    // --- GET /api/plans ---

    @Test
    @DisplayName("GET /api/plans should return list of active plans")
    void testGetAllActivePlans() throws Exception {
        // Arrange
        PlanDto plan1 = buildSampleDto(1L, "Basic Plan", "MONTHLY", 30, new BigDecimal("10.00"), true);
        PlanDto plan2 = buildSampleDto(2L, "Premium Plan", "YEARLY", 365, new BigDecimal("99.99"), true);
        when(planService.getActivePlans()).thenReturn(List.of(plan1, plan2));

        // Act & Assert
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].planName").value("Basic Plan"))
                .andExpect(jsonPath("$[0].durationDays").value(30))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].planName").value("Premium Plan"))
                .andExpect(jsonPath("$[1].durationDays").value(365));

        verify(planService).getActivePlans();
    }

    // --- GET /api/plans/admin/all ---

    @Test
    @DisplayName("GET /api/plans/admin/all should return all plans for admin")
    void testGetAllPlansForAdmin() throws Exception {
        // Arrange
        PlanDto plan1 = buildSampleDto(1L, "Basic Plan", "MONTHLY", 30, new BigDecimal("10.00"), true);
        PlanDto plan2 = buildSampleDto(2L, "Premium Plan", "YEARLY", 365, new BigDecimal("99.99"), false);
        when(planService.getAllPlans()).thenReturn(List.of(plan1, plan2));

        // Act & Assert
        mockMvc.perform(get("/api/plans/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("Basic Plan"))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].planName").value("Premium Plan"))
                .andExpect(jsonPath("$[1].isActive").value(false));

        verify(planService).getAllPlans();
    }

    // --- POST /api/plans/admin ---

    @Test
    @DisplayName("POST /api/plans/admin should create and return new plan when request is valid")
    void testCreatePlan_Success() throws Exception {
        // Arrange
        PlanRequest request = buildSampleRequest();
        PlanDto createdPlan = buildSampleDto(1L, "Premium Plan", "MONTHLY", 30, new BigDecimal("99.99"), true);
        when(planService.createPlan(any(PlanRequest.class))).thenReturn(createdPlan);

        // Act & Assert
        mockMvc.perform(post("/api/plans/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planName").value("Premium Plan"))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.amount").value(99.99));

        verify(planService).createPlan(any(PlanRequest.class));
    }

    @Test
    @DisplayName("POST /api/plans/admin should return 400 Bad Request when request body fails validation")
    void testCreatePlan_ValidationError() throws Exception {
        // Invalid request: planName is blank, durationDays < 1, amount < 0.01
        PlanRequest invalidRequest = new PlanRequest(
                "", "INVALID_TYPE", 0, new BigDecimal("0.00"),
                "100", "10", "TAG", "", null, false
        );

        // Act & Assert
        mockMvc.perform(post("/api/plans/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/plans/admin/{id} ---

    @Test
    @DisplayName("PUT /api/plans/admin/{id} should update and return updated plan")
    void testUpdatePlan_Success() throws Exception {
        // Arrange
        Long planId = 1L;
        PlanRequest request = new PlanRequest(
                "Updated Premium Plan", "YEARLY", 365, new BigDecimal("199.99"),
                "2000", "100", "PRO_TAG", "Updated premium plan", true, true
        );
        PlanDto updatedPlan = buildSampleDto(planId, "Updated Premium Plan", "YEARLY", 365, new BigDecimal("199.99"), true);
        when(planService.updatePlan(eq(planId), any(PlanRequest.class))).thenReturn(updatedPlan);

        // Act & Assert
        mockMvc.perform(put("/api/plans/admin/{id}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planName").value("Updated Premium Plan"))
                .andExpect(jsonPath("$.durationDays").value(365))
                .andExpect(jsonPath("$.amount").value(199.99));

        verify(planService).updatePlan(eq(planId), any(PlanRequest.class));
    }

    // --- DELETE /api/plans/admin/{id} ---

    @Test
    @DisplayName("DELETE /api/plans/admin/{id} should return 200/204 when plan is successfully deleted")
    void testDeletePlan_Success() throws Exception {
        // Arrange
        Long planId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/plans/admin/{id}", planId))
                .andExpect(status().isOk());

        verify(planService).deletePlan(planId);
    }

    @Test
    @DisplayName("DELETE /api/plans/admin/{id} should return 404 when plan is not found")
    void testDeletePlan_NotFound() throws Exception {
        // Arrange
        Long planId = 99L;
        doThrow(new ResourceNotFoundException("Plan not found with id: " + planId))
                .when(planService).deletePlan(planId);

        // Act & Assert
        mockMvc.perform(delete("/api/plans/admin/{id}", planId))
                .andExpect(status().isNotFound());

        verify(planService).deletePlan(planId);
    }
}