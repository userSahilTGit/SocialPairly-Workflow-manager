package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityBackground;
import com.SocialPairly_Workflow_Manager.entity.UserLifeProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;

import java.time.LocalDate;
import java.time.Period;
import java.util.stream.Collectors;

public record EventCandidateDto(
        Long userId,
        String displayName,
        String email,
        String gender,
        Integer age,
        String religion,
        String occupation,
        String locationCity,
        String stateRegion,
        String maritalStatus,
        String educationLevel,
        String profilePhotoUrl,
        boolean verified
) {
    public static EventCandidateDto from(User user, UserProfile profile, UserLifeProfile life, UserIdentityBackground identity) {
        String displayName = buildDisplayName(user);
        Integer age = null;
        String gender = null;
        String religion = null;
        String occupation = null;
        String locationCity = null;
        String educationLevel = null;

        if (profile != null) {
            if (profile.getDateOfBirth() != null) {
                age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
            }
            gender = profile.getGender();
            religion = profile.getReligion();
            occupation = profile.getOccupation();
            locationCity = profile.getLocationCity();
            if (profile.getEducations() != null && !profile.getEducations().isEmpty()) {
                educationLevel = profile.getEducations().stream()
                        .map(Education::getEducationLevel)
                        .filter(level -> level != null && !level.isBlank())
                        .collect(Collectors.joining(", "));
            }
        }

        String stateRegion = identity != null ? identity.getStateRegion() : null;
        String maritalStatus = life != null ? life.getMaritalStatus() : null;
        String photoUrl = profile != null ? profile.getProfilePhotoUrl() : null;

        return new EventCandidateDto(
                user.getId(),
                displayName,
                user.getEmail(),
                gender,
                age,
                religion,
                occupation,
                locationCity,
                stateRegion,
                maritalStatus,
                educationLevel,
                photoUrl,
                user.isVerified()
        );
    }

    private static String buildDisplayName(User user) {
        if (user.getPreferredName() != null && !user.getPreferredName().isBlank()) {
            return user.getPreferredName().trim();
        }
        return ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }
}
