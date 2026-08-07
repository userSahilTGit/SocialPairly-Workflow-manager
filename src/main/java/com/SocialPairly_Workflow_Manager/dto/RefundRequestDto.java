package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequestDto(
        @NotBlank(message = "Reason is required")
        @Size(max = 100, message = "Reason must be at most 100 characters")
        String reason
) {
}
