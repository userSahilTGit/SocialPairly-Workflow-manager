package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.ProfileRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldCreateAndSaveProfile() {
        User user = new User();
        user.setId(42L);
        user.setProfileCompleted(false);

        ProfileRequest request = new ProfileRequest(
                "About me",
                "Engineer",
                "Active",
                "New York",
                "USA",
                40.7128,
                -74.0060,
                LocalDate.of(1990, 1, 1),
                "Female",
                Set.of("music", "travel"),
                List.of(new ProfileRequest.EducationDto("University", "BS", "CS", 2010, 2014))
        );

        when(profileRepository.findByUserId(eq(42L))).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile saved = profileService.updateProfile(user, request);

        assertNotNull(saved);
        assertEquals("About me", saved.getAboutMe());
        assertEquals("Engineer", saved.getOccupation());
        assertEquals(1, saved.getEducations().size());
        assertTrue(user.isProfileCompleted());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals("USA", captor.getValue().getLocationCountry());
        verify(userRepository).save(user);
    }

    @Test
    void shouldSetProfilePhoto() {
        User user = new User();
        user.setId(17L);

        when(profileRepository.findByUserId(eq(17L))).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile profile = profileService.setProfilePhoto(user, "/uploads/photo.png");

        assertNotNull(profile);
        assertEquals("/uploads/photo.png", profile.getProfilePhotoUrl());
    }
}
