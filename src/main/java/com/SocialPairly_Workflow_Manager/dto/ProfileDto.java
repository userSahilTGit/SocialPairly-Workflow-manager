package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ProfileDto(
    Long id,
    String profilePhotoUrl,
    String aboutMe,
    String occupation,
    String lifestyle,
    String locationCity,
    String locationCountry,
    Double latitude,
    Double longitude,
    LocalDate dateOfBirth,
    String gender,
    Set<String> interests,
    List<EducationDto> educations,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProfileDto from(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        return new ProfileDto(
            profile.getId(),
            profile.getProfilePhotoUrl(),
            profile.getAboutMe(),
            profile.getOccupation(),
            profile.getLifestyle(),
            profile.getLocationCity(),
            profile.getLocationCountry(),
            profile.getLatitude(),
            profile.getLongitude(),
            profile.getDateOfBirth(),
            profile.getGender(),
            profile.getInterests(),
            profile.getEducations().stream().map(EducationDto::from).collect(Collectors.toList()),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }

    public record EducationDto(
        Long id,
        String institution,
        String degree,
        String fieldOfStudy,
        Integer startYear,
        Integer endYear
    ) {
        public static EducationDto from(Education education) {
            return new EducationDto(
                education.getId(),
                education.getInstitution(),
                education.getDegree(),
                education.getFieldOfStudy(),
                education.getStartYear(),
                education.getEndYear()
            );
        }
    }
}
