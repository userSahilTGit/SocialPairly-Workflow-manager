package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppRatingRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5 stars")
        @Max(value = 5, message = "Rating must be between 1 and 5 stars")
        Integer stars,

        @Size(max = 1000, message = "Suggestion must be 1000 characters or fewer")
        String suggestion
) {
}
