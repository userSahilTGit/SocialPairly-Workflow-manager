package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Size;

public record CreateSupportCallbackRequest(
        @Size(max = 500, message = "Note must be 500 characters or fewer")
        String note,

        @Size(max = 20, message = "Preferred date must be 20 characters or fewer")
        String preferredDate,

        @Size(max = 40, message = "Time slot must be 40 characters or fewer")
        String timeSlot
) {
    public CreateSupportCallbackRequest(String note) {
        this(note, null, null);
    }
}
