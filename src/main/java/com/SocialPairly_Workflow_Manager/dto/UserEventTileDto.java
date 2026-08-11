package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventParticipant;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UserEventTileDto(
        Long eventId,
        String eventCode,
        String title,
        String venueName,
        String location,
        LocalDate eventDate,
        LocalTime eventTime,
        List<String> eventPerks,
        RsvpStatus rsvpStatus,
        int memberCount,
        String participantCode,
        String entryCode
) {
    public static UserEventTileDto from(EventParticipant participant, int memberCount) {
        EventDetail event = participant.getEvent();
        String participantCode = event.getEventCode() + "-" + participant.getUser().getId();
        return new UserEventTileDto(
                event.getId(),
                event.getEventCode(),
                event.getTitle(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getEventPerks(),
                participant.getRsvpStatus(),
                memberCount,
                participantCode,
                participant.getEntryCode()
        );
    }
}
