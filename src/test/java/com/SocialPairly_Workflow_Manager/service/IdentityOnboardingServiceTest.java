package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.CurrentResidenceDto;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.RelationshipDto;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.exception.UnprocessableEntityException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityOnboardingServiceTest {

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
                userRepository,
                profileRepository,
                refCountryRepository,
                backgroundRepository,
                lifeProfileRepository,
                complianceRepository,
                blobRepository,
                objectMapper,
                "+91"
        );

        user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setPhoneNumber("+919876543210");
        user.setPassword("x");
        user.setRole(Role.USER);

        profile = new UserProfile();
        profile.setUser(user);
        profile.setOnboardingStep(OnboardingSteps.STEP_1_ACCOUNT);

        lenient().when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> {
            UserIdentityCompliance c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(10L);
            }
            lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(c));
            return c;
        });
        stubAcceptedConsent();
        lenient().when(backgroundRepository.save(any(UserIdentityBackground.class))).thenAnswer(inv -> {
            UserIdentityBackground bg = inv.getArgument(0);
            if (bg.getId() == null) {
                bg.setId(20L);
            }
            lenient().when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
            return bg;
        });
        lenient().when(lifeProfileRepository.save(any(UserLifeProfile.class))).thenAnswer(inv -> {
            UserLifeProfile life = inv.getArgument(0);
            if (life.getId() == null) {
                life.setId(30L);
            }
            lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            return life;
        });

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
                LocalDateTime.now(),
                LocalDateTime.now()
        ).trim());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));
        lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> {
            UserIdentityCompliance c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(10L);
            }
            lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(c));
            return c;
        });
    }

    @Test
    void continueRejectsUnderage() {
        IdentityBackgroundRequest req = baseRequest(
                IdentityOnboardingService.ACTION_CONTINUE,
                LocalDate.now().minusYears(16)
        );
        assertThrows(UnprocessableEntityException.class, () -> service.saveIdentity(user, req));
    }

    @Test
    void continueAcceptsLeapYearDobAndMarksComplete() {
        IdentityBackgroundRequest req = baseRequest(
                IdentityOnboardingService.ACTION_CONTINUE,
                LocalDate.of(2000, 2, 29)
        );
        var res = service.saveIdentity(user, req);
        assertTrue(res.identityPage1Complete());
        assertEquals(OnboardingSteps.PERSONALITY_IN_PROGRESS, profile.getOnboardingStep());
        assertNotNull(profile.getIdentityPage1CompletedAt());
        assertTrue(user.is18OrOlder());
    }

    @Test
    void saveLaterDoesNotRequireDob() {
        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                IdentityOnboardingService.ACTION_SAVE_LATER,
                "Ms.", "Ada", null, "Lovelace", null, "Addie",
                null, "She/Her", "Woman", "MATCHES",
                null, null, null, null, "EMAIL", "MORNING",
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        var res = service.saveIdentity(user, req);
        assertFalse(res.identityPage1Complete());
        assertEquals(OnboardingSteps.IDENTITY_IN_PROGRESS, profile.getOnboardingStep());
        assertEquals("Addie", user.getPreferredName());
        assertEquals("Ada", user.getFirstName());
    }

    @Test
    void rejectsInvalidNameCharacters() {
        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                IdentityOnboardingService.ACTION_CONTINUE,
                null, "Ada123", null, "Lovelace", null, null,
                LocalDate.of(1990, 1, 1), null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
    }

    @Test
    void preferredNameDoesNotOverwriteLegalFirst() {
        user.setFirstName("Ada");
        IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                IdentityOnboardingService.ACTION_SAVE_LATER,
                null, "Ada", null, "Lovelace", null, "Addie",
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        service.saveIdentity(user, req);
        assertEquals("Ada", user.getFirstName());
        assertEquals("Addie", user.getPreferredName());
    }

    @Test
    void saveWithoutConsentRejected() {
        when(complianceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        IdentityBackgroundRequest req = baseRequest(
                IdentityOnboardingService.ACTION_SAVE_LATER,
                null
        );
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        assertTrue(ex.getMessage().toLowerCase().contains("consent"));
    }

    @Test
    void invalidSsnRejected() {
        IdentityBackgroundRequest req = baseRequestWithSsn(
                IdentityOnboardingService.ACTION_SAVE_LATER,
                null,
                "12-34-5678"
        );
        assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
    }

    @Test
    void validSsnPersistedAndMaskedOnRead() {
        IdentityBackgroundRequest req = baseRequestWithSsn(
                IdentityOnboardingService.ACTION_SAVE_LATER,
                null,
                "123456789"
        );
        var res = service.saveIdentity(user, req);
        ArgumentCaptor<UserIdentityCompliance> captor = ArgumentCaptor.forClass(UserIdentityCompliance.class);
        verify(complianceRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(c -> "123-45-6789".equals(c.getSsn())));
        assertNotNull(res.verificationSummary());
        assertEquals("***-**-6789", res.verificationSummary().ssn());
    }

    @Test
    void dlUploadSetsFrontDocumentFk() {
        when(blobRepository.save(any(UserIdentityComplianceBlob.class))).thenAnswer(inv -> {
            UserIdentityComplianceBlob d = inv.getArgument(0);
            d.setId(55L);
            if (d.getCreatedAt() == null) {
                d.setCreatedAt(LocalDateTime.now());
            }
            return d;
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "dl.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        var result = service.uploadDocument(user, file, "DL_FRONT");
        assertEquals(55L, result.get("id"));
        assertEquals("DL_FRONT", result.get("docPurpose"));

        ArgumentCaptor<UserIdentityCompliance> captor = ArgumentCaptor.forClass(UserIdentityCompliance.class);
        verify(complianceRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(c -> Long.valueOf(55L).equals(c.getDlFrontBlobId())));
    }

    @Test
    void legacyIdFrontMapsToDlFront() {
        when(blobRepository.save(any(UserIdentityComplianceBlob.class))).thenAnswer(inv -> {
            UserIdentityComplianceBlob d = inv.getArgument(0);
            d.setId(56L);
            if (d.getCreatedAt() == null) {
                d.setCreatedAt(LocalDateTime.now());
            }
            return d;
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "id.png", "image/png", new byte[]{1, 2, 3}
        );
        var result = service.uploadDocument(user, file, "ID_FRONT");
        assertEquals("DL_FRONT", result.get("docPurpose"));
    }

    @Test
    void streamDocumentForbiddenForOtherUser() {
        User owner = new User();
        owner.setId(2L);
        owner.setRole(Role.USER);
        UserIdentityCompliance ownerCompliance = new UserIdentityCompliance();
        ownerCompliance.setId(77L);
        ownerCompliance.setUser(owner);
        UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
        blob.setId(99L);
        blob.setCompliance(ownerCompliance);
        blob.setContentType("image/jpeg");
        blob.setStorageBlob(new byte[]{9});
        when(blobRepository.findById(99L)).thenReturn(Optional.of(blob));

        assertThrows(ResourceNotFoundException.class, () -> service.streamDocument(user, 99L));
    }

    @Test
    void normalizeSsnFormatsDigits() {
        assertEquals("123-45-6789", IdentityOnboardingService.normalizeSsn("123456789"));
        assertEquals("123-45-6789", IdentityOnboardingService.normalizeSsn("123-45-6789"));
    }

    private IdentityBackgroundRequest baseRequest(String action, LocalDate dob) {
        return baseRequestWithSsn(action, dob, null);
    }

    private IdentityBackgroundRequest baseRequestWithSsn(String action, LocalDate dob, String ssn) {
        CurrentResidenceDto residence = new CurrentResidenceDto(
                "1 Main St", null, null, "Austin", "TX", "78701", "US",
                "RENT", 1, 2020, null, "MAYBE", null, List.of()
        );
        RelationshipDto relationship = new RelationshipDto(
                "NEVER_MARRIED", null, null, null, null, null, null, null, null, null
        );
        return new IdentityBackgroundRequest(
                action,
                "Ms.", "Ada", null, "Lovelace", null, null,
                dob, "She/Her", "Woman", "MATCHES",
                null, null, null, null, "EMAIL", "ANY",
                residence, null, null, null, relationship, null, null, null, null, null, null, null,
                ssn
        );
    }
}
