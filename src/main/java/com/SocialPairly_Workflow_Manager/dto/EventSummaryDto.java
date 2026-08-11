package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record EventSummaryDto(
        Long id,
        String eventCode,
        String title,
        String venueName,
        String location,
        LocalDate eventDate,
        LocalTime eventTime,
        EventStatus status,
        int memberCount,
        int confirmedCount,
        List<String> eventPerks,
        LocalDateTime createdAt
) {
    public static EventSummaryDto from(EventDetail event, int memberCount, int confirmedCount) {
        return new EventSummaryDto(
                event.getId(),
                event.getEventCode(),
                event.getTitle(),
                event.getVenueName(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getStatus(),
                memberCount,
                confirmedCount,
                event.getEventPerks(),
                event.getCreatedAt()
        );
    }
}
