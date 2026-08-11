package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UserEventWorkspaceDto(
        Long eventId,
        String eventCode,
        String title,
        String venueName,
        String location,
        LocalDate eventDate,
        LocalTime eventTime,
        List<String> eventPerks,
        String participantCode,
        String entryCode,
        int tokenBalance,
        int totalProfiles,
        int currentProfileIndex
) {
    public static UserEventWorkspaceDto from(
            EventDetail event,
            String participantCode,
            String entryCode,
            int totalProfiles,
            int tokenBalance
    ) {
        return new UserEventWorkspaceDto(
                event.getId(),
                event.getEventCode(),
                event.getTitle(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getEventPerks(),
                participantCode,
                entryCode,
                tokenBalance,
                totalProfiles,
                0
        );
    }
}
