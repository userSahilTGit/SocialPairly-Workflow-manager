package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleRequest;
import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleResponse;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.PersonalityOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalityOnboardingControllerTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PersonalityOnboardingService personalityOnboardingService;

    private PersonalityOnboardingController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new PersonalityOnboardingController(currentUserService, personalityOnboardingService);
        user = new User();
        user.setId(3L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getPersonalityDelegates() {
        PersonalityLifestyleResponse body = mock(PersonalityLifestyleResponse.class);
        when(personalityOnboardingService.getPersonality(user)).thenReturn(body);
        var response = controller.getPersonality();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
    }

    @Test
    void savePersonalityDelegates() {
        PersonalityLifestyleRequest request = new PersonalityLifestyleRequest(
                "CONTINUE", "headline", "story", List.of("kind"), "proud", "philosophy",
                List.of("open"), "hobbies", "notes", "goals", "coffee", "partner",
                "family", "Hindu", "Any");
        PersonalityLifestyleResponse body = mock(PersonalityLifestyleResponse.class);
        when(personalityOnboardingService.savePersonality(user, request)).thenReturn(body);
        var response = controller.savePersonality(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
        verify(personalityOnboardingService).savePersonality(user, request);
    }
}
