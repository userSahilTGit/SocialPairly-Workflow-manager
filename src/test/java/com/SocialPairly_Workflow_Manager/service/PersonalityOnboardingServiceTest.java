package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.constants.ReligionOptions;
import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleRequest;
import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleResponse;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserPersonalityProfile;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserPersonalityProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalityOnboardingServiceTest {

    @Mock UserPersonalityProfileRepository personalityRepository;
    @Mock UserProfileRepository profileRepository;

    @InjectMocks PersonalityOnboardingService service;

    private User user;
    private UserProfile profile;
    private UserPersonalityProfile personality;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);

        profile = new UserProfile();
        profile.setId(3L);
        profile.setUser(user);
        profile.setOnboardingStep(OnboardingSteps.PERSONALITY_IN_PROGRESS);

        personality = new UserPersonalityProfile();
        personality.setId(2L);
        personality.setUser(user);

        lenient().when(personalityRepository.findByUserId(7L)).thenReturn(Optional.of(personality));
        lenient().when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        lenient().when(personalityRepository.save(any(UserPersonalityProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getPersonalityDoesNotInsertWhenMissing() {
        when(personalityRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.empty());

        PersonalityLifestyleResponse res = service.getPersonality(user);

        assertNotNull(res);
        assertNull(res.headline());
        assertNull(res.religion());
        verify(personalityRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getPersonalityIncludesReligionOptions() {
        profile.setReligion("Islam");
        profile.setPreferredReligion("Open to all");

        PersonalityLifestyleResponse res = service.getPersonality(user);

        assertEquals("Islam", res.religion());
        assertEquals("Open to all", res.preferredReligion());
        assertEquals(ReligionOptions.RELIGIONS, res.religionOptions());
        assertTrue(res.preferredReligionOptions().contains("Open to all"));
        assertTrue(res.preferredReligionOptions().contains("Hinduism"));
    }

    @Test
    void savePersonalityPersistsReligionOnProfile() {
        PersonalityLifestyleRequest request = new PersonalityLifestyleRequest(
                "SAVE_LATER",
                "Headline",
                "About story text",
                List.of(),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Sikhism",
                "Hinduism"
        );

        PersonalityLifestyleResponse res = service.savePersonality(user, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals("Sikhism", captor.getValue().getReligion());
        assertEquals("Hinduism", captor.getValue().getPreferredReligion());
        assertEquals("Sikhism", res.religion());
        assertEquals("Hinduism", res.preferredReligion());
    }

    @Test
    void savePersonalityRejectsInvalidReligion() {
        PersonalityLifestyleRequest request = new PersonalityLifestyleRequest(
                "SAVE_LATER",
                "Headline",
                "About",
                List.of(),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "NotARealReligion",
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.savePersonality(user, request));
        assertTrue(ex.getMessage().toLowerCase().contains("religion"));
    }

    @Test
    void savePersonalityRejectsInvalidPreferredReligion() {
        PersonalityLifestyleRequest request = new PersonalityLifestyleRequest(
                "SAVE_LATER",
                "Headline",
                "About",
                List.of(),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Agnostic",
                "NotAllowed"
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.savePersonality(user, request));
        assertTrue(ex.getMessage().toLowerCase().contains("preferred"));
    }
}
