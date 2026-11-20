package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Size;

public record CreateSupportCallbackRequest(
        @Size(max = 500, message = "Note must be 500 characters or fewer")
        String note
) {
}
