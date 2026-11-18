package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEmployeeRoleRequest(
        @NotBlank String roleCode,
        Boolean active,
        String notes
) {
}
