package com.SocialPairly_Workflow_Manager.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record UserEventHomeStateDto(
        String phase,
        UnlockStepsDto unlockSteps,
        FeaturedEventDto featuredEvent,
        VipPassDto vipPass,
        List<UserEventNotificationDto> notifications,
        int tokenBalance
) {
    public record UnlockStepsDto(
            boolean accountRegistered,
            boolean phoneVerified,
            boolean emailVerified,
            boolean profileSectionsComplete,
            boolean tokensReady,
            boolean canBookSlot,
            int tokenBalance,
            int tokenCostRequired
    ) {
    }

    public record FeaturedEventDto(
            Long eventId,
            String eventCode,
            String title,
            String description,
            String venueName,
            String location,
            LocalDate eventDate,
            LocalTime eventTime,
            LocalDateTime matchingOpensAt,
            String rsvpStatus,
            int tokenCost,
            int tokenBalance,
            Integer slotsTotalForGender,
            Integer slotsLeftForGender,
            String gender,
            String participantCode,
            String entryCode,
            int confirmedCount,
            int memberCount
    ) {
    }

    public record VipPassDto(
            String passCode,
            String participantCode,
            String displayName,
            String photoUrl,
            String eventTitle,
            String venueName,
            String location,
            LocalDate eventDate,
            LocalTime eventTime,
            int tokensRedeemed,
            String membershipLabel
    ) {
    }

    public record UserEventNotificationDto(
            Long id,
            Long eventId,
            String kind,
            String title,
            String body,
            boolean seen,
            LocalDateTime createdAt
    ) {
    }
}
