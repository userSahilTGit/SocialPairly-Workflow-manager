package com.SocialPairly_Workflow_Manager.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record EventBookSlotResponseDto(
        Long eventId,
        String rsvpStatus,
        String entryCode,
        String participantCode,
        int tokensSpent,
        int tokenBalance,
        LocalDateTime matchingOpensAt,
        String eventTitle,
        String venueName,
        String location,
        LocalDate eventDate,
        LocalTime eventTime,
        String displayName
) {
}
