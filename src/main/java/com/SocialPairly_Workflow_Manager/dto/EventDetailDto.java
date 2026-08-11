package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record EventDetailDto(
        Long id,
        String eventCode,
        String title,
        String description,
        String venueName,
        String location,
        LocalDate eventDate,
        LocalTime eventTime,
        EventStatus status,
        List<String> eventPerks,
        int memberCount,
        int confirmedCount,
        List<EventParticipantDto> participants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventDetailDto from(EventDetail event, List<EventParticipantDto> participants, int confirmedCount) {
        return new EventDetailDto(
                event.getId(),
                event.getEventCode(),
                event.getTitle(),
                event.getDescription(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getStatus(),
                event.getEventPerks(),
                participants.size(),
                confirmedCount,
                participants,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
