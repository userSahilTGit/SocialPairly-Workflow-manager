package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Education;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProfileDtoTest {

    @Test
    void shouldConvertUserProfileToDtoAndReturnNullForMissingProfile() {
        UserProfile profile = new UserProfile();
        profile.setId(42L);
        profile.setProfilePhotoUrl("/uploads/avatar.png");
        profile.setAboutMe("Hello");
        profile.setOccupation("Designer");
        profile.setLifestyle("Creative");
        profile.setLocationCity("Rome");
        profile.setLocationCountry("Italy");
        profile.setLatitude(41.9028);
        profile.setLongitude(12.4964);
        profile.setDateOfBirth(LocalDate.of(1988, 12, 1));
        profile.setGender("Female");
        profile.setReligion("Buddhism");
        profile.setPreferredReligion("Open to all");
        profile.setInterests(Set.of("art", "travel"));
        profile.setCreatedAt(LocalDateTime.of(2024, 1, 2, 10, 15));
        profile.setUpdatedAt(LocalDateTime.of(2024, 2, 3, 12, 30));

        Education education = new Education();
        education.setId(7L);
        education.setInstitution("Academy");
        education.setDegree("MFA");
        education.setFieldOfStudy("Visual Arts");
        education.setStartYear(2010);
        education.setEndYear(2014);
        education.setProfile(profile);

        profile.setEducations(List.of(education));

        ProfileDto dto = ProfileDto.from(profile);

        assertNotNull(dto);
        assertEquals(42L, dto.id());
        assertEquals("/uploads/avatar.png", dto.profilePhotoUrl());
        assertEquals("Hello", dto.aboutMe());
        assertEquals("Designer", dto.occupation());
        assertEquals("Creative", dto.lifestyle());
        assertEquals("Rome", dto.locationCity());
        assertEquals("Italy", dto.locationCountry());
        assertEquals(41.9028, dto.latitude());
        assertEquals(12.4964, dto.longitude());
        assertEquals(LocalDate.of(1988, 12, 1), dto.dateOfBirth());
        assertEquals("Female", dto.gender());
        assertEquals("Buddhism", dto.religion());
        assertEquals("Open to all", dto.preferredReligion());
        assertEquals(Set.of("art", "travel"), dto.interests());
        assertNotNull(dto.educations());
        assertEquals(1, dto.educations().size());
        assertEquals("Academy", dto.educations().get(0).institution());
        assertEquals(LocalDateTime.of(2024, 1, 2, 10, 15), dto.createdAt());
        assertEquals(LocalDateTime.of(2024, 2, 3, 12, 30), dto.updatedAt());

        assertNull(ProfileDto.from(null));
    }
}
