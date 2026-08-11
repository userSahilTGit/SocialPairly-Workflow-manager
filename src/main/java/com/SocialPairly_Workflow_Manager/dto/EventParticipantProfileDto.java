package com.SocialPairly_Workflow_Manager.dto;

import java.util.List;
import java.util.Set;

public record EventParticipantProfileDto(
        Long userId,
        String participantCode,
        String displayName,
        Integer age,
        String locationCity,
        String locationLabel,
        boolean verified,
        String gender,
        String religion,
        String occupation,
        String maritalStatus,
        String hasChildren,
        String educationLevel,
        String aboutMe,
        Set<String> interests,
        String relationshipGoals,
        List<String> mediaUrls,
        List<String> sentReactions
) {
}
