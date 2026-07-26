package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PlanRequest(
    @NotBlank(message = "Plan name is required")
    @Size(min = 1, max = 50, message = "Plan name must be between 1 and 50 characters")
    String planName,

    @NotBlank(message = "Plan type is required")
    @Pattern(regexp = "^(MONTHLY|WEEKLY|QUARTERLY|YEARLY)$", message = "Plan type must be one of: MONTHLY, WEEKLY, QUARTERLY, YEARLY")
    String planType,

    @NotNull(message = "Duration days is required")
    @Min(value = 1, message = "Duration days must be at least 1")
    Integer durationDays,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Active status is required")
    Boolean isActive
) {}
