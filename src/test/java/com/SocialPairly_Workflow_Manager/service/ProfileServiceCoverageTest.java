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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceCoverageTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldUpdateExistingProfileWithoutCreatingNewOne() {
        User user = new User();
        user.setId(99L);
        user.setProfileCompleted(false);

        UserProfile existingProfile = new UserProfile();
        existingProfile.setId(55L);
        existingProfile.setAboutMe("Previous");

        ProfileRequest.EducationDto educationDto = new ProfileRequest.EducationDto("University", "BS", "CS", 2010, 2014);
        ProfileRequest request = new ProfileRequest(
                "Updated about",
                "Developer",
                "Busy",
                "Berlin",
                "Germany",
                52.52,
                13.405,
                LocalDate.of(1992, 9, 17),
                "Male",
                null,
                java.util.List.of(educationDto)
        );

        when(profileRepository.findByUserId(eq(99L))).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = profileService.updateProfile(user, request);

        assertSame(existingProfile, saved);
        assertEquals("Updated about", saved.getAboutMe());
        assertEquals("Developer", saved.getOccupation());
        assertEquals("Berlin", saved.getLocationCity());
        assertEquals("Germany", saved.getLocationCountry());
        assertEquals(LocalDate.of(1992, 9, 17), saved.getDateOfBirth());
        assertEquals(1, saved.getEducations().size());
        assertTrue(user.isProfileCompleted());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());
        assertSame(existingProfile, captor.getValue());
        verify(userRepository).save(user);
    }

    @Test
    void shouldUpdateProfilePhotoOnExistingProfile() {
        User user = new User();
        user.setId(13L);

        UserProfile existingProfile = new UserProfile();
        existingProfile.setId(13L);

        when(profileRepository.findByUserId(eq(13L))).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = profileService.setProfilePhoto(user, "/uploads/updated-photo.png");

        assertSame(existingProfile, saved);
        assertEquals("/uploads/updated-photo.png", saved.getProfilePhotoUrl());
        verify(profileRepository).save(existingProfile);
        verify(userRepository).save(user);
    }
}
