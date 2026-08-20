package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundResponse;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.exception.UnprocessableEntityException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityOnboardingServiceCoverageTest {

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
    RefCountry usCountry;

    @BeforeEach
    void setUp() {
        service = new IdentityOnboardingService(
                userRepository, profileRepository, refCountryRepository,
                backgroundRepository, lifeProfileRepository,
                complianceRepository, blobRepository,
                objectMapper, "+91"
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
            if (c.getId() == null) c.setId(10L);
            lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(c));
            return c;
        });
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

        usCountry = new RefCountry();
        usCountry.setCode("US");
        usCountry.setName("United States");
        usCountry.setPostalRegex("^[0-9]{5}(-[0-9]{4})?$");
        usCountry.setActive(true);
        lenient().when(refCountryRepository.findById("US")).thenReturn(Optional.of(usCountry));
        lenient().when(refCountryRepository.findByActiveTrue()).thenReturn(List.of(usCountry));
    }

    private void stubAcceptedConsent() {
        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.setId(10L);
        compliance.setUser(user);
        compliance.setConsents("""
                [{"documentVersion":"%s","accepted":true,"acceptedAt":"%s","createdAt":"%s"}]
                """.formatted(
                OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION,
                LocalDateTime.now(), LocalDateTime.now()
        ).trim());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));
        lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> {
            UserIdentityCompliance c = inv.getArgument(0);
            if (c.getId() == null) c.setId(10L);
            lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(c));
            return c;
        });
    }

    private void stubBlobSave() {
        lenient().when(blobRepository.save(any(UserIdentityComplianceBlob.class))).thenAnswer(inv -> {
            UserIdentityComplianceBlob d = inv.getArgument(0);
            d.setId(55L);
            if (d.getCreatedAt() == null) d.setCreatedAt(LocalDateTime.now());
            return d;
        });
    }

    private IdentityBackgroundRequest minimalRequest(String action) {
        return new IdentityBackgroundRequest(
                action, null, "Ada", null, "Lovelace", null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private IdentityBackgroundRequest continueRequest(LocalDate dob) {
        CurrentResidenceDto residence = new CurrentResidenceDto(
                "1 Main St", null, null, "Austin", "TX", "78701", "US",
                "RENT", 1, 2020, null, "MAYBE", null, List.of()
        );
        RelationshipDto relationship = new RelationshipDto(
                "NEVER_MARRIED", null, null, null, null, null, null, null, null, null
        );
        return new IdentityBackgroundRequest(
                "CONTINUE", "Ms.", "Ada", null, "Lovelace", null, null,
                dob, "She/Her", "Woman", "MATCHES",
                null, null, null, null, "EMAIL", "ANY",
                residence, null, null, null, relationship, null, null, null, null, null, null, null, null
        );
    }

    // ── getIdentity ───────────────────────────────────────────────────

    @Nested
    class GetIdentity {

        @Test
        void returnsResponseWithDisplayNameFromPreferred() {
            user.setPreferredName("Addie");
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertEquals("Addie", res.displayName());
            assertEquals("Ada", res.firstName());
        }

        @Test
        void returnsFirstNameAsDisplayWhenNoPreferred() {
            user.setPreferredName(null);
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertEquals("Ada", res.displayName());
        }

        @Test
        void createsProfileWhenMissing() {
            when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res);
            assertEquals("MATCHES", res.genderShownToMatches());
        }

        @Test
        void returnsAgeWhenDobSet() {
            profile.setDateOfBirth(LocalDate.of(2000, 1, 1));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.age());
            assertTrue(res.age() >= 18);
        }

        @Test
        void returnsNullAgeWhenNoDob() {
            profile.setDateOfBirth(null);
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNull(res.age());
        }

        @Test
        void includesVerificationSummaryWhenComplianceExists() {
            UserIdentityCompliance compliance = new UserIdentityCompliance();
            compliance.setId(10L);
            compliance.setUser(user);
            compliance.setSsn("123-45-6789");
            compliance.setDlFrontBlobId(55L);
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.verificationSummary());
            assertEquals("***-**-6789", res.verificationSummary().ssn());
            assertEquals(55L, res.verificationSummary().dlFrontDocumentId());
        }

        @Test
        void includesResidenceWhenBackgroundPopulated() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setLine1("1 Main St");
            bg.setCity("Austin");
            bg.setStateRegion("TX");
            bg.setPostalCode("78701");
            bg.setCountryCode("US");
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.currentResidence());
            assertEquals("Austin", res.currentResidence().city());
        }

        @Test
        void includesNationalityWhenBackgroundHasNationalityData() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setPrimaryNationality("US");
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.nationality());
            assertEquals("US", res.nationality().primaryNationality());
        }

        @Test
        void includesImmigrationWhenBackgroundHasImmigrationData() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setCurrentCountryOfResidence("US");
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.immigration());
        }

        @Test
        void includesRelationshipWhenLifeProfileHasRelationshipData() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setMaritalStatus("SINGLE");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.relationship());
        }

        @Test
        void includesFamilyWhenLifeProfileHasFamilyData() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setHasChildren("YES");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.family());
        }

        @Test
        void includesCareerWhenLifeProfileHasCareerData() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setJobFunction("Engineer");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.career());
        }

        @Test
        void includesFinancialWhenLifeProfileHasFinancialData() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setIncomeRange("50K_74K");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.financial());
        }

        @Test
        void includesSafetyWhenLifeProfileHasSafetyData() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setCriminalConviction("NO");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.safety());
        }

        @Test
        void includesCivilJudgmentWhenLifeProfileHasCivilData() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setHasJudgment("NO");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.civilJudgment());
        }

        @Test
        void includesConsentWhenComplianceHasConsents() {
            stubAcceptedConsent();
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.backgroundConsent());
            assertTrue(res.backgroundConsent().accepted());
            assertEquals(OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION, res.backgroundConsent().documentVersion());
        }

        @Test
        void identityPage1CompleteWhenCompletedAtSet() {
            profile.setIdentityPage1CompletedAt(LocalDateTime.now());
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertTrue(res.identityPage1Complete());
        }
    }

    // ── getReferenceData ──────────────────────────────────────────────

    @Nested
    class GetReferenceData {

        @Test
        void includesCountryWithPostalRegex() {
            var result = service.getReferenceData();
            assertNotNull(result.get("countries"));
            @SuppressWarnings("unchecked")
            List<Map<String, String>> countries = (List<Map<String, String>>) result.get("countries");
            assertEquals(1, countries.size());
            assertEquals("US", countries.get(0).get("code"));
            assertEquals("United States", countries.get(0).get("name"));
            assertNotNull(countries.get(0).get("postalRegex"));
        }

        @Test
        void omitsPostalRegexWhenNull() {
            RefCountry in = new RefCountry();
            in.setCode("IN");
            in.setName("India");
            in.setPostalRegex(null);
            in.setActive(true);
            when(refCountryRepository.findByActiveTrue()).thenReturn(List.of(in));

            var result = service.getReferenceData();
            @SuppressWarnings("unchecked")
            List<Map<String, String>> countries = (List<Map<String, String>>) result.get("countries");
            assertEquals(1, countries.size());
            assertFalse(countries.get(0).containsKey("postalRegex"));
        }

        @Test
        void containsAllReferenceKeys() {
            var result = service.getReferenceData();
            assertTrue(result.containsKey("residenceTypes"));
            assertTrue(result.containsKey("maritalStatuses"));
            assertTrue(result.containsKey("educationLevels"));
            assertTrue(result.containsKey("incomeRanges"));
            assertTrue(result.containsKey("backgroundConsentDocumentVersion"));
        }
    }

    // ── acceptBackgroundConsent ────────────────────────────────────────

    @Nested
    class AcceptBackgroundConsent {

        @Test
        void rejectsNullAccepted() {
            assertThrows(BadRequestException.class,
                    () -> service.acceptBackgroundConsent(user, null, "BG_SCREEN_V1"));
        }

        @Test
        void rejectsFalseAccepted() {
            assertThrows(BadRequestException.class,
                    () -> service.acceptBackgroundConsent(user, false, "BG_SCREEN_V1"));
        }

        @Test
        void rejectsWrongDocumentVersion() {
            assertThrows(BadRequestException.class,
                    () -> service.acceptBackgroundConsent(user, true, "BG_SCREEN_V99"));
        }

        @Test
        void rejectsNullDocumentVersion() {
            assertThrows(BadRequestException.class,
                    () -> service.acceptBackgroundConsent(user, true, null));
        }

        @Test
        void rejectsBlankDocumentVersion() {
            assertThrows(BadRequestException.class,
                    () -> service.acceptBackgroundConsent(user, true, "  "));
        }

        @Test
        void successAppendsConsent() {
            var result = service.acceptBackgroundConsent(user, true,
                    OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION);
            assertTrue((Boolean) result.get("accepted"));
            assertEquals(OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION, result.get("documentVersion"));
            assertNotNull(result.get("acceptedAt"));
        }
    }

    // ── startVerification ─────────────────────────────────────────────

    @Nested
    class StartVerification {

        @Test
        void createsNewSession() {
            var result = service.startVerification(user);
            assertNotNull(result.get("sessionId"));
            assertFalse(result.get("sessionId").toString().isBlank());
            assertEquals("IN_PROGRESS", result.get("overallStatus"));
            assertEquals("INTERNAL_V1", result.get("provider"));
        }

        @Test
        void reusesExistingSession() {
            UserIdentityCompliance existing = new UserIdentityCompliance();
            existing.setId(10L);
            existing.setUser(user);
            existing.setSessionId("existing-session-id");
            existing.setOverallStatus("IN_PROGRESS");
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

            var result = service.startVerification(user);
            assertEquals("existing-session-id", result.get("sessionId"));
        }

        @Test
        void movesNotStartedToInProgress() {
            UserIdentityCompliance existing = new UserIdentityCompliance();
            existing.setId(10L);
            existing.setUser(user);
            existing.setOverallStatus("NOT_STARTED");
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

            var result = service.startVerification(user);
            assertEquals("IN_PROGRESS", result.get("overallStatus"));
        }

        @Test
        void doesNotRegressAlreadyInProgress() {
            UserIdentityCompliance existing = new UserIdentityCompliance();
            existing.setId(10L);
            existing.setUser(user);
            existing.setSessionId("s1");
            existing.setOverallStatus("VERIFIED");
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

            var result = service.startVerification(user);
            assertEquals("VERIFIED", result.get("overallStatus"));
        }
    }

    // ── submitSelfie ──────────────────────────────────────────────────

    @Nested
    class SubmitSelfie {

        @Test
        void rejectsMissingSession() {
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.empty());
            assertThrows(BadRequestException.class,
                    () -> service.submitSelfie(user, 1L, true));
        }

        @Test
        void rejectsForeignDocument() {
            UserIdentityCompliance comp = makeCompliance();
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            User otherUser = new User();
            otherUser.setId(2L);
            UserIdentityCompliance otherComp = new UserIdentityCompliance();
            otherComp.setUser(otherUser);
            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(77L);
            blob.setCompliance(otherComp);
            blob.setDocPurpose("SELFIE");
            when(blobRepository.findById(77L)).thenReturn(Optional.of(blob));

            assertThrows(BadRequestException.class,
                    () -> service.submitSelfie(user, 77L, true));
        }

        @Test
        void rejectsNonSelfieDocument() {
            UserIdentityCompliance comp = makeCompliance();
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(88L);
            blob.setCompliance(comp);
            blob.setDocPurpose("DL_FRONT");
            when(blobRepository.findById(88L)).thenReturn(Optional.of(blob));

            assertThrows(BadRequestException.class,
                    () -> service.submitSelfie(user, 88L, true));
        }

        @Test
        void rejectsMissingDlFront() {
            UserIdentityCompliance comp = makeCompliance();
            comp.setDlFrontBlobId(null);
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(88L);
            blob.setCompliance(comp);
            blob.setDocPurpose("SELFIE");
            when(blobRepository.findById(88L)).thenReturn(Optional.of(blob));

            assertThrows(BadRequestException.class,
                    () -> service.submitSelfie(user, 88L, true));
        }

        @Test
        void verifiedSetsPhotoStatusAndBadge() {
            UserIdentityCompliance comp = makeCompliance();
            comp.setDlFrontBlobId(55L);
            comp.setOverallStatus("NOT_STARTED");
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(88L);
            blob.setCompliance(comp);
            blob.setDocPurpose("SELFIE");
            when(blobRepository.findById(88L)).thenReturn(Optional.of(blob));

            var result = service.submitSelfie(user, 88L, true);
            assertEquals("VERIFIED", result.get("photoStatus"));
            assertEquals("IN_PROGRESS", result.get("overallStatus"));
            assertTrue(profile.getShowVerificationBadge());
        }

        @Test
        void mismatchSetsPhotoStatusMismatch() {
            UserIdentityCompliance comp = makeCompliance();
            comp.setDlFrontBlobId(55L);
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(88L);
            blob.setCompliance(comp);
            blob.setDocPurpose("SELFIE");
            when(blobRepository.findById(88L)).thenReturn(Optional.of(blob));

            var result = service.submitSelfie(user, 88L, false);
            assertEquals("MISMATCH", result.get("photoStatus"));
        }

        @Test
        void nullDocumentIdSetsInProgress() {
            UserIdentityCompliance comp = makeCompliance();
            comp.setOverallStatus("IN_PROGRESS");
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            var result = service.submitSelfie(user, null, false);
            assertEquals("IN_PROGRESS", result.get("photoStatus"));
        }

        @Test
        void rejectsDocumentIdNotFound() {
            UserIdentityCompliance comp = makeCompliance();
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));
            when(blobRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> service.submitSelfie(user, 999L, true));
        }

        @Test
        void sessionIdEmptyStringWhenNull() {
            UserIdentityCompliance comp = makeCompliance();
            comp.setSessionId(null);
            comp.setOverallStatus("IN_PROGRESS");
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(comp));

            var result = service.submitSelfie(user, null, false);
            assertEquals("", result.get("sessionId"));
        }

        private UserIdentityCompliance makeCompliance() {
            UserIdentityCompliance c = new UserIdentityCompliance();
            c.setId(10L);
            c.setUser(user);
            c.setSessionId("session-1");
            c.setOverallStatus("IN_PROGRESS");
            return c;
        }
    }

    // ── handleVerificationWebhook ─────────────────────────────────────

    @Nested
    class HandleVerificationWebhook {

        @Test
        void rejectsNullBody() {
            assertThrows(BadRequestException.class,
                    () -> service.handleVerificationWebhook(null));
        }

        @Test
        void rejectsEmptyBody() {
            assertThrows(BadRequestException.class,
                    () -> service.handleVerificationWebhook(Map.of()));
        }

        @Test
        void rejectsMissingSessionId() {
            assertThrows(BadRequestException.class,
                    () -> service.handleVerificationWebhook(Map.of("status", "VERIFIED")));
        }

        @Test
        void rejectsBlankSessionId() {
            assertThrows(BadRequestException.class,
                    () -> service.handleVerificationWebhook(Map.of("sessionId", "  ")));
        }

        @Test
        void rejectsUnknownSession() {
            when(complianceRepository.findBySessionId("unknown-id")).thenReturn(Optional.empty());
            assertThrows(BadRequestException.class,
                    () -> service.handleVerificationWebhook(Map.of("sessionId", "unknown-id")));
        }

        @Test
        void acceptsSessionIdCamelCase() {
            UserIdentityCompliance comp = makeWebhookCompliance("sess-1");
            when(complianceRepository.findBySessionId("sess-1")).thenReturn(Optional.of(comp));

            var result = service.handleVerificationWebhook(Map.of(
                    "sessionId", "sess-1", "status", "VERIFIED"
            ));
            assertEquals("VERIFIED", result.get("overallStatus"));
        }

        @Test
        void acceptsSessionIdSnakeCase() {
            UserIdentityCompliance comp = makeWebhookCompliance("sess-2");
            when(complianceRepository.findBySessionId("sess-2")).thenReturn(Optional.of(comp));

            var result = service.handleVerificationWebhook(Map.of(
                    "session_id", "sess-2", "status", "VERIFIED"
            ));
            assertEquals("VERIFIED", result.get("overallStatus"));
        }

        @Test
        void updatesAllStatusFields() {
            UserIdentityCompliance comp = makeWebhookCompliance("sess-3");
            when(complianceRepository.findBySessionId("sess-3")).thenReturn(Optional.of(comp));

            Map<String, Object> body = new HashMap<>();
            body.put("sessionId", "sess-3");
            body.put("photoStatus", "verified");
            body.put("nameStatus", "verified");
            body.put("ageStatus", "verified");
            body.put("overallStatus", "completed");

            var result = service.handleVerificationWebhook(body);
            assertEquals("VERIFIED", result.get("photoStatus"));
            assertEquals("COMPLETED", result.get("overallStatus"));
        }

        @Test
        void usesOverallStatusFallback() {
            UserIdentityCompliance comp = makeWebhookCompliance("sess-4");
            when(complianceRepository.findBySessionId("sess-4")).thenReturn(Optional.of(comp));

            Map<String, Object> body = new HashMap<>();
            body.put("sessionId", "sess-4");
            body.put("overallStatus", "FAILED");

            var result = service.handleVerificationWebhook(body);
            assertEquals("FAILED", result.get("overallStatus"));
        }

        @Test
        void noStatusFieldsDoesNotCrash() {
            UserIdentityCompliance comp = makeWebhookCompliance("sess-5");
            when(complianceRepository.findBySessionId("sess-5")).thenReturn(Optional.of(comp));

            Map<String, Object> body = new HashMap<>();
            body.put("sessionId", "sess-5");

            var result = service.handleVerificationWebhook(body);
            assertEquals("NOT_STARTED", result.get("overallStatus"));
        }

        private UserIdentityCompliance makeWebhookCompliance(String sessionId) {
            UserIdentityCompliance c = new UserIdentityCompliance();
            c.setId(10L);
            c.setUser(user);
            c.setSessionId(sessionId);
            c.setOverallStatus("NOT_STARTED");
            return c;
        }
    }

    // ── streamDocument ────────────────────────────────────────────────

    @Nested
    class StreamDocument {

        @Test
        void rejectsNullId() {
            assertThrows(BadRequestException.class,
                    () -> service.streamDocument(user, null));
        }

        @Test
        void rejectsNotFound() {
            when(blobRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> service.streamDocument(user, 999L));
        }

        @Test
        void ownerCanStream() {
            UserIdentityComplianceBlob blob = makeBlob(user, "image/jpeg", new byte[]{1, 2});
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            var result = service.streamDocument(user, 1L);
            assertEquals("image/jpeg", result.contentType());
            assertArrayEquals(new byte[]{1, 2}, result.bytes());
        }

        @Test
        void adminCanViewOtherUsersDoc() {
            User owner = new User();
            owner.setId(2L);
            owner.setRole(Role.USER);
            User admin = new User();
            admin.setId(3L);
            admin.setRole(Role.ADMIN);

            UserIdentityCompliance ownerComp = new UserIdentityCompliance();
            ownerComp.setId(11L);
            ownerComp.setUser(owner);
            lenient().when(complianceRepository.findByUserId(2L)).thenReturn(Optional.of(ownerComp));
            lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> inv.getArgument(0));

            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(1L);
            blob.setCompliance(ownerComp);
            blob.setContentType("image/png");
            blob.setStorageBlob(new byte[]{5});
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            var result = service.streamDocument(admin, 1L);
            assertEquals("image/png", result.contentType());
        }

        @Test
        void nonOwnerNonAdminRejected() {
            User owner = new User();
            owner.setId(2L);
            owner.setRole(Role.USER);
            UserIdentityCompliance ownerComp = new UserIdentityCompliance();
            ownerComp.setUser(owner);
            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(1L);
            blob.setCompliance(ownerComp);
            blob.setContentType("image/png");
            blob.setStorageBlob(new byte[]{5});
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            assertThrows(ResourceNotFoundException.class,
                    () -> service.streamDocument(user, 1L));
        }

        @Test
        void rejectsEmptyBlob() {
            UserIdentityComplianceBlob blob = makeBlob(user, "image/jpeg", new byte[]{});
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            assertThrows(ResourceNotFoundException.class,
                    () -> service.streamDocument(user, 1L));
        }

        @Test
        void rejectsNullBlob() {
            UserIdentityComplianceBlob blob = makeBlob(user, "image/jpeg", null);
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            assertThrows(ResourceNotFoundException.class,
                    () -> service.streamDocument(user, 1L));
        }

        @Test
        void blankContentTypeFallsBackToOctetStream() {
            UserIdentityComplianceBlob blob = makeBlob(user, "  ", new byte[]{1});
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            var result = service.streamDocument(user, 1L);
            assertEquals("application/octet-stream", result.contentType());
        }

        @Test
        void nullContentTypeFallsBackToOctetStream() {
            UserIdentityComplianceBlob blob = makeBlob(user, null, new byte[]{1});
            when(blobRepository.findById(1L)).thenReturn(Optional.of(blob));

            var result = service.streamDocument(user, 1L);
            assertEquals("application/octet-stream", result.contentType());
        }

        private UserIdentityComplianceBlob makeBlob(User owner, String contentType, byte[] data) {
            UserIdentityCompliance comp = new UserIdentityCompliance();
            comp.setId(10L);
            comp.setUser(owner);
            lenient().when(complianceRepository.findByUserId(owner.getId())).thenReturn(Optional.of(comp));
            lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> inv.getArgument(0));
            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setId(1L);
            blob.setCompliance(comp);
            blob.setContentType(contentType);
            blob.setStorageBlob(data);
            return blob;
        }
    }

    // ── uploadDocument ────────────────────────────────────────────────

    @Nested
    class UploadDocument {

        @Test
        void rejectsNullFile() {
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, null, "DL_FRONT"));
        }

        @Test
        void rejectsEmptyFile() {
            MockMultipartFile file = new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "DL_FRONT"));
        }

        @Test
        void rejectsOversizeFile() {
            byte[] big = new byte[(int) (5 * 1024 * 1024 + 1)];
            MockMultipartFile file = new MockMultipartFile("file", "f.jpg", "image/jpeg", big);
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "DL_FRONT"));
        }

        @Test
        void rejectsNullPurpose() {
            MockMultipartFile file = new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{1});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, null));
        }

        @Test
        void rejectsBlankPurpose() {
            MockMultipartFile file = new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{1});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "  "));
        }

        @Test
        void rejectsUnsupportedPurpose() {
            MockMultipartFile file = new MockMultipartFile("file", "f.jpg", "image/jpeg", new byte[]{1});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "PASSPORT"));
        }

        @Test
        void idBackMapsToDlBack() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "id.png", "image/png", new byte[]{1});
            var result = service.uploadDocument(user, file, "ID_BACK");
            assertEquals("DL_BACK", result.get("docPurpose"));
        }

        @Test
        void rejectsNonImageForDlFront() {
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "DL_FRONT"));
        }

        @Test
        void rejectsNonImageForDlBack() {
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "DL_BACK"));
        }

        @Test
        void rejectsNonImageForSelfie() {
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, file, "SELFIE"));
        }

        @Test
        void blankContentTypeFallsBackToOctetStream() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "f.dat", "", new byte[]{1});
            var result = service.uploadDocument(user, file, "SAFETY_SUPPORT");
            assertEquals("application/octet-stream", result.get("contentType"));
        }

        @Test
        void selfieUploadSuccess() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "s.jpg", "image/jpeg", new byte[]{1, 2, 3});
            var result = service.uploadDocument(user, file, "SELFIE");
            assertEquals("SELFIE", result.get("docPurpose"));
            assertEquals(55L, result.get("id"));
        }

        @Test
        void dlBackSetsBackBlobId() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "back.jpg", "image/jpeg", new byte[]{1});
            service.uploadDocument(user, file, "DL_BACK");
            verify(complianceRepository, atLeastOnce()).save(argThat(c ->
                    c instanceof UserIdentityCompliance && Long.valueOf(55L).equals(((UserIdentityCompliance)c).getDlBackBlobId())
            ));
        }

        @Test
        void ioExceptionThrowsBadRequest() throws IOException {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(100L);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getBytes()).thenThrow(new IOException("disk error"));

            assertThrows(BadRequestException.class,
                    () -> service.uploadDocument(user, mockFile, "DL_FRONT"));
        }

        @Test
        void supportPurposeAllowsNonImageContentType() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            var result = service.uploadDocument(user, file, "SAFETY_SUPPORT");
            assertEquals("SAFETY_SUPPORT", result.get("docPurpose"));
        }

        @Test
        void eduVerifyPurposeWorks() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            var result = service.uploadDocument(user, file, "EDU_VERIFY");
            assertEquals("EDU_VERIFY", result.get("docPurpose"));
        }

        @Test
        void empVerifyPurposeWorks() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            var result = service.uploadDocument(user, file, "EMP_VERIFY");
            assertEquals("EMP_VERIFY", result.get("docPurpose"));
        }

        @Test
        void civilSupportPurposeWorks() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
            var result = service.uploadDocument(user, file, "CIVIL_SUPPORT");
            assertEquals("CIVIL_SUPPORT", result.get("docPurpose"));
        }

        @Test
        void webpAcceptedForDl() {
            stubBlobSave();
            MockMultipartFile file = new MockMultipartFile("file", "f.webp", "image/webp", new byte[]{1});
            var result = service.uploadDocument(user, file, "DL_FRONT");
            assertEquals("image/webp", result.get("contentType"));
        }
    }

    // ── saveIdentity ──────────────────────────────────────────────────

    @Nested
    class SaveIdentity {

        @Test
        void rejectsUnknownAction() {
            IdentityBackgroundRequest req = minimalRequest("PUBLISH");
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void rejectsNullAction() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    null, null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void continueMissingFirstNameRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, null, null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void continueMissingLastNameRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, null, null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void continueMissingDobRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void futureDobRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.now().plusDays(1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void todayDobRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.now(), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void underageRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.now().minusYears(17), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(UnprocessableEntityException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void saveLaterWithAllSections() {
            stubAcceptedConsent();
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main St", "Apt 2", "3B", "Austin", "TX", "78701", "US",
                    "RENT", 6, 2020, null, "YES", 50, List.of("Dallas", "Houston")
            );
            List<PreviousAddressDto> previousAddresses = List.of(
                    new PreviousAddressDto(null, "NYC", "NY", "US", "10001",
                            1, 2018, 5, 2020, "Job")
            );
            NationalityDto nationality = new NationalityDto(
                    "US", "US", "US", List.of(), List.of(
                    new LanguageDto("en", "NATIVE"), new LanguageDto("es", "INTERMEDIATE")
            ));
            ImmigrationDto immigration = new ImmigrationDto(
                    "US", "CITIZEN", "NO", "NO", "YES", List.of("US")
            );
            RelationshipDto relationship = new RelationshipDto(
                    "NEVER_MARRIED", 0, 0, null, null, null, null, null, null, "MONOGAMY"
            );
            FamilyDto family = new FamilyDto(
                    "NO", 0, List.of(), null, null, "YES", "YES", 2,
                    "NO", "NO", null, null, "1 dog"
            );
            List<EducationDto> educations = List.of(
                    new EducationDto(null, "BACHELOR", "BS", "CS", "MIT", "Cambridge", "US",
                            2014, 2018, false, "Magna Cum Laude", true, null)
            );
            CareerDto career = new CareerDto(
                    "EMPLOYEE", "Software Engineer", "Technology", "Senior",
                    "500+", "REMOTE", "FULL_TIME", null, 10, "HIGH",
                    "OCCASIONALLY", "OPEN", "Tech lead", "BALANCED", "Acme Inc", true
            );
            FinancialDto financial = new FinancialDto(
                    "100K_149K", "740_799", "50K_99K", "RENTER", "NONE", "NONE",
                    "Retirement", "Regular", "Moderate", "HIGH",
                    "OPEN", "OPEN", "50/50", null
            );
            SafetyDto safety = new SafetyDto(
                    "NO", "NO", "NO", "NO", "NO", "TX", 2020, true, null
            );
            CivilJudgmentDto civilJudgment = new CivilJudgmentDto(
                    "NO", null, null, null, null, null
            );

            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", "Mr.", "Ada", "Marie", "Lovelace", "Jr.", "Addie",
                    LocalDate.of(1990, 5, 15), "She/Her", "Woman", "MATCHES",
                    "ada2@example.com", "+919876543211", "+919876543212", "+919876543213",
                    "EMAIL", "MORNING",
                    residence, previousAddresses, nationality, immigration, relationship,
                    family, educations, career, financial, safety, civilJudgment,
                    "PRIVATE", null
            );

            IdentityBackgroundResponse res = service.saveIdentity(user, req);
            assertNotNull(res);
            assertEquals("Addie", res.displayName());
            assertEquals(OnboardingSteps.IDENTITY_IN_PROGRESS, profile.getOnboardingStep());
            assertNotNull(res.currentResidence());
            assertEquals("Austin", res.currentResidence().city());
        }

        @Test
        void continueWithRequiredSections() {
            stubAcceptedConsent();
            IdentityBackgroundResponse res = service.saveIdentity(user, continueRequest(LocalDate.of(1990, 1, 1)));
            assertTrue(res.identityPage1Complete());
            assertEquals(OnboardingSteps.PERSONALITY_IN_PROGRESS, profile.getOnboardingStep());
        }

        @Test
        void saveLaterDoesNotMoveToPastIdentityCompleted() {
            stubAcceptedConsent();
            profile.setOnboardingStep(OnboardingSteps.PERSONALITY_IN_PROGRESS);
            profile.setIdentityPage1CompletedAt(LocalDateTime.now());

            IdentityBackgroundRequest req = minimalRequest("SAVE_LATER");
            service.saveIdentity(user, req);
            assertEquals(OnboardingSteps.PERSONALITY_IN_PROGRESS, profile.getOnboardingStep());
        }

        @Test
        void ssnWithStarsIsIgnored() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    "***-**-6789"
            );
            service.saveIdentity(user, req);
            verify(complianceRepository, atLeastOnce()).save(argThat(c ->
                    c instanceof UserIdentityCompliance && ((UserIdentityCompliance) c).getSsn() == null
            ));
        }

        @Test
        void incomeRangeSharePreferenceApplied() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    "MATCHES", null
            );
            service.saveIdentity(user, req);
            assertEquals("MATCHES", profile.getIncomeRangeSharePreference());
        }

        @Test
        void invalidIncomeRangeSharePreferenceRejects() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    "INVALID_PREF", null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void phoneDiffResetsVerification() {
            stubAcceptedConsent();
            user.setPhoneVerified(true);
            user.setPhoneNumber("+919876543210");
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, "+919876543299", null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertFalse(user.isPhoneVerified());
        }

        @Test
        void samePhoneDoesNotResetVerification() {
            stubAcceptedConsent();
            user.setPhoneVerified(true);
            user.setPhoneNumber("+91-9876543210");
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, "+919876543210", null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertTrue(user.isPhoneVerified());
        }

        @Test
        void secondaryEmailNormalizedToLowerCase() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    "ADA2@EXAMPLE.COM", null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertEquals("ada2@example.com", user.getSecondaryEmail());
        }

        @Test
        void suffixNoneNormalizesToNull() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", "None", null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertNull(user.getNameSuffix());
        }

        @Test
        void genderShownDefaultsToMatches() {
            stubAcceptedConsent();
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertEquals("MATCHES", profile.getGenderShownToMatches());
        }
    }

    // ── Validation rejections ─────────────────────────────────────────

    @Nested
    class ValidationRejections {

        @BeforeEach
        void acceptConsent() {
            stubAcceptedConsent();
        }

        @Test
        void invalidPostalCodeRejects() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "Austin", "TX", "BADZIP", "US",
                    null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    residence, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidMaritalStatusRejects() {
            RelationshipDto relationship = new RelationshipDto(
                    "COHABITING", null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, relationship, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void divorcesExceedMarriagesRejects() {
            RelationshipDto relationship = new RelationshipDto(
                    "DIVORCED", 1, 3, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, relationship, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidResidenceTypeRejects() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "Austin", "TX", "78701", "US",
                    "CASTLE", null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    residence, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidRelocatePrefRejects() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "Austin", "TX", "78701", "US",
                    null, null, null, null, "ABSOLUTELY", null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    residence, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidMoveInMonthRejects() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "Austin", "TX", "78701", "US",
                    null, 13, 2020, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    residence, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void previousAddressInvalidDateRangeRejects() {
            List<PreviousAddressDto> addresses = List.of(
                    new PreviousAddressDto(null, "NYC", "NY", null, null,
                            6, 2022, 1, 2020, null)
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, addresses, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void duplicateNationalityRejects() {
            NationalityDto nationality = new NationalityDto(
                    "US", "US", "US", List.of("US"), null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, nationality, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void duplicateAdditionalNationalitiesReject() {
            NationalityDto nationality = new NationalityDto(
                    null, null, null, List.of("IN", "IN"), null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, nationality, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidResidencyCategoryRejects() {
            ImmigrationDto immigration = new ImmigrationDto(
                    "US", "TOURIST", null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, immigration, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidFamilyHasChildrenRejects() {
            FamilyDto family = new FamilyDto(
                    "MAYBE_LATER", null, null, null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, family, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidChildAgeRangeRejects() {
            FamilyDto family = new FamilyDto(
                    "YES", 1, List.of("INVALID_RANGE"), null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, family, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidEducationLevelRejects() {
            List<EducationDto> educations = List.of(
                    new EducationDto(null, "PHD_PLUS", null, null, "MIT",
                            null, null, null, null, null, null, null, null)
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, educations, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void educationGraduationBeforeStartRejects() {
            List<EducationDto> educations = List.of(
                    new EducationDto(null, "BACHELOR", null, null, "MIT",
                            null, null, 2020, 2018, null, null, null, null)
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, educations, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void educationWithNullInstitutionSkipped() {
            List<EducationDto> educations = List.of(
                    new EducationDto(null, null, null, null, null,
                            null, null, null, null, null, null, null, null),
                    new EducationDto(null, "BACHELOR", "BS", "CS", "MIT",
                            null, null, 2014, 2018, false, null, true, null)
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, educations, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertEquals(1, profile.getEducations().size());
        }

        @Test
        void invalidEmploymentStatusRejects() {
            CareerDto career = new CareerDto(
                    "FREELANCER", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, career, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidIncomeRangeRejects() {
            FinancialDto financial = new FinancialDto(
                    "1M_PLUS", null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, financial, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidCreditScoreRangeRejects() {
            FinancialDto financial = new FinancialDto(
                    null, "PERFECT", null, null, null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, financial, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidSafetyCriminalConvictionRejects() {
            SafetyDto safety = new SafetyDto(
                    "MAYBE", null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, safety, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidCivilJudgmentHasJudgmentRejects() {
            CivilJudgmentDto civilJudgment = new CivilJudgmentDto(
                    "MAYBE", null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, civilJudgment, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidPronounsRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, "Xe/Xem", null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidGenderRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, "Alien", null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidGenderVisibilityRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, "EVERYONE",
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidPrefixRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", "Prof.", "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidSuffixRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", "IV", null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidContactMethodRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, "PIGEON", null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidBestTimeRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, "MIDNIGHT",
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void nameExceedsMaxLengthRejects() {
            String longName = "A".repeat(61);
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, longName, null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void unsupportedCountryCodeForPostalCodeRejects() {
            when(refCountryRepository.findById("XX")).thenReturn(Optional.empty());
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "City", "ST", "12345", "XX",
                    null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    residence, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidImmigrationRelocationPrefRejects() {
            ImmigrationDto immigration = new ImmigrationDto(
                    null, null, "MAYBE", null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, immigration, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidSavingsRangeRejects() {
            FinancialDto financial = new FinancialDto(
                    null, null, "BILLIONS", null, null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, financial, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidHousingStatusRejects() {
            FinancialDto financial = new FinancialDto(
                    null, null, null, "HOMELESS", null, null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, financial, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidDebtRangeRejects() {
            FinancialDto financial = new FinancialDto(
                    null, null, null, null, "MILLIONS", null, null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, financial, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void invalidStudentLoanRangeRejects() {
            FinancialDto financial = new FinancialDto(
                    null, null, null, null, null, "MILLIONS", null, null, null, null, null, null, null, null
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, financial, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void emptyPreviousAddressesClearsField() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, List.of(), null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            verify(backgroundRepository, atLeastOnce()).save(any(UserIdentityBackground.class));
        }

        @Test
        void previousAddressZeroMonthRejects() {
            List<PreviousAddressDto> addresses = List.of(
                    new PreviousAddressDto(null, "NYC", "NY", null, null,
                            0, 2020, 6, 2022, null)
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, addresses, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }
    }

    // ── maskSsn / normalizeSsn ────────────────────────────────────────

    @Nested
    class MaskAndNormalizeSsn {

        @Test
        void maskSsnNull() {
            assertNull(IdentityOnboardingService.maskSsn(null));
        }

        @Test
        void maskSsnShortString() {
            assertNull(IdentityOnboardingService.maskSsn("12"));
        }

        @Test
        void maskSsnThreeChars() {
            assertNull(IdentityOnboardingService.maskSsn("abc"));
        }

        @Test
        void maskSsnExactlyFourChars() {
            assertEquals("***-**-6789", IdentityOnboardingService.maskSsn("6789"));
        }

        @Test
        void maskSsnFullFormat() {
            assertEquals("***-**-6789", IdentityOnboardingService.maskSsn("123-45-6789"));
        }

        @Test
        void normalizeSsnRejectsNonNineDigits() {
            assertThrows(BadRequestException.class,
                    () -> IdentityOnboardingService.normalizeSsn("12345"));
        }

        @Test
        void normalizeSsnRejectsTenDigits() {
            assertThrows(BadRequestException.class,
                    () -> IdentityOnboardingService.normalizeSsn("1234567890"));
        }

        @Test
        void normalizeSsnWithDashes() {
            assertEquals("123-45-6789", IdentityOnboardingService.normalizeSsn("123-45-6789"));
        }

        @Test
        void normalizeSsnWithSpaces() {
            assertEquals("123-45-6789", IdentityOnboardingService.normalizeSsn("123 45 6789"));
        }
    }

    // ── continueRequiresSections ───────────────────────────────────────

    @Nested
    class ContinueRequiresSections {

        @BeforeEach
        void acceptConsent() {
            stubAcceptedConsent();
        }

        @Test
        void continueMissingResidenceRejects() {
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.empty());
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null,
                    new RelationshipDto("NEVER_MARRIED", null, null, null, null, null, null, null, null, null),
                    null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void continueMissingRelationshipRejects() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    new CurrentResidenceDto("1 Main", null, null, "Austin", "TX", "78701", "US",
                            null, null, null, null, null, null, null),
                    null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void continueIncompleteResidenceRejects() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setLine1("1 Main St");
            bg.setCity(null);
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    new CurrentResidenceDto("1 Main", null, null, null, "TX", "78701", "US",
                            null, null, null, null, null, null, null),
                    null, null, null,
                    new RelationshipDto("NEVER_MARRIED", null, null, null, null, null, null, null, null, null),
                    null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }
    }

    // ── Edge cases for response DTO mapping ───────────────────────────

    @Nested
    class ResponseMapping {

        @Test
        void educationsReturnedFromProfile() {
            Education edu = new Education();
            edu.setId(1L);
            edu.setProfile(profile);
            edu.setInstitution("MIT");
            edu.setDegree("BS");
            edu.setFieldOfStudy("CS");
            edu.setStartYear(2014);
            edu.setEndYear(2018);
            edu.setEducationLevel("BACHELOR");
            edu.setCurrentlyStudying(false);
            edu.setShowInstitutionPublicly(true);
            profile.getEducations().add(edu);

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertEquals(1, res.educations().size());
            assertEquals("MIT", res.educations().get(0).institution());
        }

        @Test
        void previousAddressesFromBackground() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setLine1("1 Main St");
            bg.setPreviousAddresses("""
                    [{"city":"NYC","stateRegion":"NY","countryCode":"US","postalCode":"10001","fromMonth":1,"fromYear":2018,"toMonth":5,"toYear":2020,"reasonForMoving":"Job"}]
                    """.trim());
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertEquals(1, res.previousAddresses().size());
            assertEquals("NYC", res.previousAddresses().get(0).city());
        }

        @Test
        void languagesReturnedInNationality() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setPrimaryNationality("US");
            bg.setLanguages("""
                    [{"languageCode":"en","proficiency":"NATIVE"}]
                    """.trim());
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.nationality());
            assertEquals(1, res.nationality().languages().size());
            assertEquals("en", res.nationality().languages().get(0).languageCode());
        }

        @Test
        void careerSetsProfileOccupation() {
            stubAcceptedConsent();
            CareerDto career = new CareerDto(
                    "EMPLOYEE", "Engineer", null, null, null, null, null, null, null, null, null, null, null, null, "Acme", true
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, career, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertEquals("Engineer", profile.getOccupation());
            assertTrue(profile.getEmployerNamePubliclyAllowed());
        }

        @Test
        void residenceDurationCalculated() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setLine1("1 Main St");
            bg.setMoveInMonth(1);
            bg.setMoveInYear(2020);
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.currentResidence());
            assertNotNull(res.currentResidence().residenceDurationMonths());
            assertTrue(res.currentResidence().residenceDurationMonths() > 0);
        }

        @Test
        void preferredRelocateLocationsInResidence() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setLine1("1 Main St");
            bg.setPreferredRelocateLocations("[\"Dallas\",\"Houston\"]");
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.currentResidence());
            assertEquals(2, res.currentResidence().preferredFutureLocations().size());
        }

        @Test
        void immigrationPreferredFutureCountriesReturned() {
            UserIdentityBackground bg = new UserIdentityBackground();
            bg.setId(20L);
            bg.setUser(user);
            bg.setCurrentCountryOfResidence("US");
            bg.setPreferredFutureCountries("[\"US\",\"IN\"]");
            when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.immigration());
            assertEquals(2, res.immigration().preferredFutureCountries().size());
        }

        @Test
        void familyChildAgeRangesReturned() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setHasChildren("YES");
            life.setChildAgeRanges("[\"0_2\",\"3_5\"]");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.family());
            assertEquals(2, res.family().childAgeRanges().size());
        }
    }

    // ── Full career / financial / safety life profile data ────────────

    @Nested
    class LifeProfileDataPaths {

        @Test
        void fullCareerLifeProfileFields() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setEmploymentStatus("EMPLOYEE");
            life.setJobFunction("Engineer");
            life.setIndustry("Tech");
            life.setSeniority("Senior");
            life.setCompanySize("500+");
            life.setWorkArrangement("REMOTE");
            life.setWorkSchedule("FULL_TIME");
            life.setSelfEmploymentCategory("CONSULTING");
            life.setYearsInProfession(10);
            life.setCareerSatisfaction("HIGH");
            life.setTravelFrequency("MONTHLY");
            life.setRelocationPossibility("OPEN");
            life.setCareerAmbitions("CTO");
            life.setWorkLifeBalancePref("BALANCED");
            life.setEmployerName("Acme");
            life.setShowEmployerPublicly(true);
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            CareerDto career = res.career();
            assertNotNull(career);
            assertEquals("Engineer", career.jobFunction());
            assertEquals("Tech", career.industry());
            assertTrue(career.showEmployerPublicly());
        }

        @Test
        void fullFinancialLifeProfileFields() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setIncomeRange("100K_149K");
            life.setCreditScoreRange("740_799");
            life.setSavingsRange("50K_99K");
            life.setHousingStatus("HOMEOWNER");
            life.setGeneralDebtRange("NONE");
            life.setStudentLoanRange("UNDER_10K");
            life.setFinancialGoals("Retire early");
            life.setSavingsHabits("Regular");
            life.setSpendingStyle("Moderate");
            life.setBudgetConsciousness("HIGH");
            life.setJointFinancePref("OPEN");
            life.setSeparateFinancePref("OPEN");
            life.setHouseholdContributionExpectation("50/50");
            life.setExtendedFamilySupportPref("NO");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            FinancialDto fin = res.financial();
            assertNotNull(fin);
            assertEquals("100K_149K", fin.incomeRange());
            assertEquals("50/50", fin.householdContributionExpectation());
        }

        @Test
        void fullSafetyLifeProfileFields() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setCriminalConviction("NO");
            life.setPendingCriminalCases("NO");
            life.setProtectiveRestrainingOrder("NO");
            life.setDvStalkingSexualOffense("NO");
            life.setGovernmentOffenderRegistry("NO");
            life.setSafetyJurisdiction("TX");
            life.setSafetyApproxYear(2020);
            life.setSafetyCaseResolved(true);
            life.setSafetyExplanation("None");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            SafetyDto safety = res.safety();
            assertNotNull(safety);
            assertEquals("NO", safety.criminalConviction());
            assertEquals(2020, safety.approxYear());
        }

        @Test
        void fullCivilJudgmentFields() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setHasJudgment("YES");
            life.setCivilCategories("Contract");
            life.setCivilJurisdiction("NY");
            life.setCivilApproxYear(2019);
            life.setCivilResolved(true);
            life.setCivilExplanation("Settled out of court");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            CivilJudgmentDto civil = res.civilJudgment();
            assertNotNull(civil);
            assertEquals("YES", civil.hasJudgment());
            assertEquals("Contract", civil.categories());
            assertTrue(civil.resolved());
        }

        @Test
        void fullRelationshipFields() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setMaritalStatus("DIVORCED");
            life.setPreviousMarriagesCount(2);
            life.setDivorcesCount(1);
            life.setAnnulmentsCount(0);
            life.setCurrentlySeparated(false);
            life.setDivorceFinalized(true);
            life.setMostRecentDivorceYear(2022);
            life.setCoParenting("YES");
            life.setUnresolvedCommitments("NONE");
            life.setRelationshipModelPref("MONOGAMY");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            RelationshipDto rel = res.relationship();
            assertNotNull(rel);
            assertEquals("DIVORCED", rel.maritalStatus());
            assertEquals(2, rel.previousMarriagesCount());
            assertEquals("YES", rel.coParenting());
        }

        @Test
        void fullFamilyFields() {
            UserLifeProfile life = new UserLifeProfile();
            life.setId(30L);
            life.setUser(user);
            life.setHasChildren("YES");
            life.setChildrenCount(2);
            life.setChildrenLiveWithUser("YES");
            life.setCustodyArrangement("FULL");
            life.setFutureChildrenPref("YES");
            life.setOpenToPartnerWithChildren("YES");
            life.setPreferredFutureChildrenCount(1);
            life.setAdoptionPref("NO");
            life.setFosterPref("NO");
            life.setElderCare("YES");
            life.setOtherDependents("Elderly parent");
            life.setPetsInfo("2 cats");
            life.setChildAgeRanges("[\"3_5\",\"6_9\"]");
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));

            IdentityBackgroundResponse res = service.getIdentity(user);
            FamilyDto fam = res.family();
            assertNotNull(fam);
            assertEquals("YES", fam.hasChildren());
            assertEquals(2, fam.childrenCount());
            assertEquals("2 cats", fam.petsInfo());
            assertEquals(2, fam.childAgeRanges().size());
        }
    }
}
