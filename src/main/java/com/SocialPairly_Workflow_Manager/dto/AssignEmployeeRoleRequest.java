package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AssignEmployeeRoleRequest(
        @NotBlank @Email String email,
        @NotBlank String roleCode,
        String notes
) {
}
