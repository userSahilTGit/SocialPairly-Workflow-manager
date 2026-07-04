package com.SocialPairly_Workflow_Manager.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ProfileRequest(
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
    List<EducationDto> educations
) {
    public record EducationDto(
        String institution,
        String degree,
        String fieldOfStudy,
        Integer startYear,
        Integer endYear
    ) {}
}