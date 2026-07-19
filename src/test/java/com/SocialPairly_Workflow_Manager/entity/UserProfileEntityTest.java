package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileEntityTest {

    @Test
    void shouldExposeAllFieldsAndLifecycleCallbacks() {
        UserProfile profile = new UserProfile();

        User user = new User();
        user.setId(1L);
        profile.setUser(user);
        profile.setProfilePhotoUrl("/uploads/photo.jpg");
        profile.setAboutMe("About me text");
        profile.setOccupation("Engineer");
        profile.setLifestyle("Healthy");
        profile.setLocationCity("Austin");
        profile.setLocationCountry("USA");
        profile.setLatitude(30.2672);
        profile.setLongitude(-97.7431);
        profile.setDateOfBirth(LocalDate.of(1995, 5, 5));
        profile.setGender("Non-binary");

        Set<String> interests = new HashSet<>();
        interests.add("music");
        interests.add("coding");
        profile.setInterests(interests);

        Education education = new Education();
        education.setId(101L);
        education.setInstitution("School");
        education.setDegree("BS");
        education.setFieldOfStudy("Computer Science");
        education.setStartYear(2013);
        education.setEndYear(2017);
        education.setProfile(profile);

        List<Education> educations = new ArrayList<>();
        educations.add(education);
        profile.setEducations(educations);

        profile.onCreate();
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());

        LocalDateTime beforeUpdate = profile.getUpdatedAt();
        profile.onUpdate();
        assertNotNull(profile.getUpdatedAt());
        assertTrue(profile.getUpdatedAt().isEqual(beforeUpdate) || profile.getUpdatedAt().isAfter(beforeUpdate));

        assertEquals(user, profile.getUser());
        assertEquals("/uploads/photo.jpg", profile.getProfilePhotoUrl());
        assertEquals("About me text", profile.getAboutMe());
        assertEquals("Engineer", profile.getOccupation());
        assertEquals("Healthy", profile.getLifestyle());
        assertEquals("Austin", profile.getLocationCity());
        assertEquals("USA", profile.getLocationCountry());
        assertEquals(30.2672, profile.getLatitude());
        assertEquals(-97.7431, profile.getLongitude());
        assertEquals(LocalDate.of(1995, 5, 5), profile.getDateOfBirth());
        assertEquals("Non-binary", profile.getGender());
        assertEquals(interests, profile.getInterests());
        assertEquals(1, profile.getEducations().size());
        assertEquals(101L, profile.getEducations().get(0).getId());
    }
}
