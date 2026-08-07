package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SlotRequestDto(
        @NotNull(message = "Preferred date and time is required")
        LocalDateTime slot
) {
}
