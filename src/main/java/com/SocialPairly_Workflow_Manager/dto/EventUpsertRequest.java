package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventUpsertRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotBlank @Size(max = 255) String venueName,
        @NotBlank @Size(max = 255) String location,
        @NotNull LocalDate eventDate,
        @NotNull LocalTime eventTime,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "State code must be exactly 2 letters") String stateCode,
        @NotBlank @Pattern(regexp = "^\\d{5}$", message = "FIPS code must be exactly 5 digits") String fipsCode,
        List<String> eventPerks,
        @NotEmpty(message = "At least one member must be selected") List<Long> memberUserIds,
        Boolean notifyParticipants
) {
}
