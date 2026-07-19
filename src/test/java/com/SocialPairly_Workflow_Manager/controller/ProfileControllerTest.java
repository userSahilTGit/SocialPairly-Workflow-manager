package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.ProfileDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.FileStorageService;
import com.SocialPairly_Workflow_Manager.service.ProfileCompletionService;
import com.SocialPairly_Workflow_Manager.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ProfileCompletionService profileCompletionService;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void shouldReturnCurrentUserProfileAndCompletion() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        ProfileDto dto = new ProfileDto(
                10L,
                "/uploads/avatar.png",
                "About me",
                "Engineer",
                "Active",
                "London",
                "UK",
                51.5074,
                -0.1278,
                null,
                "Female",
                null,
                null,
                null,
                null
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(profileService.getProfile(user)).thenReturn(null);
        when(profileCompletionService.calculate(user, null)).thenReturn(Map.of("completion", 42));

        ResponseEntity<Map<String, Object>> profileResponse = profileController.getProfile();
        ResponseEntity<Map<String, Object>> completionResponse = profileController.getProfileCompletion();

        assertEquals(200, profileResponse.getStatusCode().value());
        assertNotNull(profileResponse.getBody());
        assertTrue(profileResponse.getBody().containsKey("user"));
        assertNull(profileResponse.getBody().get("profile"));

        assertEquals(200, completionResponse.getStatusCode().value());
        assertEquals(42, completionResponse.getBody().get("completion"));

        verify(currentUserService, times(2)).getCurrentUser();
        verify(profileService, times(2)).getProfile(user);
        verify(profileCompletionService).calculate(user, null);
    }

    @Test
    void shouldUpdateProfileAndUploadPhoto() {
        User user = new User();
        user.setId(2L);

        ProfileRequest request = new ProfileRequest(
                "About",
                "Engineer",
                "Lifestyle",
                "Paris",
                "France",
                48.8566,
                2.3522,
                null,
                "Male",
                null,
                null
        );

        UserProfile savedProfile = new UserProfile();
        savedProfile.setAboutMe("About");
        savedProfile.setOccupation("Engineer");
        savedProfile.setLifestyle("Lifestyle");
        savedProfile.setLocationCity("Paris");
        savedProfile.setLocationCountry("France");
        savedProfile.setLatitude(48.8566);
        savedProfile.setLongitude(2.3522);
        savedProfile.setGender("Male");

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "dummy".getBytes());

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(profileService.updateProfile(user, request)).thenReturn(savedProfile);
        when(fileStorageService.store(file)).thenReturn("/uploads/photo.png");

        ResponseEntity<com.SocialPairly_Workflow_Manager.dto.ProfileDto> updateResponse = profileController.updateProfile(request);
        ResponseEntity<Map<String, String>> uploadResponse = profileController.uploadPhoto(file);

        assertEquals(200, updateResponse.getStatusCode().value());
        assertNotNull(updateResponse.getBody());
        assertEquals("About", updateResponse.getBody().aboutMe());
        assertEquals("Engineer", updateResponse.getBody().occupation());
        assertEquals("Lifestyle", updateResponse.getBody().lifestyle());
        assertEquals("Paris", updateResponse.getBody().locationCity());
        assertEquals("France", updateResponse.getBody().locationCountry());
        assertEquals("Male", updateResponse.getBody().gender());
        assertEquals("/uploads/photo.png", uploadResponse.getBody().get("url"));

        verify(profileService).updateProfile(user, request);
        verify(profileService).setProfilePhoto(user, "/uploads/photo.png");
    }
}
