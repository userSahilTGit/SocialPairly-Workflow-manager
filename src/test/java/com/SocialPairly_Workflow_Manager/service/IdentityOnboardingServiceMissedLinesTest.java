package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityOnboardingServiceMissedLinesTest {

    @Mock UserRepository userRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock RefCountryRepository refCountryRepository;
    @Mock UserIdentityBackgroundRepository backgroundRepository;
    @Mock UserLifeProfileRepository lifeProfileRepository;
    @Mock UserIdentityComplianceRepository complianceRepository;
    @Mock UserIdentityComplianceBlobRepository blobRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    IdentityOnboardingService service;
    User user;
    UserProfile profile;

    @BeforeEach
    void setUp() {
        service = new IdentityOnboardingService(
                userRepository, profileRepository, refCountryRepository,
                backgroundRepository, lifeProfileRepository, complianceRepository,
                blobRepository, objectMapper, "+91");

        user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setPhoneNumber("+91-9876543210");
        user.setPassword("x");
        user.setRole(Role.USER);

        profile = new UserProfile();
        profile.setUser(user);
        profile.setOnboardingStep(OnboardingSteps.STEP_1_ACCOUNT);
        profile.setEducations(new ArrayList<>());

        lenient().when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(backgroundRepository.save(any(UserIdentityBackground.class))).thenAnswer(inv -> {
            UserIdentityBackground bg = inv.getArgument(0);
            if (bg.getId() == null) bg.setId(20L);
            lenient().when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
            return bg;
        });
        lenient().when(lifeProfileRepository.save(any(UserLifeProfile.class))).thenAnswer(inv -> {
            UserLifeProfile life = inv.getArgument(0);
            if (life.getId() == null) life.setId(30L);
            lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            return life;
        });
        stubAcceptedConsent();

        RefCountry us = new RefCountry();
        us.setCode("US");
        us.setName("United States");
        us.setPostalRegex("^[0-9]{5}(-[0-9]{4})?$");
        us.setActive(true);
        lenient().when(refCountryRepository.findById("US")).thenReturn(Optional.of(us));
    }

    private void stubAcceptedConsent() {
        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.setId(10L);
        compliance.setUser(user);
        compliance.setConsents("""
                [{"documentVersion":"%s","accepted":true,"acceptedAt":"%s","createdAt":"%s"}]
                """.formatted(
                OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION,
                LocalDateTime.now(), LocalDateTime.now()).trim());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));
        lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> {
            UserIdentityCompliance c = inv.getArgument(0);
            if (c.getId() == null) c.setId(10L);
            lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(c));
            return c;
        });
    }

    @Test
    void continueWithBlankMaritalStatusRejects() {
        UserIdentityBackground bg = new UserIdentityBackground();
        bg.setId(20L);
        bg.setUser(user);
        bg.setLine1("1 Main");
        bg.setCity("Austin");
        bg.setStateRegion("TX");
        bg.setPostalCode("78701");
        bg.setCountryCode("US");
        when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

        UserLifeProfile life = new UserLifeProfile();
        life.setId(30L);
        life.setUser(user);
        life.setMaritalStatus("   ");
        when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                LocalDate.of(1990, 1, 1), null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        assertTrue(ex.getMessage().contains("maritalStatus"));
    }

    @Test
    void familyChildAgeRangesSkipsBlankEntries() {
        List<String> ranges = new java.util.ArrayList<>();
        ranges.add("0_2");
        ranges.add("  ");
        ranges.add(null);
        ranges.add("3_5");
        FamilyDto family = new FamilyDto(
                "YES", 1, ranges, "YES", null,
                "YES", "YES", 1, "NO", "NO", null, null, null
        );
        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, family, null, null, null, null, null, null, null
        );

        service.saveIdentity(user, req);

        verify(lifeProfileRepository, atLeastOnce()).save(argThat(life ->
                life.getChildAgeRanges() != null
                        && life.getChildAgeRanges().contains("0_2")
                        && life.getChildAgeRanges().contains("3_5")));
    }

    @Test
    void invalidOptionalPhoneRejects() {
        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                null, null, null, null,
                null, "123", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
    }

    @Test
    void genderShownDefaultsToMatchesWhenUnset() {
        profile.setGenderShownToMatches(null);
        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        service.saveIdentity(user, req);
        assertEquals("MATCHES", profile.getGenderShownToMatches());
    }

    @Test
    void getIdentityHandlesMalformedJsonListsGracefully() {
        UserIdentityBackground bg = new UserIdentityBackground();
        bg.setUser(user);
        bg.setPreviousAddresses("not-json");
        bg.setPreferredRelocateLocations("{bad");
        bg.setAdditionalNationalities("[");
        bg.setLanguages("null");
        when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

        UserLifeProfile life = new UserLifeProfile();
        life.setUser(user);
        life.setChildAgeRanges("not-an-array");
        life.setCivilCategories("{");
        when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.setUser(user);
        compliance.setConsents("not-json-array");
        compliance.setSsn("12");
        when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));

        assertDoesNotThrow(() -> service.getIdentity(user));
    }

    @Test
    void getIdentityPicksLatestAcceptedConsent() {
        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.setUser(user);
        compliance.setConsents("""
                [
                  {"documentVersion":"%s","accepted":true,"acceptedAt":"2020-01-01T00:00:00","createdAt":"2020-01-01T00:00:00"},
                  {"documentVersion":"%s","accepted":true,"acceptedAt":"2024-06-01T12:00:00","createdAt":"2024-06-01T12:00:00"},
                  {"documentVersion":"OTHER","accepted":true,"acceptedAt":"2025-01-01T00:00:00","createdAt":"2025-01-01T00:00:00"}
                ]
                """.formatted(
                OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION,
                OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION).trim());
        when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));

        var res = service.getIdentity(user);
        assertNotNull(res.backgroundConsent());
        assertTrue(res.backgroundConsent().accepted());
        assertEquals(OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION, res.backgroundConsent().documentVersion());
        assertNotNull(res.backgroundConsent().acceptedAt());
    }

    @Test
    void residenceDurationHandlesFutureMoveInAsZero() {
        UserIdentityBackground bg = new UserIdentityBackground();
        bg.setUser(user);
        bg.setLine1("1 Main");
        bg.setCity("Austin");
        bg.setStateRegion("TX");
        bg.setPostalCode("78701");
        bg.setCountryCode("US");
        bg.setMoveInMonth(1);
        bg.setMoveInYear(LocalDate.now().getYear() + 2);
        when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

        var res = service.getIdentity(user);
        assertNotNull(res.currentResidence());
        assertEquals(0, res.currentResidence().residenceDurationMonths());
    }

    @Test
    void incompleteResidenceBranchesCoverMissingFields() {
        UserIdentityBackground bg = new UserIdentityBackground();
        bg.setId(20L);
        bg.setUser(user);
        bg.setLine1("1 Main");
        bg.setCity("Austin");
        bg.setStateRegion("TX");
        bg.setPostalCode("78701");
        bg.setCountryCode(null);
        when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

        UserLifeProfile life = new UserLifeProfile();
        life.setUser(user);
        life.setMaritalStatus("NEVER_MARRIED");
        lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                LocalDate.of(1990, 1, 1), null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
    }
}
