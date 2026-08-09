package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.ProfileRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceMoreTests {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldSkipBlankEducationEntries() {
        User user = new User();
        user.setId(5L);

        ProfileRequest request = new ProfileRequest(
                "About",
                "Engineer",
                "Active",
                "City",
                "Country",
                1.0,
                2.0,
                LocalDate.of(1990, 1, 1),
                "Other",
                null,
                null,
                null,
                List.of(new ProfileRequest.EducationDto("", "BSc", "CS", 2010, 2014))
        );

        when(profileRepository.findByUserId(eq(5L))).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile saved = profileService.updateProfile(user, request);

        assertNotNull(saved);
        assertTrue(saved.getEducations().isEmpty());
    }

    @Test
    void shouldCreateProfileWhenNoneExistsForPhotoUpdate() {
        User user = new User();
        user.setId(7L);

        when(profileRepository.findByUserId(eq(7L))).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile profile = profileService.setProfilePhoto(user, "/uploads/new.png");

        assertNotNull(profile);
        assertEquals("/uploads/new.png", profile.getProfilePhotoUrl());
    }
}
