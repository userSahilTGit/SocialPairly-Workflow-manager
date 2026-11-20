package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSupportCallbackStatusRequest(
        @NotNull(message = "Status is required")
        SupportCallbackStatus status
) {
}
