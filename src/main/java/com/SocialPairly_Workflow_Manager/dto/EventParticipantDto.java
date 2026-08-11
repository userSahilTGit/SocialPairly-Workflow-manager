package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventParticipant;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import com.SocialPairly_Workflow_Manager.entity.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record EventParticipantDto(
        Long id,
        Long userId,
        String displayName,
        String email,
        String gender,
        String occupation,
        String profilePhotoUrl,
        String participantCode,
        RsvpStatus rsvpStatus,
        String entryCode,
        VerificationStatus verificationStatus,
        LocalDateTime assignedAt
) {
    public static EventParticipantDto from(EventParticipant participant, String eventCode) {
        var user = participant.getUser();
        var profile = user.getProfile();
        String displayName = buildDisplayName(user.getPreferredName(), user.getFirstName(), user.getLastName());
        String gender = profile != null ? profile.getGender() : null;
        String occupation = profile != null ? profile.getOccupation() : null;
        String photoUrl = profile != null ? profile.getProfilePhotoUrl() : null;
        String participantCode = eventCode + "-" + user.getId();

        return new EventParticipantDto(
                participant.getId(),
                user.getId(),
                displayName,
                user.getEmail(),
                gender,
                occupation,
                photoUrl,
                participantCode,
                participant.getRsvpStatus(),
                participant.getEntryCode(),
                participant.getVerificationStatus(),
                participant.getAssignedAt()
        );
    }

    private static String buildDisplayName(String preferred, String first, String last) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }
}
