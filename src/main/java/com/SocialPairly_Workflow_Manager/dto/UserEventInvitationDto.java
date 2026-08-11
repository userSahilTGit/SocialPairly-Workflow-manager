package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UserEventInvitationDto(
        Long eventId,
        String eventCode,
        String title,
        String venueName,
        String location,
        LocalDate eventDate,
        LocalTime eventTime,
        List<String> eventPerks,
        int memberCount,
        List<EventMemberPreviewDto> memberPreviews
) {
    public static UserEventInvitationDto from(EventDetail event, int memberCount, List<EventMemberPreviewDto> previews) {
        return new UserEventInvitationDto(
                event.getId(),
                event.getEventCode(),
                event.getTitle(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getEventPerks(),
                memberCount,
                previews
        );
    }
}
