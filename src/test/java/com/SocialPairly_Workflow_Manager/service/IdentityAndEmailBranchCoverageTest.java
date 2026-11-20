package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;
import com.SocialPairly_Workflow_Manager.dto.SupportAttachment;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityAndEmailBranchCoverageTest {

    private UserRepository userRepo;
    private UserProfileRepository profileRepo;
    private RefCountryRepository countryRepo;
    private UserIdentityBackgroundRepository bgRepo;
    private UserLifeProfileRepository lifeRepo;
    private UserIdentityComplianceRepository compRepo;
    private UserIdentityComplianceBlobRepository blobRepo;
    private IdentityOnboardingService identityService;
    private User user;
    private UserProfile profile;
    private UserIdentityBackground bg;
    private UserLifeProfile life;
    private UserIdentityCompliance comp;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        profileRepo = mock(UserProfileRepository.class);
        countryRepo = mock(RefCountryRepository.class);
        bgRepo = mock(UserIdentityBackgroundRepository.class);
        lifeRepo = mock(UserLifeProfileRepository.class);
        compRepo = mock(UserIdentityComplianceRepository.class);
        blobRepo = mock(UserIdentityComplianceBlobRepository.class);

        identityService = new IdentityOnboardingService(
                userRepo, profileRepo, countryRepo, bgRepo, lifeRepo, compRepo, blobRepo,
                new ObjectMapper(), "+91");

        user = new User();
        user.setId(100L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");

        profile = new UserProfile();
        profile.setUser(user);
        profile.setDateOfBirth(LocalDate.of(1995, 5, 10));

        bg = new UserIdentityBackground();
        bg.setUser(user);

        life = new UserLifeProfile();
        life.setUser(user);

        comp = new UserIdentityCompliance();
        comp.setUser(user);

        when(profileRepo.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(bgRepo.findByUserId(100L)).thenReturn(Optional.of(bg));
        when(lifeRepo.findByUserId(100L)).thenReturn(Optional.of(life));
        when(compRepo.findByUserId(100L)).thenReturn(Optional.of(comp));
        when(userRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bgRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lifeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(compRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        RefCountry in = new RefCountry();
        in.setCode("IN");
        in.setName("India");
        in.setPostalRegex("^[0-9]{6}$");
        when(countryRepo.findById("IN")).thenReturn(Optional.of(in));
    }

    private IdentityBackgroundRequest withIdDoc(String idDocType, String idDocNumber) {
        return new IdentityBackgroundRequest(
                "SAVE_LATER",
                null, null, null, null, null, null, // 6 name fields
                null, // dateOfBirth
                null, null, null, // pronouns, gender, genderShownToMatches
                null, null, null, null, null, null, // 6 contact fields
                null, null, null, null, null, null, null, null, null, null, null, // 11 section fields
                null, null, idDocType, idDocNumber);
    }

    @Test
    void resolveIdImageContentTypeCoversEveryMagicByteAndExtensionBranch() {
        assertEquals("image/png", IdentityOnboardingService.resolveIdImageContentType("x.bin", "IMAGE/PNG", null));
        assertEquals("image/jpeg", IdentityOnboardingService.resolveIdImageContentType("photo.jpg", null, null));
        assertEquals("image/jpeg", IdentityOnboardingService.resolveIdImageContentType("photo.jpeg", "application/octet-stream", null));
        assertEquals("image/png", IdentityOnboardingService.resolveIdImageContentType("photo.png", "", null));
        assertEquals("image/webp", IdentityOnboardingService.resolveIdImageContentType("photo.webp", "", null));

        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        byte[] badJpeg1 = new byte[]{0x00, (byte) 0xD8, (byte) 0xFF};
        byte[] badJpeg2 = new byte[]{(byte) 0xFF, 0x00, (byte) 0xFF};
        byte[] badJpeg3 = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00};
        assertEquals("image/jpeg", IdentityOnboardingService.resolveIdImageContentType("blob", "", jpeg));
        assertEquals("application/octet-stream", IdentityOnboardingService.resolveIdImageContentType("blob", "", badJpeg1));
        assertEquals("application/octet-stream", IdentityOnboardingService.resolveIdImageContentType("blob", "", badJpeg2));
        assertEquals("application/octet-stream", IdentityOnboardingService.resolveIdImageContentType("blob", "", badJpeg3));

        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0};
        byte[] badPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x00, 0, 0, 0, 0};
        assertEquals("image/png", IdentityOnboardingService.resolveIdImageContentType(null, "", png));
        assertEquals("application/octet-stream", IdentityOnboardingService.resolveIdImageContentType(null, "", badPng));

        byte[] webp = "RIFF1234WEBP".getBytes();
        assertEquals("image/webp", IdentityOnboardingService.resolveIdImageContentType(null, "", webp));
        // Flip each checked index in RIFF....WEBP to cover short-circuit branches on line 329
        for (int idx : new int[]{0, 1, 2, 3, 8, 9, 10, 11}) {
            byte[] mutated = Arrays.copyOf(webp, webp.length);
            mutated[idx] = 'X';
            assertEquals("application/octet-stream", IdentityOnboardingService.resolveIdImageContentType(null, "", mutated));
        }
        assertEquals("application/pdf", IdentityOnboardingService.resolveIdImageContentType(null, "application/pdf", new byte[]{1}));
    }

    @Test
    void idDocumentFamilyRelationshipEducationAndLocationBranchesCovered() {
        // ID Document branches
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user, withIdDoc("BAD_DOC", "12345678")));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user, withIdDoc("PASSPORT", "X".repeat(101))));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user, withIdDoc("STATE_ID", "1234")));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user, withIdDoc("DRIVER_LICENSE", "1234")));

        // Valid STATE_ID then number-only update using stored compliance type
        identityService.saveIdentity(user, withIdDoc("STATE_ID", "12345678"));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user, withIdDoc(null, "999")));
        identityService.saveIdentity(user, withIdDoc("PASSPORT", null));

        // Co-parenting & Family consistency branches (Bug 1592)
        String validRelModel = com.SocialPairly_Workflow_Manager.constants.IdentityReferenceEnums.RELATIONSHIP_MODEL_PREFS.get(0);
        RelationshipDto coParentYes = new RelationshipDto("DIVORCED", 1, 1, 0, false, true, 2020, "YES", null, validRelModel);
        life.setHasChildren("NO");
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(coParentYes, null, null, null, null)));
        life.setHasChildren("YES");
        life.setChildrenCount(0);
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(coParentYes, null, null, null, null)));
        life.setChildrenCount(2);
        identityService.saveIdentity(user, withSections(coParentYes, null, null, null, null));

        // Family when coParenting is already YES on life
        FamilyDto famNo = new FamilyDto("NO", 0, List.of("", "0_2"), null, null, "YES", "YES", 1, "NO", "NO", null, null, "2");
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, famNo, null, null, null)));
        FamilyDto famNullCount = new FamilyDto("YES", null, null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, famNullCount, null, null, null)));
        FamilyDto famZeroCount = new FamilyDto("YES", 0, null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, famZeroCount, null, null, null)));
        FamilyDto famTooManyPets = new FamilyDto("YES", 1, List.of("0_2"), null, null, null, null, null, null, null, null, null, "9");
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, famTooManyPets, null, null, null)));
        FamilyDto famValid = new FamilyDto("YES", 2, List.of("", "0_2"), null, null, "YES", "NO", 2, "NO", "NO", null, null, "2");
        identityService.saveIdentity(user, withSections(null, famValid, null, null, null));

        // Nationality + Immigration consistency
        IdentityOnboardingService.validateNationalityImmigrationConsistency(null, "CITIZEN", "NO", Set.of("US"));
        IdentityOnboardingService.validateNationalityImmigrationConsistency("US", null, "NO", Set.of("US"));
        IdentityOnboardingService.validateNationalityImmigrationConsistency("US", "CITIZEN", "NO", Set.of());
        assertThrows(BadRequestException.class, () ->
                IdentityOnboardingService.validateNationalityImmigrationConsistency("US", "CITIZEN", "YES", Set.of("US")));
        assertThrows(BadRequestException.class, () ->
                IdentityOnboardingService.validateNationalityImmigrationConsistency("CA", "CITIZEN", "NO", Set.of("US")));
        assertThrows(BadRequestException.class, () ->
                IdentityOnboardingService.validateNationalityImmigrationConsistency("US", "STUDENT", "NO", Set.of("US")));

        // Education date branches
        EducationDto gradBeforeStartYear = new EducationDto(null, "BACHELORS", "BS", "CS", "MIT", "Boston", "US", 9, 2022, 5, 2020, false, null, true, null);
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, List.of(gradBeforeStartYear), null, null)));
        EducationDto gradBeforeStartMonth = new EducationDto(null, "BACHELORS", "BS", "CS", "MIT", "Boston", "US", 9, 2022, 3, 2022, false, null, true, null);
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, List.of(gradBeforeStartMonth), null, null)));
        EducationDto beforeDob = new EducationDto(null, "BACHELORS", "BS", "CS", "MIT", "Boston", "US", 9, 1990, 5, 1994, false, null, true, null);
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, List.of(beforeDob), null, null)));

        // PreferredFutureLocations branches
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, null, residenceWith(List.of(
                        new PreferredFutureLocationDto("S1", List.of("C")),
                        new PreferredFutureLocationDto("S2", List.of("C")),
                        new PreferredFutureLocationDto("S3", List.of("C")),
                        new PreferredFutureLocationDto("S4", List.of("C")),
                        new PreferredFutureLocationDto("S5", List.of("C")),
                        new PreferredFutureLocationDto("S6", List.of("C"))
                )), null)));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, null, residenceWith(List.of(
                        new PreferredFutureLocationDto("  ", List.of("Austin"))
                )), null)));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, null, residenceWith(List.of(
                        new PreferredFutureLocationDto("Texas", List.of("Austin")),
                        new PreferredFutureLocationDto("texas", List.of("Dallas"))
                )), null)));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, null, residenceWith(List.of(
                        new PreferredFutureLocationDto("Texas", List.of())
                )), null)));
        assertThrows(BadRequestException.class, () -> identityService.saveIdentity(user,
                withSections(null, null, null, residenceWith(List.of(
                        new PreferredFutureLocationDto("Texas", List.of("", "Austin", "austin"))
                )), null)));

        // Legacy & malformed preferredRelocateLocations JSON on read + admin document catalog branches
        bg.setPreferredRelocateLocations("[\"Austin\", \"\"]");
        assertEquals(1, identityService.getIdentity(user).currentResidence().preferredFutureLocations().size());
        bg.setPreferredRelocateLocations("{\"not\":\"array\"}");
        assertTrue(identityService.getIdentity(user).currentResidence().preferredFutureLocations().isEmpty());
        bg.setPreferredRelocateLocations("not-json");
        assertTrue(identityService.getIdentity(user).currentResidence().preferredFutureLocations().isEmpty());

        User uNoName = new User();
        uNoName.setId(201L);
        UserIdentityCompliance cShort = new UserIdentityCompliance();
        cShort.setUser(uNoName);
        cShort.setIdDocumentNumber("1234");
        UserIdentityComplianceBlob b1 = new UserIdentityComplianceBlob();
        b1.setId(1L);
        b1.setCompliance(cShort);
        b1.setDocPurpose("STATE_ID");

        User uPref = new User();
        uPref.setId(202L);
        uPref.setPreferredName("Preferred");
        UserIdentityCompliance cLong = new UserIdentityCompliance();
        cLong.setUser(uPref);
        cLong.setIdDocumentNumber("12345678");
        UserIdentityComplianceBlob b2 = new UserIdentityComplianceBlob();
        b2.setId(2L);
        b2.setCompliance(cLong);

        UserIdentityComplianceBlob bOrphan = new UserIdentityComplianceBlob();
        bOrphan.setCompliance(null);
        when(blobRepo.findAllForAdminByPurposes(anySet())).thenReturn(List.of(bOrphan, b1, b2));
        assertEquals(2, identityService.listIdentityDocumentsForAdmin().size());
    }

    private CurrentResidenceDto residenceWith(List<PreferredFutureLocationDto> locs) {
        return new CurrentResidenceDto("123 Main", null, null, "Pune", "MH", "411001", "IN", "RENT", 1, 2022, null, "YES", 25, locs);
    }

    private IdentityBackgroundRequest withSections(
            RelationshipDto rel, FamilyDto fam, List<EducationDto> edus, CurrentResidenceDto res, ImmigrationDto imm) {
        return new IdentityBackgroundRequest(
                "SAVE_LATER",
                null, null, null, null, null, null, // 6 name fields
                null, // dateOfBirth
                null, null, null, // pronouns, gender, genderShownToMatches
                null, null, null, null, null, null, // 6 contact fields
                res, null, null, imm, rel, fam, edus, null, null, null, null,
                null, null, null, null);
    }

    @Test
    void emailServiceCoversCallbackCustomerCarePromotionAndTransientRetryBranches() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        PlanRepository planRepo = mock(PlanRepository.class);
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));

        EmailService emailService = new EmailService(sender, planRepo);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@socialpairly.com");
        ReflectionTestUtils.setField(emailService, "appName", "SocialPairly");
        ReflectionTestUtils.setField(emailService, "supportEmail", "customercare@socialpairly.com");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "https://www.socialpairly.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "minSendGapMs", 1L);
        ReflectionTestUtils.setField(emailService, "maxSendAttempts", 2);
        ReflectionTestUtils.setField(emailService, "initialBackoffMs", 1L);

        // 1. sendPremiumCallbackRequestEmails branches
        SupportCallbackRequest cb = new SupportCallbackRequest();
        cb.setId(9L);
        cb.setUserName("Jane");
        cb.setUserEmail("jane@example.com");
        cb.setPhoneNumber("+1 (555) 234-5678");
        cb.setNote("Call after 5pm");

        emailService.sendPremiumCallbackRequestEmails(null, cb);
        emailService.sendPremiumCallbackRequestEmails(List.of(), cb);
        emailService.sendPremiumCallbackRequestEmails(List.of("staff@socialpairly.com"), null);
        emailService.sendPremiumCallbackRequestEmails(
                Arrays.asList(null, "not-an-email", "   ", "staff@socialpairly.com"), cb);

        // Callback without phone/note + supportEmail already in recipient list
        SupportCallbackRequest cbMinimal = new SupportCallbackRequest();
        cbMinimal.setId(10L);
        cbMinimal.setUserName("Alex");
        cbMinimal.setUserEmail("alex@example.com");
        emailService.sendPremiumCallbackRequestEmails(List.of("customercare@socialpairly.com"), cbMinimal);

        // 2. OTP & disabled account branches
        User invalidUser = new User();
        assertThrows(MessagingException.class, () -> emailService.sendPasswordChangeOtpEmail(invalidUser, "1234"));
        assertThrows(MessagingException.class, () -> emailService.sendPhoneChangeOtpEmail(invalidUser, "1234", "+91-****10"));
        emailService.sendAccountDisabledEmail(invalidUser);

        User validUser = new User();
        validUser.setEmail("ada@example.com");
        validUser.setFirstName("Ada");
        emailService.sendPhoneChangeOtpEmail(validUser, "4321", "+91-******3210");
        emailService.sendAccountDisabledEmail(validUser);

        // 3. sendCustomerCareEmail with attachment & URL/issue branches
        CustomerCareRequest ccHttp = new CustomerCareRequest(
                "Ada", "ada@example.com", "+1 555-0100", Arrays.asList(null, "  ", "Billing"),
                "Help", "Need invoice", "https://www.socialpairly.com/billing");
        List<SupportAttachment> attachments = Arrays.asList(
                null,
                new SupportAttachment("empty.png", "image/png", new byte[0]),
                new SupportAttachment(null, null, new byte[]{1, 2, 3}),
                new SupportAttachment("shot.png", "image/png", new byte[]{4, 5})
        );
        emailService.sendCustomerCareEmail(ccHttp, attachments);

        CustomerCareRequest ccRelative = new CustomerCareRequest(
                "Ada", "ada@example.com", null, List.of("   "),
                "General", "Question", "/settings");
        emailService.sendCustomerCareEmail(ccRelative, null);

        // 4. sendPlanPromotionEmail with rich plan attributes & fallback trending plan
        Plan rich = new Plan();
        rich.setPlanName(null);
        rich.setPlanType("MONTHLY");
        rich.setAmount(BigDecimal.valueOf(19.99));
        rich.setDurationDays(30);
        rich.setTokensIncluded("500");
        rich.setCommunityPosts("10/mo");
        rich.setSubBadgeTag("Gold");
        when(planRepo.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(rich));
        emailService.sendPlanPromotionEmail(validUser);

        when(planRepo.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(null);
        emailService.sendPlanPromotionEmail(validUser);

        // 5. Transient SMTP retry + looksLikeTransientSmtp branches
        AtomicInteger calls = new AtomicInteger();
        doAnswer(inv -> {
            if (calls.getAndIncrement() == 0) {
                throw new MailSendException("wrapper", null,
                        Map.of("k", new SMTPSendFailedException("SEND", 421, "4.3.0 temporary", null, null, null, null)));
            }
            return null;
        }).when(sender).send(any(MimeMessage.class));
        emailService.sendWelcomeEmail(validUser);

        for (String msg : List.of(
                "temporary system problem", "try again later", "4.3.0", "4.7.0", "4.7.1",
                "rate limit", "too many", "resources temporarily unavailable", "421", "450 busy", "451 local", "452 storage", "550 permanent")) {
            ReflectionTestUtils.invokeMethod(EmailService.class, "looksLikeTransientSmtp", msg);
        }
    }
}
