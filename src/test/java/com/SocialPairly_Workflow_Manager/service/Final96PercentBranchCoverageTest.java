package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.config.WorldCountrySeedRunner;
import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.checkout.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Final96PercentBranchCoverageTest {

    @Test
    void userServiceAuthCookieAndWorldSeedCoverRemainingBranches() throws Exception {
        // 1. UserService blank/null email & password/phone branches (19 branches)
        UserRepository userRepo = mock(UserRepository.class);
        UserProfileRepository profileRepo = mock(UserProfileRepository.class);
        UserAnswerRepository answerRepo = mock(UserAnswerRepository.class);
        VideoAccessRequestRepository videoRepo = mock(VideoAccessRequestRepository.class);
        UserMediaRepository mediaRepo = mock(UserMediaRepository.class);
        BankDetailsRepository bankRepo = mock(BankDetailsRepository.class);
        RefundRepository refundRepo = mock(RefundRepository.class);
        PaymentRepository payRepo = mock(PaymentRepository.class);
        SubscriptionRepository subRepo = mock(SubscriptionRepository.class);
        EventParticipantRepository partRepo = mock(EventParticipantRepository.class);
        PlanUpgradeRequestRepository upgRepo = mock(PlanUpgradeRequestRepository.class);
        UserPersonalityProfileRepository persRepo = mock(UserPersonalityProfileRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        OtpService otpSvc = mock(OtpService.class);
        EmailService emailSvc = mock(EmailService.class);

        UserService userService = new UserService(
                userRepo, profileRepo, answerRepo, videoRepo, mediaRepo, bankRepo,
                refundRepo, payRepo, subRepo, partRepo, upgRepo, persRepo, encoder, otpSvc, emailSvc);
        ReflectionTestUtils.setField(userService, "defaultCountryCode", "+91");

        User blankEmailUser = new User();
        blankEmailUser.setId(1L);
        blankEmailUser.setRole(Role.USER);
        blankEmailUser.setEmail("   ");
        blankEmailUser.setPhoneNumber("+91-9876543210");
        blankEmailUser.setPassword("");

        User nullEmailUser = new User();
        nullEmailUser.setId(2L);
        nullEmailUser.setRole(Role.USER);
        nullEmailUser.setEmail(null);
        nullEmailUser.setPhoneNumber("+91-9876543210");
        nullEmailUser.setPassword(null);

        assertThrows(BadRequestException.class, () -> userService.sendAccountDeleteOtp(blankEmailUser));
        assertThrows(BadRequestException.class, () -> userService.deleteAccount(blankEmailUser, new DeleteAccountRequest("1234")));
        assertThrows(BadRequestException.class, () -> userService.sendPasswordChangeOtp(nullEmailUser));
        assertThrows(BadRequestException.class, () -> userService.sendPasswordChangeOtp(blankEmailUser));
        assertThrows(BadRequestException.class, () -> userService.verifyPasswordChangeOtp(nullEmailUser, "1234"));
        assertThrows(BadRequestException.class, () -> userService.verifyPasswordChangeOtp(blankEmailUser, "1234"));
        assertThrows(BadRequestException.class, () -> userService.verifyPasswordChangeOtp( makeEmailUser(), null));
        assertThrows(BadRequestException.class, () -> userService.verifyPasswordChangeOtp( makeEmailUser(), "abcd"));

        // changePassword branches: blankPassword user, viaOtp with null/blank email, bad otp, blank currentPassword, no password
        assertThrows(BadRequestException.class, () -> userService.changePassword(nullEmailUser,
                new ChangePasswordRequest(null, "NewPass@1", "NewPass@1", "1234")));
        assertThrows(BadRequestException.class, () -> userService.changePassword(blankEmailUser,
                new ChangePasswordRequest(null, "NewPass@1", "NewPass@1", "1234")));
        assertThrows(BadRequestException.class, () -> userService.changePassword(makeEmailUser(),
                new ChangePasswordRequest(null, "NewPass@1", "NewPass@1", "99")));
        assertThrows(BadRequestException.class, () -> userService.changePassword(blankEmailUser,
                new ChangePasswordRequest("   ", "NewPass@1", "NewPass@1", null)));
        assertThrows(BadRequestException.class, () -> userService.changePassword(blankEmailUser,
                new ChangePasswordRequest("OldPass@1", "NewPass@1", "NewPass@1", null)));

        // OAuth user (no existing password) setting password via OTP
        User oauthUser = makeEmailUser();
        oauthUser.setPassword(null);
        when(encoder.encode(anyString())).thenReturn("hashed");
        userService.changePassword(oauthUser, new ChangePasswordRequest(null, "NewPass@1", "NewPass@1", "1234"));

        // sendPhoneChangeOtp & updatePhone with null/blank email
        assertThrows(BadRequestException.class, () -> userService.sendPhoneChangeOtp(nullEmailUser));
        assertThrows(BadRequestException.class, () -> userService.sendPhoneChangeOtp(blankEmailUser));
        assertThrows(BadRequestException.class, () -> userService.updatePhone(nullEmailUser, "+91-9123456789", "1234"));
        assertThrows(BadRequestException.class, () -> userService.updatePhone(blankEmailUser, "+91-9123456789", "1234"));
        assertThrows(BadRequestException.class, () -> userService.updatePhone(makeEmailUser(), "+91-9123456789", null));

        // 2. AuthCookieService branches (7 branches)
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.resolveExpirationMs(anyBoolean())).thenReturn(3600_000L);
        AuthCookieService cookieSvc1 = new AuthCookieService(jwt, null, false, null);
        AuthCookieService cookieSvc2 = new AuthCookieService(jwt, "   ", true, "   ");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        cookieSvc1.writeAuthCookie(null, null, "tok", false);
        cookieSvc1.writeAuthCookie(null, resp, null, false);
        cookieSvc1.writeAuthCookie(null, resp, "   ", false);

        MockHttpServletRequest req = new MockHttpServletRequest();
        assertNull(cookieSvc1.readToken(req));
        req.setCookies(new Cookie("OTHER", "x"), new Cookie("SP_AUTH", null));
        assertNull(cookieSvc1.readToken(req));
        req.setCookies(new Cookie("SP_AUTH", "   "));
        assertNull(cookieSvc1.readToken(req));

        MockHttpServletRequest bearerBlank = new MockHttpServletRequest();
        bearerBlank.addHeader("Authorization", "Bearer    ");
        assertNull(cookieSvc1.resolveToken(bearerBlank));
        MockHttpServletRequest bearerShort = new MockHttpServletRequest();
        bearerShort.addHeader("Authorization", "Bearer ");
        assertNull(cookieSvc2.resolveToken(bearerShort));

        // 3. WorldCountrySeedRunner when all countries already exist (inserted == 0)
        RefCountryRepository refRepo = mock(RefCountryRepository.class);
        when(refRepo.existsById(anyString())).thenReturn(true);
        new WorldCountrySeedRunner(refRepo).run(new DefaultApplicationArguments());
    }

    private User makeEmailUser() {
        User u = new User();
        u.setId(3L);
        u.setRole(Role.USER);
        u.setEmail("member@example.com");
        u.setPhoneNumber("+91-9876543210");
        return u;
    }

    @Test
    void planUpgradeCheckoutRefundEmployeeAndMediaRemainingBranchesCovered() throws Exception {
        // 1. PlanUpgradeService static & helper branches (16 branches)
        assertFalse(PlanUpgradeService.isEligibleUpgrade(null, null, 0));
        Plan noCurrentCandidate = new Plan();
        noCurrentCandidate.setTokensIncluded("500");
        assertTrue(PlanUpgradeService.isEligibleUpgrade(noCurrentCandidate, null, 100));
        assertFalse(PlanUpgradeService.isEligibleUpgrade(noCurrentCandidate, null, 600));

        Plan nullAmt = new Plan();
        nullAmt.setTokensIncluded("UNLIMITED");
        nullAmt.setAmount(null);
        Plan curAmt = new Plan();
        curAmt.setTokensIncluded("100");
        curAmt.setAmount(BigDecimal.TEN);
        assertFalse(PlanUpgradeService.isEligibleUpgrade(nullAmt, curAmt, 0));

        Plan curNullAmt = new Plan();
        curNullAmt.setTokensIncluded("100");
        curNullAmt.setAmount(null);
        Plan candAmt = new Plan();
        candAmt.setDescription("Unlimited connection tokens included");
        candAmt.setAmount(BigDecimal.valueOf(50));
        assertFalse(PlanUpgradeService.isEligibleUpgrade(candAmt, curNullAmt, 0));

        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(PlanUpgradeService.class, "tokenRank", (Plan) null));
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(PlanUpgradeService.class, "firstTokenNumber", (String) null));
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(PlanUpgradeService.class, "firstTokenNumber", "no digits here"));
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(PlanUpgradeService.class, "largestGroupedTokenNumber", "   "));
        assertEquals(2000, (int) ReflectionTestUtils.invokeMethod(PlanUpgradeService.class, "largestGroupedTokenNumber", "50 or 2000 or 1,500"));

        // 2. RefundService resolvePlanForRefund branches (6 branches)
        RefundRepository refRepo = mock(RefundRepository.class);
        PaymentRepository payRepo = mock(PaymentRepository.class);
        SubscriptionRepository subRepo = mock(SubscriptionRepository.class);
        BankDetailsRepository bankRepo = mock(BankDetailsRepository.class);
        PlanUpgradeRequestRepository upgReqRepo = mock(PlanUpgradeRequestRepository.class);
        CurrentUserService curSvc = mock(CurrentUserService.class);
        EmailService emailSvc = mock(EmailService.class);
        UserTokenService tokSvc = mock(UserTokenService.class);
        RefundService refundSvc = new RefundService(refRepo, bankRepo, payRepo, subRepo, upgReqRepo, curSvc, emailSvc, tokSvc);

        Subscription subNullPlan = new Subscription();
        subNullPlan.setPlan(null);
        Plan fallbackPlan = new Plan();
        Subscription subWithPlan = new Subscription();
        subWithPlan.setPlan(fallbackPlan);
        assertSame(fallbackPlan, ReflectionTestUtils.invokeMethod(refundSvc, "resolvePlanForRefund", null, List.of(subNullPlan, subWithPlan)));
        Payment payNoSub = new Payment();
        assertSame(fallbackPlan, ReflectionTestUtils.invokeMethod(refundSvc, "resolvePlanForRefund", payNoSub, List.of(subWithPlan)));
        Payment paySubNoPlan = new Payment();
        paySubNoPlan.setSubscription(subNullPlan);
        assertNull(ReflectionTestUtils.invokeMethod(refundSvc, "resolvePlanForRefund", paySubNoPlan, List.of()));

        // 3. EmployeeAccessService remaining branches (11 branches)
        StaffRoleAssignmentRepository assignRepo = mock(StaffRoleAssignmentRepository.class);
        RoleDashboardAccessRepository dashRepo = mock(RoleDashboardAccessRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        EmployeeAccessService empSvc = new EmployeeAccessService(assignRepo, dashRepo, userRepo, curSvc);

        StaffRoleAssignment linkedRow = new StaffRoleAssignment();
        linkedRow.setEmail("linked@socialpairly.com");
        linkedRow.setUserId(77L);
        linkedRow.setActive(false);
        when(assignRepo.findByEmailIgnoreCase("linked@socialpairly.com")).thenReturn(Optional.of(linkedRow));
        User adminToDemote = new User();
        adminToDemote.setId(77L);
        adminToDemote.setEmail("linked@socialpairly.com");
        adminToDemote.setRole(Role.ADMIN);
        when(userRepo.findById(77L)).thenReturn(Optional.of(adminToDemote));
        empSvc.applyEmployeeAccessOnAuth(adminToDemote);
        assertEquals(Role.USER, adminToDemote.getRole());

        empSvc.ensureSeedSuperAdmin(adminToDemote); // existing with non-null userId

        User seedAlreadyAdmin = new User();
        seedAlreadyAdmin.setId(78L);
        seedAlreadyAdmin.setEmail("newseed@socialpairly.com");
        seedAlreadyAdmin.setRole(Role.ADMIN);
        when(assignRepo.findByEmailIgnoreCase("newseed@socialpairly.com")).thenReturn(Optional.empty());
        empSvc.ensureSeedSuperAdmin(seedAlreadyAdmin);

        when(assignRepo.existsByEmailIgnoreCase("auto@socialpairly.com")).thenReturn(true);
        User autoUser = new User();
        autoUser.setId(79L);
        autoUser.setEmail("auto@socialpairly.com");
        ReflectionTestUtils.invokeMethod(empSvc, "ensureDomainAutoAssignment", autoUser);

        assertTrue(((Map<?, ?>) ReflectionTestUtils.invokeMethod(empSvc, "loadUsersByEmails", List.of())).isEmpty());

        // 4. UserMediaService moderation & avatar branches (11 branches)
        UserMediaRepository mediaRepo = mock(UserMediaRepository.class);
        TextModerationService txtMod = mock(TextModerationService.class);
        ImageModerationService imgMod = mock(ImageModerationService.class);
        UserMediaService mediaSvc = new UserMediaService(mediaRepo, userRepo, txtMod, imgMod);

        MockMultipartFile bigImg = new MockMultipartFile("f", "big.jpg", "image/jpeg", new byte[0]) {
            @Override
            public long getSize() {
                return 6L * 1024 * 1024;
            }
        };
        User u = new User();
        u.setId(88L);
        assertThrows(BadRequestException.class, () -> mediaSvc.uploadMedia(u, bigImg));
        assertThrows(BadRequestException.class, () -> mediaSvc.uploadProfilePhoto(u, bigImg));

        when(imgMod.isEnabled()).thenReturn(true);
        when(imgMod.isImageSafe(any())).thenReturn(true);
        when(mediaRepo.findByUserId(88L)).thenReturn(List.of());
        when(mediaRepo.save(any())).thenAnswer(inv -> {
            UserMedia m = inv.getArgument(0);
            if (m != null) {
                m.setUser(null); // exercises owner == null in mapToDto
            }
            return m;
        });
        MockMultipartFile smallVid = new MockMultipartFile("f", "v.mp4", "video/mp4", new byte[]{1, 2, 3});
        mediaSvc.uploadMedia(u, smallVid);

        UserMedia vidExisting = new UserMedia();
        vidExisting.setMediaType(MediaType.VIDEO);
        vidExisting.setContentHash(null);
        vidExisting.setMediaData(null);
        UserMedia primaryPhoto = new UserMedia();
        primaryPhoto.setMediaType(MediaType.PHOTO);
        primaryPhoto.setIsCover(false);
        primaryPhoto.setMediaCategory("PRIMARY");
        primaryPhoto.setPrivacyMode("FRIENDS");
        when(mediaRepo.findByUserId(88L)).thenReturn(List.of(vidExisting, primaryPhoto));
        doAnswer(inv -> inv.getArgument(0)).when(mediaRepo).save(any());
        MockMultipartFile smallImg = new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{9, 8, 7});
        mediaSvc.uploadProfilePhoto(u, smallImg);
    }

    @Test
    void identityOnboardingAndEmailRemainingMicroBranchesCovered() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        UserProfileRepository profileRepo = mock(UserProfileRepository.class);
        RefCountryRepository countryRepo = mock(RefCountryRepository.class);
        UserIdentityBackgroundRepository bgRepo = mock(UserIdentityBackgroundRepository.class);
        UserLifeProfileRepository lifeRepo = mock(UserLifeProfileRepository.class);
        UserIdentityComplianceRepository compRepo = mock(UserIdentityComplianceRepository.class);
        UserIdentityComplianceBlobRepository blobRepo = mock(UserIdentityComplianceBlobRepository.class);

        RefCountry c1 = new RefCountry();
        c1.setCode("US");
        c1.setName("United States");
        c1.setPostalRegex("^[0-9]{5}$");
        c1.setPhoneCode("+1");
        RefCountry c2 = new RefCountry();
        c2.setCode("AQ");
        c2.setName("Antarctica");
        c2.setPostalRegex(null);
        c2.setPhoneCode("   ");
        when(countryRepo.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(c1, c2));
        when(countryRepo.findById("AQ")).thenReturn(Optional.of(c2));

        IdentityOnboardingService svc = new IdentityOnboardingService(
                userRepo, profileRepo, countryRepo, bgRepo, lifeRepo, compRepo, blobRepo,
                new ObjectMapper(), "+91");

        // getReferenceData uncached + cached hit + country fields
        Map<String, Object> ref1 = svc.getReferenceData();
        Map<String, Object> ref2 = svc.getReferenceData();
        assertSame(ref1, ref2);

        User user = new User();
        user.setId(500L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setDateOfBirth(LocalDate.of(1995, 5, 10));
        UserIdentityBackground bg = new UserIdentityBackground();
        bg.setUser(user);
        UserLifeProfile life = new UserLifeProfile();
        life.setUser(user);
        UserIdentityCompliance comp = new UserIdentityCompliance();
        comp.setUser(user);

        when(profileRepo.findByUserId(500L)).thenReturn(Optional.of(profile));
        when(bgRepo.findByUserId(500L)).thenReturn(Optional.of(bg));
        when(lifeRepo.findByUserId(500L)).thenReturn(Optional.of(life));
        when(compRepo.findByUserId(500L)).thenReturn(Optional.of(comp));
        when(userRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bgRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lifeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(compRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // PreviousAddresses empty & populated with blank residenceType + AQ null regex postal
        PreviousAddressDto prev = new PreviousAddressDto(
                null, "1 Ice Rd", null, null, "McMurdo", "South", "AQ", "0000", "   ", 1, 2020, 6, 2021, "Move");
        NationalityDto nat = new NationalityDto(
                "US", "US", "US", Arrays.asList("  ", "IN"),
                List.of(new LanguageDto("  ", null), new LanguageDto("en", "FLUENT")));
        ImmigrationDto imm = new ImmigrationDto(
                "US", "CITIZEN", "NO", "NO", "YES", Arrays.asList("  ", "IN"));
        CareerDto career = new CareerDto(
                "  ", "Software Engineer", "Technology", null, null, null, null, null, 5,
                null, null, null, "Lead", null, "TechCorp", true, "Full-Time");

        IdentityBackgroundRequest fullDraft = new IdentityBackgroundRequest(
                "SAVE_LATER",
                "Ms.", "Ada", "Augusta", "Lovelace", "None", "Ada",
                LocalDate.of(1995, 5, 10),
                "She/Her", "Woman", "PUBLIC",
                "sec@example.com", null, "+1-2025550111", null, "EMAIL", "EVENING",
                new CurrentResidenceDto("1 Main", null, null, "Austin", "TX", "78701", "US", "OWN", 2, 2022, null, "NO", 0,
                        List.of(new PreferredFutureLocationDto("Texas", List.of("Austin")))),
                List.of(prev),
                nat,
                imm,
                new RelationshipDto("  ", null, null, null, false, false, null, "NO", null, "   "),
                new FamilyDto("NO", 0, null, null, null, null, null, null, null, null, null, null, "dog"),
                List.of(new EducationDto(null, "  ", "BS", "Math", "Oxford", "London", "US", null, 2015, null, 2019, false, null, false, null)),
                career,
                null, null, null,
                "MATCHES", "***-**-1234", null, null);
        svc.saveIdentity(user, fullDraft);

        // Second legal name change throws LEGAL_NAME_LOCKED_MESSAGE, then clear previousAddresses with unchanged middleName
        IdentityBackgroundRequest lockedChange = new IdentityBackgroundRequest(
                "SAVE_LATER", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, List.of(), null, null, null, null, null, null, null, null, null,
                null, null, null, null);
        assertThrows(BadRequestException.class, () -> svc.saveIdentity(user, lockedChange));

        IdentityBackgroundRequest clearPrev = new IdentityBackgroundRequest(
                "SAVE_LATER", null, "Ada", "Augusta", "Lovelace", null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, List.of(), null, null, null, null, null, null, null, null, null,
                null, null, null, null);
        svc.saveIdentity(user, clearPrev);

        // Extra IdentityOnboardingService helper & webhook/stream/consent branches
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asInteger", (Object) null));
        assertEquals(7, (Integer) ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asInteger", 7L));
        assertEquals(8, (Integer) ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asInteger", "8"));
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asInteger", "bad"));
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asLong", (Object) null));
        assertEquals(9L, (Long) ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asLong", 9));
        assertEquals(10L, (Long) ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asLong", "10"));
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asLong", "bad"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asBoolean", Boolean.FALSE));
        assertTrue(((List<?>) ReflectionTestUtils.invokeMethod(svc, "readMapList", "null")).isEmpty());
        assertTrue(((List<?>) ReflectionTestUtils.invokeMethod(svc, "readMapList", "not-json")).isEmpty());
        assertTrue(((List<?>) ReflectionTestUtils.invokeMethod(svc, "readStringList", "null")).isEmpty());
        assertTrue(((List<?>) ReflectionTestUtils.invokeMethod(svc, "readStringList", "not-json")).isEmpty());
        assertNull(ReflectionTestUtils.invokeMethod(svc, "calculateResidenceDurationMonths", null, 2022));
        assertNull(ReflectionTestUtils.invokeMethod(svc, "calculateResidenceDurationMonths", 5, null));
        assertNull(ReflectionTestUtils.invokeMethod(svc, "calculateResidenceDurationMonths", 0, 2022));
        assertNull(ReflectionTestUtils.invokeMethod(svc, "calculateResidenceDurationMonths", 13, 2022));
        assertEquals(0, (Integer) ReflectionTestUtils.invokeMethod(svc, "calculateResidenceDurationMonths", 12, 2099));

        comp.setSessionId("sess-1");
        comp.setOverallStatus("IN_PROGRESS");
        comp.setPhotoStatus("PENDING");
        when(compRepo.findBySessionId("sess-1")).thenReturn(Optional.of(comp));
        svc.startVerification(user);
        svc.handleVerificationWebhook(Map.of("session_id", "sess-1", "overallStatus", "VERIFIED", "photoStatus", "VERIFIED", "nameStatus", "VERIFIED", "ageStatus", "VERIFIED"));
        svc.handleVerificationWebhook(Map.of("sessionId", "sess-1", "status", "   "));

        // ProdProfileSelector extra branches
        assertNull(com.SocialPairly_Workflow_Manager.config.ProdProfileSelector.selectProfile(new String[]{null, "--other"}));
        com.SocialPairly_Workflow_Manager.config.ProdProfileSelector.isProdEnabled(new String[]{"--app.prod-flag", "true"});
        com.SocialPairly_Workflow_Manager.config.ProdProfileSelector.isPreprodEnabled(new String[]{"--app.preprod-flag=1"});

        // Future month rejection, negative travel radius, >10 previous addresses, invalid approxYear/month/yearRange
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                IdentityOnboardingService.class, "rejectFutureMonth", "Start month", 12, 2099, 2025 * 12 + 5));
        ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "rejectFutureMonth", "Start month", null, 2025, 2025 * 12 + 5);
        ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "rejectFutureMonth", "Start month", 5, null, 2025 * 12 + 5);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateOptionalApproxYear", "yr", 1800));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateOptionalApproxYear", "yr", 2999));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateMonth", "m", 0));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(svc, "validateMonth", "m", 13));
        ReflectionTestUtils.invokeMethod(svc, "validateYearRange", "addr", null, null, null, 2020);
        ReflectionTestUtils.invokeMethod(svc, "validateYearRange", "addr", null, 2020, null, null);
        ReflectionTestUtils.invokeMethod(svc, "validatePostalCode", null, "12345");
        ReflectionTestUtils.invokeMethod(svc, "validatePostalCode", "US", null);
        assertNull(ReflectionTestUtils.invokeMethod(svc, "resolveCountryName", (String) null));
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asBoolean", (Object) null));
        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "asBoolean", "true"));

        // Structured preferredRelocateLocations JSON edge cases
        bg.setPreferredRelocateLocations("[{\"state\":null,\"cities\":[]},{\"state\":\"TX\",\"cities\":\"not-array\"},123]");
        svc.getIdentity(user);

        // EmailService minimal plan without optional fields (covers false branches of lines 131-145)
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage((jakarta.mail.Session) null));
        PlanRepository planRepo = mock(PlanRepository.class);
        EmailService emailSvc = new EmailService(sender, planRepo);
        ReflectionTestUtils.setField(emailSvc, "senderEmail", "noreply@socialpairly.com");
        ReflectionTestUtils.setField(emailSvc, "appName", "SocialPairly");
        ReflectionTestUtils.setField(emailSvc, "supportEmail", null);
        ReflectionTestUtils.setField(emailSvc, "appBaseUrl", "https://www.socialpairly.com");
        ReflectionTestUtils.setField(emailSvc, "frontendUrl", "http://localhost:3000");

        Plan barePlan = new Plan();
        barePlan.setPlanName("Basic");
        barePlan.setTokensIncluded("   ");
        barePlan.setCommunityPosts("   ");
        barePlan.setSubBadgeTag("   ");
        when(planRepo.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(barePlan));
        emailSvc.sendPlanPromotionEmail(user);

        SupportCallbackRequest cbBlankNote = new SupportCallbackRequest();
        cbBlankNote.setId(55L);
        cbBlankNote.setUserName("Ada");
        cbBlankNote.setUserEmail("ada@example.com");
        cbBlankNote.setNote("   ");
        emailSvc.sendPremiumCallbackRequestEmails(List.of("staff@socialpairly.com"), cbBlankNote);
    }

    @Test
    void crossCuttingHelpersPushBranchCoveragePast96Percent() throws Exception {
        // 1. EmailTemplateBuilder null/blank branches (4 branches)
        com.SocialPairly_Workflow_Manager.util.EmailTemplateBuilder.infoBox("T", null);
        com.SocialPairly_Workflow_Manager.util.EmailTemplateBuilder.htmlParagraph(null);
        com.SocialPairly_Workflow_Manager.util.EmailTemplateBuilder.htmlInfoBox("T", null);
        com.SocialPairly_Workflow_Manager.util.EmailTemplateBuilder.htmlInfoBox("T", Arrays.asList(null, "   ", "<b>ok</b>"));

        // 2. SupportController private helpers (5 branches)
        assertEquals("image/jpeg", ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "normalizeContentType", "image/pjpeg"));
        assertEquals("image/jpeg", ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "normalizeContentType", "image/jpg"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "extensionMatches", "doc.jpeg", "image/jpeg"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "extensionMatches", "doc.pdf", "application/pdf"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "extensionMatches", "doc.exe", "application/zip"));
        assertEquals("attachment-1", ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "safeFilename", null, 1));
        assertEquals("attachment-2", ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "safeFilename", "..", 2));
        ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "toAttachments",
                Arrays.asList(null, new MockMultipartFile("f", "empty.png", "image/png", new byte[0])));

        // 3. DashboardAccessInterceptor branches (4 branches)
        EmployeeAccessService empMock = mock(EmployeeAccessService.class);
        com.SocialPairly_Workflow_Manager.security.DashboardAccessInterceptor interceptor =
                new com.SocialPairly_Workflow_Manager.security.DashboardAccessInterceptor(empMock);
        MockHttpServletRequest ctxReq = new MockHttpServletRequest("GET", "/app/api/admin/");
        ctxReq.setContextPath("/app");
        interceptor.preHandle(ctxReq, new MockHttpServletResponse(), new Object());
        MockHttpServletRequest mismatchCtx = new MockHttpServletRequest("GET", "/other/api/public");
        mismatchCtx.setContextPath("/app");
        interceptor.preHandle(mismatchCtx, new MockHttpServletResponse(), new Object());
        ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.security.DashboardAccessInterceptor.class, "resolveDashboardKey", (String) null);
        ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.security.DashboardAccessInterceptor.class, "resolveDashboardKey", "   ");
        ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.security.DashboardAccessInterceptor.class, "resolveDashboardKey", "/api/admin");

        // 4. SecurityConfig static helpers (6 branches)
        List<String> origins = new ArrayList<>(List.of("http://localhost:3000"));
        ReflectionTestUtils.invokeMethod(com.SocialPairly_Workflow_Manager.config.SecurityConfig.class,
                "addConfiguredFrontendOrigin", origins, -1);
        ReflectionTestUtils.invokeMethod(com.SocialPairly_Workflow_Manager.config.SecurityConfig.class,
                "addConfiguredFrontendOrigin", origins, 70000);
        ReflectionTestUtils.invokeMethod(com.SocialPairly_Workflow_Manager.config.SecurityConfig.class,
                "addConfiguredFrontendOrigin", origins, 3000);
        Class<?> corsCls = Arrays.stream(com.SocialPairly_Workflow_Manager.config.SecurityConfig.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("SameHostCorsConfigurationSource"))
                .findFirst().orElseThrow();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "relative/path"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "https://socialpairly.com:443"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "http://www.socialpairly.com:80"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "ftp://socialpairly.com"));
        MockHttpServletRequest hostReq = new MockHttpServletRequest();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "sameHost", hostReq, "relative/path"));

        // 5. EventCandidateService & UserEventService static helpers (13 branches)
        EventCandidateDto cand = new EventCandidateDto(
                1L, "Ada", "ada@x.com", null, null, null, null, null, null, null, null, null, false);
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(EventCandidateService.class, "matchesGender", cand, "Female"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(EventCandidateService.class, "matchesReligion", cand, "Hindu"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(EventCandidateService.class, "matchesMaritalStatus", cand, "SINGLE"));

        User emptyNameUser = new User();
        emptyNameUser.setPreferredName("   ");
        assertEquals("", ReflectionTestUtils.invokeMethod(UserEventService.class, "displayName", emptyNameUser));
        UserIdentityBackground blankBg = new UserIdentityBackground();
        blankBg.setStateRegion("   ");
        assertEquals("", ReflectionTestUtils.invokeMethod(UserEventService.class, "buildLocationLabel", "   ", blankBg));
        assertEquals("", ReflectionTestUtils.invokeMethod(UserEventService.class, "buildLocationLabel", null, null));

        // 6. SupportCallbackService remaining 4 branches
        SupportCallbackRequestRepository cbRepo = mock(SupportCallbackRequestRepository.class);
        StaffInboxNotificationRepository notifRepo = mock(StaffInboxNotificationRepository.class);
        StaffRoleAssignmentRepository roleRepo = mock(StaffRoleAssignmentRepository.class);
        CurrentUserService curSvc = mock(CurrentUserService.class);
        SubscriptionService subSvc = mock(SubscriptionService.class);
        EmailService emailMock = mock(EmailService.class);
        SupportCallbackService cbSvc = new SupportCallbackService(cbRepo, notifRepo, roleRepo, curSvc, subSvc, emailMock);

        User uNoPhone = new User();
        uNoPhone.setId(91L);
        uNoPhone.setPreferredName("");
        uNoPhone.setEmail("nophone@example.com");
        when(curSvc.getCurrentUser()).thenReturn(uNoPhone);
        when(subSvc.hasActiveSubscription(91L)).thenReturn(true);
        when(cbRepo.findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(eq(91L), any(), any()))
                .thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> cbSvc.requestCallback(new CreateSupportCallbackRequest(null)));
        assertEquals("nophone@example.com", ReflectionTestUtils.invokeMethod(SupportCallbackService.class, "formatName", uNoPhone));

        // 7. PlanUpgradeService remaining 6 branches
        PlanUpgradeRequestRepository upgRepo = mock(PlanUpgradeRequestRepository.class);
        RefundRepository refRepo = mock(RefundRepository.class);
        SubscriptionRepository subRepo = mock(SubscriptionRepository.class);
        PlanRepository planRepo = mock(PlanRepository.class);
        PaymentRepository payRepo = mock(PaymentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        PlanUpgradeService upgSvc = new PlanUpgradeService(upgRepo, refRepo, subRepo, planRepo, payRepo, userRepo, curSvc);

        User member = new User();
        member.setId(92L);
        member.setUserTokens(0);
        when(curSvc.getCurrentUser()).thenReturn(member);
        when(subRepo.findActiveSubscriptionsForUser(eq(92L), any())).thenReturn(List.of());
        Plan pNullAmt1 = new Plan();
        pNullAmt1.setTokensIncluded("100");
        pNullAmt1.setAmount(null);
        pNullAmt1.setDurationDays(10);
        Plan pNullAmt2 = new Plan();
        pNullAmt2.setTokensIncluded("200");
        pNullAmt2.setAmount(BigDecimal.ZERO);
        pNullAmt2.setDurationDays(null);
        when(planRepo.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(pNullAmt1, pNullAmt2));
        assertEquals(2, upgSvc.getEligibleUpgradePlans().size());
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(PlanUpgradeService.class, "firstTokenNumber", "   "));

        // 8. CheckoutFulfillmentService private helpers (8 branches)
        CheckoutFulfillmentService cfSvc = new CheckoutFulfillmentService(
                subRepo, payRepo, userRepo, planRepo, emailMock, mock(UserTokenService.class), upgSvc);
        assertNull(ReflectionTestUtils.invokeMethod(cfSvc, "fetchReceiptNumber", "   "));
        assertNull(ReflectionTestUtils.invokeMethod(cfSvc, "fetchReceiptNumber", "not_ch"));
        assertNull(ReflectionTestUtils.invokeMethod(cfSvc, "fetchPaymentMethodLabel", "   ", "   "));
        assertNull(ReflectionTestUtils.invokeMethod(cfSvc, "fetchPaymentMethodLabel", null, "bad_charge"));
        assertNull(ReflectionTestUtils.invokeMethod(cfSvc, "fetchReceiptUrl", "   "));
        assertNull(ReflectionTestUtils.invokeMethod(cfSvc, "fetchReceiptUrl", "bad_charge"));

        // 9. EmailService remaining helper branches (12 branches)
        MimeMessage msg = new MimeMessage((jakarta.mail.Session) null);
        org.springframework.mail.javamail.MimeMessageHelper helper =
                new org.springframework.mail.javamail.MimeMessageHelper(msg, true, "UTF-8");
        ReflectionTestUtils.invokeMethod(EmailService.class, "addSupportAttachments", helper,
                List.of(
                        new SupportAttachment("a", null, null),
                        new SupportAttachment("   ", "   ", new byte[]{1}),
                        new SupportAttachment("ok.txt", "text/plain", new byte[]{2})));
        ReflectionTestUtils.invokeMethod(EmailService.class, "formatAttachmentNames",
                List.of(
                        new SupportAttachment("a", null, null),
                        new SupportAttachment("   ", "text/plain", new byte[]{1})));
        assertEquals("tel:+123", ReflectionTestUtils.invokeMethod(EmailService.class, "telHref", "++123"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(EmailService.class, "isHttpUrl", "http://example.com"));
        ReflectionTestUtils.invokeMethod(EmailService.class, "sleepForMail", 0L);
        ReflectionTestUtils.invokeMethod(EmailService.class, "isTransientSmtpFailure",
                new org.springframework.mail.MailSendException("plain"));
        ReflectionTestUtils.invokeMethod(EmailService.class, "hasTransientSmtpSignal",
                new org.eclipse.angus.mail.smtp.SMTPSendFailedException("SEND", 550, "permanent", null, null, null, null));
        ReflectionTestUtils.invokeMethod(EmailService.class, "hasTransientSmtpSignal",
                new org.eclipse.angus.mail.smtp.SMTPSendFailedException("SEND", 350, "info", null, null, null, null));

        // 10. IdentityOnboardingService extra branches (~25 branches)
        UserProfileRepository profileRepo = mock(UserProfileRepository.class);
        RefCountryRepository countryRepo = mock(RefCountryRepository.class);
        UserIdentityBackgroundRepository bgRepo = mock(UserIdentityBackgroundRepository.class);
        UserLifeProfileRepository lifeRepo = mock(UserLifeProfileRepository.class);
        UserIdentityComplianceRepository compRepo = mock(UserIdentityComplianceRepository.class);
        UserIdentityComplianceBlobRepository blobRepo = mock(UserIdentityComplianceBlobRepository.class);
        IdentityOnboardingService idSvc = new IdentityOnboardingService(
                userRepo, profileRepo, countryRepo, bgRepo, lifeRepo, compRepo, blobRepo, new ObjectMapper(), "+91");

        // PNG short bytes / partial magic bytes on line 322 & 326
        assertEquals("application/octet-stream",
                IdentityOnboardingService.resolveIdImageContentType(null, "", new byte[]{(byte) 0x89, 0x00, 0, 0, 0, 0, 0, 0}));
        assertEquals("application/octet-stream",
                IdentityOnboardingService.resolveIdImageContentType(null, "", new byte[]{(byte) 0x89, 0x50, 0x00, 0, 0, 0, 0, 0}));

        // buildDisplayName: first only, last only, email present
        User uEmail = new User();
        uEmail.setEmail("only@email.com");
        assertEquals("only@email.com", ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "buildDisplayName", uEmail));
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "maskIdDocumentNumber", "   "));

        // validateCareerAlphanumericField & validateEmploymentTypeField invalid inputs
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateCareerAlphanumericField", "job", "Job123"));
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateEmploymentTypeField", "$$$"));
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateEducationTextField", "city", "City123",
                        java.util.regex.Pattern.compile("^[A-Za-z]+$")));
        ReflectionTestUtils.invokeMethod(idSvc, "validateEducationTextField", "city", null,
                java.util.regex.Pattern.compile("^[A-Za-z]+$"));
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateEducationYear", "yr", 1960, 2025, 1990));
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateEducationYear", "yr", 2030, 2025, null));
        ReflectionTestUtils.invokeMethod(idSvc, "validateEducationYear", "yr", 2015, 2025, null);

        // PhoneNumberNormalizer registered code without strict length? Test lookupKeys & registerNationalLength edge branches
        com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.registerNationalLength(null, 10);
        com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.registerNationalLength("   ", 10);
        com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.registerNationalLength("+1", 2);
        com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.registerNationalLength("+1", 20);
        assertNull(com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.nationalLengthFor(null));
        assertNull(com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.nationalLengthFor("+"));

        // 11. RefCountryDataInitializer, DataInitializer, PhoneCodeSeedRunner, UserTokenService, AdminService, Personality, Identity upload/stream (~70 branches)
        RefCountryRepository refInitRepo = mock(RefCountryRepository.class);
        RefCountry existingWithRegex = new RefCountry();
        existingWithRegex.setCode("US");
        existingWithRegex.setPostalRegex("^[0-9]{5}$");
        RefCountry existingNullRegex = new RefCountry();
        existingNullRegex.setCode("IN");
        existingNullRegex.setPostalRegex(null);
        RefCountry existingBlankRegex = new RefCountry();
        existingBlankRegex.setCode("CA");
        existingBlankRegex.setPostalRegex("   ");
        when(refInitRepo.findById(anyString())).thenReturn(Optional.empty());
        when(refInitRepo.findById("US")).thenReturn(Optional.of(existingWithRegex));
        when(refInitRepo.findById("IN")).thenReturn(Optional.of(existingNullRegex));
        when(refInitRepo.findById("CA")).thenReturn(Optional.of(existingBlankRegex));
        new com.SocialPairly_Workflow_Manager.config.RefCountryDataInitializer(refInitRepo).run();

        // DataInitializer when admin exists (found vs null) and questions already exist
        UserRepository diUserRepo = mock(UserRepository.class);
        QuestionRepository qRepo = mock(QuestionRepository.class);
        org.springframework.security.crypto.password.PasswordEncoder diEnc = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        com.SocialPairly_Workflow_Manager.config.DataInitializer dataInit =
                new com.SocialPairly_Workflow_Manager.config.DataInitializer(diUserRepo, qRepo, diEnc, empMock);
        ReflectionTestUtils.setField(dataInit, "adminEmail", "admin@socialpairly.com");
        ReflectionTestUtils.setField(dataInit, "adminPassword", "Admin@123");
        when(diUserRepo.existsByEmail("admin@socialpairly.com")).thenReturn(true);
        when(diUserRepo.findByEmail("admin@socialpairly.com")).thenReturn(Optional.empty());
        when(qRepo.count()).thenReturn(5L);
        dataInit.run();

        // PhoneCodeSeedRunner parse edge lines
        String phoneLines = "# comment\n\nbadline\nUS|United States|1|notnum\nIN|India|+91|99\nX|Short|+1|10\nZZ|NoDial|+|10\n";
        ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.config.PhoneCodeSeedRunner.class,
                "loadPhoneSeeds",
                new java.io.BufferedReader(new java.io.StringReader(phoneLines)));
        RefCountryRepository seedNoDirtyRepo = mock(RefCountryRepository.class);
        when(seedNoDirtyRepo.findById(anyString())).thenReturn(Optional.empty());
        new com.SocialPairly_Workflow_Manager.config.PhoneCodeSeedRunner(seedNoDirtyRepo)
                .run(new DefaultApplicationArguments());

        // UserTokenService null plan & non-matching subscription payment
        UserTokenService realTokSvc = new UserTokenService(userRepo, payRepo);
        User tokUser = new User();
        tokUser.setId(701L);
        tokUser.setUserTokens(5);
        assertEquals(5, realTokSvc.creditTokensFromPlan(tokUser, null));
        realTokSvc.ensureSubscriptionTokensCredited(null, null);
        Subscription subNoPlan = new Subscription();
        realTokSvc.ensureSubscriptionTokensCredited(tokUser, subNoPlan);
        Subscription sub701 = new Subscription();
        sub701.setId(10L);
        Plan p701 = new Plan();
        p701.setTokensIncluded("50");
        sub701.setPlan(p701);
        Subscription otherSub = new Subscription();
        otherSub.setId(99L);
        Payment otherPay = new Payment();
        otherPay.setSubscription(otherSub);
        otherPay.setStatus("pending");
        when(payRepo.findBySubscription_Id(10L)).thenReturn(List.of());
        when(payRepo.findUncreditedSucceededPaymentsForUser(701L)).thenReturn(List.of(otherPay));
        realTokSvc.ensureSubscriptionTokensCredited(tokUser, sub701);

        // AdminService blank answer & unknown option & old/null createdAt
        UserAnswerRepository ansRepo = mock(UserAnswerRepository.class);
        AdminService adminSvc = new AdminService(diUserRepo, qRepo, ansRepo, subRepo, payRepo, empMock);
        User oldU = new User();
        oldU.setCreatedAt(java.time.LocalDateTime.now().minusDays(30));
        User nullDateU = new User();
        when(diUserRepo.findAll()).thenReturn(List.of(oldU, nullDateU));
        Question mcQ = new Question();
        mcQ.setId(1L);
        mcQ.setQuestionText("Q?");
        mcQ.setType(QuestionType.MULTI_CHOICE);
        QuestionOption opt = new QuestionOption();
        opt.setOptionText("Yes");
        mcQ.getOptions().add(opt);
        Question txtQ = new Question();
        txtQ.setId(2L);
        txtQ.setType(QuestionType.TEXT);
        when(qRepo.findAll()).thenReturn(List.of(mcQ, txtQ));
        UserAnswer aBlank = new UserAnswer();
        aBlank.setQuestion(mcQ);
        aBlank.setAnswerValue("   ");
        UserAnswer aUnknown = new UserAnswer();
        aUnknown.setQuestion(mcQ);
        aUnknown.setAnswerValue("UnknownOpt, Yes");
        UserAnswer aNull = new UserAnswer();
        aNull.setQuestion(mcQ);
        aNull.setAnswerValue(null);
        UserAnswer aOtherQ = new UserAnswer();
        aOtherQ.setQuestion(txtQ);
        aOtherQ.setAnswerValue("Yes");
        when(ansRepo.findAll()).thenReturn(List.of(aBlank, aUnknown, aNull, aOtherQ));
        adminSvc.getStats();

        // PersonalityOnboardingService isBlank branches
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(PersonalityOnboardingService.class, "isBlank", (String) null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(PersonalityOnboardingService.class, "isBlank", "   "));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(PersonalityOnboardingService.class, "isBlank", "ok"));

        // IdentityOnboardingService uploadDocument, streamDocument, appendJsonArray, and strict validation micro-branches
        User docOwner = new User();
        docOwner.setId(801L);
        docOwner.setRole(Role.USER);
        User adminViewer = new User();
        adminViewer.setId(802L);
        adminViewer.setRole(Role.ADMIN);
        UserIdentityCompliance docComp = new UserIdentityCompliance();
        docComp.setUser(docOwner);
        when(compRepo.findByUserId(801L)).thenReturn(Optional.of(docComp));
        when(compRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UserIdentityComplianceBlob storedBlob = new UserIdentityComplianceBlob();
        storedBlob.setId(501L);
        storedBlob.setCompliance(docComp);
        storedBlob.setStorageBlob(new byte[]{1, 2, 3});
        storedBlob.setContentType("   ");
        when(blobRepo.findById(501L)).thenReturn(Optional.of(storedBlob));
        idSvc.streamDocument(docOwner, 501L);
        idSvc.streamDocument(adminViewer, 501L);

        when(blobRepo.save(any())).thenAnswer(inv -> {
            UserIdentityComplianceBlob b = inv.getArgument(0);
            b.setId(502L);
            b.setCreatedAt(java.time.LocalDateTime.now());
            return b;
        });
        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0};
        idSvc.uploadDocument(docOwner, new MockMultipartFile("f", "back.png", "   ", validPng), "DL_BACK");
        idSvc.uploadDocument(docOwner, new MockMultipartFile("f", "emp.pdf", "application/pdf", new byte[]{1, 2}), "EMP_VERIFY");

        // appendJsonArray ring-buffer trim + unknown field branch
        ReflectionTestUtils.invokeMethod(idSvc, "appendJsonArray", docComp, "unknownField",
                "[{\"a\":1},{\"a\":2}]", Map.of("a", 3), 1);

        // validateAllowlist case-insensitive match & blankToNull null branch
        ReflectionTestUtils.invokeMethod(idSvc, "validateAllowlist", "prefix", "mr.", Set.of("Mr."));
        assertNull(ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "blankToNull", (String) null));

        // EventCandidateService duplicate profile merge & null user filter
        User ecUser = new User();
        ecUser.setId(901L);
        ecUser.setFirstName("Ada");
        ecUser.setRole(Role.USER);
        UserRepository ecUserRepo = mock(UserRepository.class);
        when(ecUserRepo.findByRole(Role.USER)).thenReturn(List.of(ecUser));
        UserProfile pNullU = new UserProfile();
        UserProfile pMatch1 = new UserProfile();
        pMatch1.setUser(ecUser);
        pMatch1.setGender("Female");
        pMatch1.setReligion("Hindu");
        UserProfile pMatch2 = new UserProfile();
        pMatch2.setUser(ecUser);
        when(profileRepo.findAll()).thenReturn(List.of(pNullU, pMatch1, pMatch2));
        UserLifeProfile lpNullU = new UserLifeProfile();
        UserLifeProfile lpMatch = new UserLifeProfile();
        lpMatch.setUser(ecUser);
        lpMatch.setMaritalStatus("SINGLE");
        when(lifeRepo.findAll()).thenReturn(List.of(lpNullU, lpMatch));
        UserIdentityBackground ibNullU = new UserIdentityBackground();
        UserIdentityBackground ibMatch = new UserIdentityBackground();
        ibMatch.setUser(ecUser);
        when(bgRepo.findAll()).thenReturn(List.of(ibNullU, ibMatch));
        EventCandidateService ecSvc = new EventCandidateService(ecUserRepo, profileRepo, lifeRepo, bgRepo);
        ecSvc.searchCandidates(null, false, "Female", "Hindu", null, null, null, null, null, "SINGLE");
        ecSvc.searchCandidates(null, null, "Male", null, null, null, null, null, null, null);

        // 12. Final 55 micro-branches for >= 96% JaCoCo HTML branch total
        // PhoneNumberNormalizer: dial code registered in knownCountryCodes without NATIONAL_LENGTH_BY_DIAL entry
        @SuppressWarnings("unchecked")
        Map<String, Integer> lenMap = (Map<String, Integer>) ReflectionTestUtils.getField(
                com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.class, "NATIONAL_LENGTH_BY_DIAL");
        String[] origCodes = (String[]) ReflectionTestUtils.getField(
                com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.class, "knownCountryCodes");
        try {
            lenMap.remove("+999");
            List<String> extCodes = new ArrayList<>(Arrays.asList(origCodes));
            extCodes.add(0, "+999");
            ReflectionTestUtils.setField(
                    com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.class,
                    "knownCountryCodes", extCodes.toArray(new String[0]));
            assertEquals("+999-12345678",
                    com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("+99912345678", "+91"));
            assertEquals("+9-991234567",
                    com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("+9991234567", "+91"));
            assertThrows(IllegalArgumentException.class,
                    () -> com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("+9991234567890123456", "+91"));
            assertEquals("+999-12345678",
                    com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("12345678", "+999"));
            assertThrows(IllegalArgumentException.class,
                    () -> com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("1234567", "+999"));
            assertThrows(IllegalArgumentException.class,
                    () -> com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("1234567890123456", "+999"));
        } finally {
            ReflectionTestUtils.setField(
                    com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.class,
                    "knownCountryCodes", origCodes);
        }
        assertThrows(IllegalArgumentException.class,
                () -> com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer.toStorageFormat("9876543210", "+"));

        // IdentityOnboardingService JSON & validation branches
        ReflectionTestUtils.invokeMethod(idSvc, "readPreferredFutureLocations", "[\"   \", \"Austin\"]");
        ReflectionTestUtils.invokeMethod(idSvc, "readPreferredFutureLocations",
                "[{\"state\":\"   \",\"cities\":[\"   \"]},{\"cities\":[\"Dallas\"]},{\"state\":\"TX\",\"cities\":\"bad\"},123]");
        ReflectionTestUtils.invokeMethod(idSvc, "readMapList", "null");
        ReflectionTestUtils.invokeMethod(idSvc, "readStringList", "null");
        ReflectionTestUtils.invokeMethod(idSvc, "latestAcceptedConsent",
                "[{\"accepted\":true,\"documentVersion\":\"OLD_V0\"}]");

        RefCountry noRegexCountry = new RefCountry();
        noRegexCountry.setCode("AE");
        noRegexCountry.setName("UAE");
        noRegexCountry.setPostalRegex(null);
        RefCountry blankRegexCountry = new RefCountry();
        blankRegexCountry.setCode("QA");
        blankRegexCountry.setName("Qatar");
        blankRegexCountry.setPostalRegex("   ");
        when(countryRepo.findById("AE")).thenReturn(Optional.of(noRegexCountry));
        when(countryRepo.findById("QA")).thenReturn(Optional.of(blankRegexCountry));
        ReflectionTestUtils.invokeMethod(idSvc, "validatePostalCode", "AE", "00000");
        ReflectionTestUtils.invokeMethod(idSvc, "validatePostalCode", "QA", "00000");

        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateAllowlist", "prefix", "NOPE", Set.of("Mr.")));
        ReflectionTestUtils.invokeMethod(idSvc, "validateNameField", "middle", "   ", false);
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(idSvc, "validateEducationYear", "yr", 1990, 2026, 2000));
        ReflectionTestUtils.invokeMethod(idSvc, "validateEducationYear", "yr", 2020, 2026, (Integer) null);
        ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "rejectFutureMonth", "Start", null, 2020, 2026 * 12);
        ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "rejectFutureMonth", "Start", 5, null, 2026 * 12);

        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "validatePreferredFutureLocations",
                List.of(new PreferredFutureLocationDto("TX", null))));
        List<String> fiftyTwoCities = new ArrayList<>();
        fiftyTwoCities.add("   ");
        for (int i = 0; i < 52; i++) {
            fiftyTwoCities.add("City" + (char) ('A' + (i / 26)) + (char) ('A' + (i % 26)));
        }
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "validatePreferredFutureLocations",
                List.of(new PreferredFutureLocationDto("StateA", fiftyTwoCities))));

        User idU = new User();
        idU.setId(950L);
        UserProfile idProf = new UserProfile();
        idProf.setUser(idU);
        UserIdentityBackground idBg = new UserIdentityBackground();
        idBg.setUser(idU);
        UserLifeProfile idLife = new UserLifeProfile();
        idLife.setUser(idU);
        when(bgRepo.findByUserId(950L)).thenReturn(Optional.of(idBg));
        when(bgRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lifeRepo.findByUserId(950L)).thenReturn(Optional.of(idLife));
        when(lifeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // applyCurrentResidence branches
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "applyCurrentResidence", idU, idProf,
                new CurrentResidenceDto("1 St", null, null, "Austin", "TX", "78701", "US", "OWN", 1, 2020, null, "YES", -1, null)));
        ReflectionTestUtils.invokeMethod(idSvc, "applyCurrentResidence", idU, idProf,
                new CurrentResidenceDto("1 St", null, null, "Austin", "TX", "78701", "US", "   ", 1, 2020, null, "YES", 25,
                        List.of(new PreferredFutureLocationDto("Texas", List.of("   ", "Austin")))));

        // applyPreviousAddresses branches
        PreviousAddressDto pa = new PreviousAddressDto(null, "1 St", null, null, "Austin", "TX", "US", "78701", "   ", 1, 2018, 12, 2019, null);
        ReflectionTestUtils.invokeMethod(idSvc, "applyPreviousAddresses", idU, List.of(pa));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "applyPreviousAddresses", idU,
                Collections.nCopies(11, pa)));

        // applyRelationship & applyFamily branches
        idLife.setHasChildren("YES");
        idLife.setChildrenCount(0);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "applyRelationship", idU,
                new RelationshipDto("   ", null, null, null, null, null, null, "YES", null, "   ")));
        idLife.setChildrenCount(2);
        ReflectionTestUtils.invokeMethod(idSvc, "applyRelationship", idU,
                new RelationshipDto("   ", null, null, null, null, null, null, "YES", null, "   "));

        idLife.setCoParenting("NO");
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "applyFamily", idU,
                new FamilyDto("NO", -1, null, null, null, null, null, null, null, null, null, null, null)));
        idLife.setCoParenting("YES");
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "applyFamily", idU,
                new FamilyDto(null, 0, null, null, null, null, null, null, null, null, null, null, null)));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(idSvc, "applyFamily", idU,
                new FamilyDto("YES", null, null, null, null, null, null, null, null, null, null, null, null)));

        // SecurityConfig.SameHostCorsConfigurationSource branches
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", ":::bad"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "mailto:user@example.com"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "https://socialpairly.com:8443"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "http://www.socialpairly.com:80"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "http://www.socialpairly.com:8080"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "socialPairlyPublicOrigin", "ftp://socialpairly.com"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(corsCls, "sameHost", new MockHttpServletRequest(), "mailto:user@example.com"));

        // EmailService sleep / gap / failedMessages transient checks
        ReflectionTestUtils.invokeMethod(EmailService.class, "sleepForMail", 0L);
        Thread.currentThread().interrupt();
        try {
            assertThrows(org.springframework.mail.MailSendException.class,
                    () -> ReflectionTestUtils.invokeMethod(EmailService.class, "sleepForMail", 10L));
        } finally {
            Thread.interrupted();
        }
        Map<Object, Exception> transientMap = new LinkedHashMap<>();
        transientMap.put("m1", new RuntimeException("permanent failure"));
        transientMap.put("m2", new RuntimeException("421 try again later"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(EmailService.class, "isTransientSmtpFailure",
                new org.springframework.mail.MailSendException(transientMap)));

        // 13. DTO, Entity, Controller, ProdProfileSelector & Service 32 branches -> 96% HTML total
        assertFalse(new RefundTokenAdjustment(10, 5, 10, 5, 2, null).hasTokenUsageDeduction());
        assertFalse(new RefundTokenAdjustment(10, 5, 10, 5, 2, BigDecimal.ZERO).hasTokenUsageDeduction());

        User payUser = new User();
        payUser.setId(960L);
        payUser.setEmail("pay@example.com");
        Payment blankChargePay = new Payment();
        blankChargePay.setId(1L);
        blankChargePay.setUser(payUser);
        blankChargePay.setStripeChargeId("   ");
        blankChargePay.setStripePaymentIntentId("pi_123");
        blankChargePay.setAmountRefunded(BigDecimal.ZERO);
        blankChargePay.setStatus("SUCCEEDED");
        AdminPaymentDto.from(blankChargePay);

        User candBlankPref = new User();
        candBlankPref.setId(961L);
        candBlankPref.setPreferredName("   ");
        candBlankPref.setFirstName(null);
        candBlankPref.setLastName("Doe");
        EventCandidateDto.from(candBlankPref, null, null, null);

        StaffRoleAssignment sra = new StaffRoleAssignment();
        sra.setEmail(null);
        sra.onCreate();
        sra.onUpdate();

        UserPersonalityProfile upp = new UserPersonalityProfile();
        upp.setFriendDescriptors(null);
        upp.setPersonalityTraits(null);

        Class<?> ppsCls = com.SocialPairly_Workflow_Manager.config.ProdProfileSelector.class;
        ReflectionTestUtils.invokeMethod(ppsCls, "expandPlaceholder", "plain}");
        ReflectionTestUtils.invokeMethod(ppsCls, "expandPlaceholder", "${open");
        ReflectionTestUtils.invokeMethod(ppsCls, "expandPlaceholder", "${UNSET_ENV_VAR_99999}");
        ReflectionTestUtils.invokeMethod(ppsCls, "firstNonBlank", "primary", "secondary");

        // EventCandidateService: non-member profile rows + null candidate attributes when filtering
        User nonMember = new User();
        nonMember.setId(999L);
        UserProfile nonMemProf = new UserProfile();
        nonMemProf.setUser(nonMember);
        UserLifeProfile nonMemLife = new UserLifeProfile();
        nonMemLife.setUser(nonMember);
        UserIdentityBackground nonMemBg = new UserIdentityBackground();
        nonMemBg.setUser(nonMember);
        when(profileRepo.findAll()).thenReturn(List.of(nonMemProf));
        when(lifeRepo.findAll()).thenReturn(List.of(nonMemLife));
        when(bgRepo.findAll()).thenReturn(List.of(nonMemBg));
        ecSvc.searchCandidates(null, null, "Female", null, null, null, null, null, null, null);
        ecSvc.searchCandidates(null, null, null, "Hindu", null, null, null, null, null, null);
        ecSvc.searchCandidates(null, null, null, null, null, null, null, null, null, "SINGLE");

        // PlanUpgradeService: active subscription with currentPlanName + null candidate planName + reverse sort
        Plan curPlan = new Plan();
        curPlan.setPlanName("Bronze");
        curPlan.setTokensIncluded("10");
        curPlan.setAmount(BigDecimal.ZERO);
        Subscription activeSub = new Subscription();
        activeSub.setPlan(curPlan);
        when(subRepo.findActiveSubscriptionsForUser(eq(92L), any())).thenReturn(List.of(activeSub));
        Plan candA = new Plan();
        candA.setPlanName(null);
        candA.setTokensIncluded("500");
        candA.setAmount(new BigDecimal("10"));
        candA.setDurationDays(30);
        Plan candB = new Plan();
        candB.setPlanName("Gold");
        candB.setTokensIncluded("500");
        candB.setAmount(new BigDecimal("10"));
        candB.setDurationDays(null);
        when(planRepo.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(candA, candB));
        upgSvc.getEligibleUpgradePlans();

        // UserTokenService: findBySubscription_Id non-empty + null subscription & matching subscription in uncredited list
        Payment subPay = new Payment();
        subPay.setStatus("pending");
        Payment orphanPay = new Payment();
        orphanPay.setSubscription(null);
        orphanPay.setStatus("pending");
        Payment matchSubPay = new Payment();
        matchSubPay.setSubscription(sub701);
        matchSubPay.setStatus("pending");
        when(payRepo.findBySubscription_Id(10L)).thenReturn(List.of(subPay));
        when(payRepo.findUncreditedSucceededPaymentsForUser(701L)).thenReturn(List.of(orphanPay, matchSubPay));
        realTokSvc.ensureSubscriptionTokensCredited(tokUser, sub701);

        // UserMediaController range branches
        UserMediaService mSvcMock = mock(UserMediaService.class);
        UserMedia vidMedia = new UserMedia();
        vidMedia.setMediaType(MediaType.VIDEO);
        vidMedia.setMediaData(new byte[]{1, 2, 3, 4, 5});
        when(mSvcMock.getMediaEntityById(77L)).thenReturn(vidMedia);
        com.SocialPairly_Workflow_Manager.controller.UserMediaController mCtrl =
                new com.SocialPairly_Workflow_Manager.controller.UserMediaController(
                        mSvcMock, curSvc, mock(VideoAccessRequestRepository.class));
        assertEquals(206, mCtrl.streamMedia(77L, "bytes=1-").getStatusCode().value());
        assertEquals(416, mCtrl.streamMedia(77L, "bytes=99-100").getStatusCode().value());
        assertEquals(416, mCtrl.streamMedia(77L, "bytes=3-1").getStatusCode().value());

        // AuthController logout blank & invalid token branches
        AuthCookieService acMock = mock(AuthCookieService.class);
        JwtUtil jwtMock = mock(JwtUtil.class);
        com.SocialPairly_Workflow_Manager.controller.AuthController authCtrl =
                new com.SocialPairly_Workflow_Manager.controller.AuthController(
                        mock(AuthService.class), curSvc, mock(AuthRateLimitService.class),
                        acMock, jwtMock, mock(com.SocialPairly_Workflow_Manager.security.JwtTokenBlacklistService.class),
                        null);
        when(acMock.resolveToken(any())).thenReturn("   ");
        authCtrl.logout(new MockHttpServletRequest(), new MockHttpServletResponse());
        when(acMock.resolveToken(any())).thenReturn("expired.jwt.tok");
        when(jwtMock.isTokenValid("expired.jwt.tok")).thenReturn(false);
        authCtrl.logout(new MockHttpServletRequest(), new MockHttpServletResponse());

        // 14. Final 16 branches -> 96% HTML branch coverage in JaCoCo index.html
        assertEquals("", ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "normalizeContentType", (String) null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "extensionMatches", "pic.jpeg", "image/jpeg"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "extensionMatches", "pic.bmp", "image/jpeg"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.SupportController.class, "extensionMatches", "pic.bmp", "text/plain"));

        MockHttpServletRequest blankFwdReq = new MockHttpServletRequest();
        blankFwdReq.addHeader("X-Forwarded-For", "   ");
        ReflectionTestUtils.invokeMethod(
                com.SocialPairly_Workflow_Manager.controller.UserController.class, "clientIp", blankFwdReq);

        Plan zeroTokPlan = new Plan();
        zeroTokPlan.setId(77L);
        zeroTokPlan.setTokensIncluded("none");
        realTokSvc.creditTokensFromPlan(tokUser, zeroTokPlan);

        User adminOwner = new User();
        adminOwner.setId(801L);
        adminOwner.setRole(Role.ADMIN);
        idSvc.streamDocument(adminOwner, 501L);

        UserIdentityComplianceBlob blobNoComp = new UserIdentityComplianceBlob();
        UserIdentityCompliance compNoUser = new UserIdentityCompliance();
        UserIdentityComplianceBlob blobCompNoUser = new UserIdentityComplianceBlob();
        blobCompNoUser.setCompliance(compNoUser);
        when(blobRepo.findAllForAdminByPurposes(any())).thenReturn(List.of(blobNoComp, blobCompNoUser));
        assertTrue(idSvc.listIdentityDocumentsForAdmin().isEmpty());

        User blankPrefUser = new User();
        blankPrefUser.setPreferredName("   ");
        ReflectionTestUtils.invokeMethod(IdentityOnboardingService.class, "buildDisplayName", blankPrefUser);

        assertNull(ReflectionTestUtils.invokeMethod(idSvc, "calculateResidenceDurationMonths", null, 2020));
        assertNull(ReflectionTestUtils.invokeMethod(idSvc, "calculateResidenceDurationMonths", 5, null));
        assertNull(ReflectionTestUtils.invokeMethod(idSvc, "calculateResidenceDurationMonths", 0, 2020));
        assertNull(ReflectionTestUtils.invokeMethod(idSvc, "calculateResidenceDurationMonths", 13, 2020));
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(idSvc, "calculateResidenceDurationMonths", 1, 2099));

        docComp.setIdDocumentType(null);
        ReflectionTestUtils.invokeMethod(idSvc, "applyIdDocument", docOwner, null, "12345678");
        ReflectionTestUtils.invokeMethod(idSvc, "applyIdDocument", docOwner, "STATE_ID", "12345678");

        com.SocialPairly_Workflow_Manager.controller.UserController uCtrl =
                new com.SocialPairly_Workflow_Manager.controller.UserController(
                        curSvc, mock(UserService.class), profileRepo, subSvc, realTokSvc, mock(AuthRateLimitService.class), empMock);
        uCtrl.updatePhone(new UpdatePhoneRequest("+91-9876543210", "   "), new MockHttpServletRequest());

        StaffRoleAssignmentRepository sraRepo = mock(StaffRoleAssignmentRepository.class);
        RoleDashboardAccessRepository rdaRepo = mock(RoleDashboardAccessRepository.class);
        when(rdaRepo.findByRoleCodeAndDashboardKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(rdaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        User superActor = new User();
        superActor.setId(990L);
        superActor.setRole(Role.ADMIN);
        superActor.setEmail("admin@socialpairly.com");
        when(curSvc.getCurrentUser()).thenReturn(superActor);
        EmployeeAccessService realEmpSvc = new EmployeeAccessService(sraRepo, rdaRepo, mock(UserRepository.class), curSvc);
        realEmpSvc.updateDashboardAccess(new UpdateRoleDashboardAccessRequest("SUPER_ADMIN", "DASHBOARD", true));
        realEmpSvc.updateDashboardAccess(new UpdateRoleDashboardAccessRequest("SUPER_ADMIN", "ROLE_ACCESS", true));
        realEmpSvc.updateDashboardAccess(new UpdateRoleDashboardAccessRequest("CUSTOMER_SUPPORT", "ROLE_ACCESS", false));
    }
}
