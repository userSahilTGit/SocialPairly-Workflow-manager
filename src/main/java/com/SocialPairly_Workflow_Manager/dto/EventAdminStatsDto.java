package com.SocialPairly_Workflow_Manager.dto;

public record EventAdminStatsDto(
        long totalUsers,
        long publishedEvents,
        long draftEvents,
        long acceptedMembers
) {
}
