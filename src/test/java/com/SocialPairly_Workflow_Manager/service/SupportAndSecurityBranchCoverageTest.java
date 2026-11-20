package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.DashboardKeys;
import com.SocialPairly_Workflow_Manager.constants.EmployeeRoleCodes;
import com.SocialPairly_Workflow_Manager.controller.AdminCustomerSupportController;
import com.SocialPairly_Workflow_Manager.controller.SupportCallbackController;
import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ForbiddenException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.SocialPairly_Workflow_Manager.security.CookieSessionCsrfRequestMatcher;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SupportAndSecurityBranchCoverageTest {

    @Test
    void supportEntitiesAndControllersCoverAllBranches() {
        SupportCallbackRequest reqEntity = new SupportCallbackRequest();
        reqEntity.setStatus(null);
        reqEntity.onCreate();
        assertEquals(SupportCallbackStatus.PENDING, reqEntity.getStatus());
        assertNotNull(reqEntity.getCreatedAt());
        reqEntity.setStatus(SupportCallbackStatus.CONTACTED);
        reqEntity.onCreate();
        assertEquals(SupportCallbackStatus.CONTACTED, reqEntity.getStatus());
        reqEntity.onUpdate();
        assertNotNull(reqEntity.getUpdatedAt());

        StaffInboxNotification notif = new StaffInboxNotification();
        notif.setRecipientEmail(null);
        notif.onCreate();
        assertNull(notif.getRecipientEmail());
        notif.setRecipientEmail("  ADMIN@SocialPairly.com ");
        notif.onCreate();
        assertEquals("admin@socialpairly.com", notif.getRecipientEmail());
        notif.setId(5L);
        notif.setRecipientUserId(9L);
        notif.setRecipientRoleCode("ADMIN");
        notif.setType("CALLBACK_REQUEST");
        notif.setTitle("T");
        notif.setMessage("M");
        notif.setRelatedCallbackId(3L);
        notif.setRead(true);
        notif.setCreatedAt(LocalDateTime.now());
        assertEquals(5L, notif.getId());
        assertEquals(9L, notif.getRecipientUserId());
        assertEquals("ADMIN", notif.getRecipientRoleCode());
        assertEquals("CALLBACK_REQUEST", notif.getType());
        assertEquals("T", notif.getTitle());
        assertEquals("M", notif.getMessage());
        assertEquals(3L, notif.getRelatedCallbackId());
        assertTrue(notif.isRead());

        SupportCallbackService svc = mock(SupportCallbackService.class);
        when(svc.listCallbacks(any())).thenReturn(List.of());
        when(svc.countPending()).thenReturn(2L);
        when(svc.countMyUnreadNotifications()).thenReturn(4L);
        when(svc.listMyNotifications()).thenReturn(List.of());

        AdminCustomerSupportController adminCtrl = new AdminCustomerSupportController(svc);
        assertEquals(200, adminCtrl.listCallbacks(null).getStatusCode().value());
        assertEquals(200, adminCtrl.listCallbacks("   ").getStatusCode().value());
        assertEquals(200, adminCtrl.listCallbacks("pending").getStatusCode().value());
        assertEquals(200, adminCtrl.listCallbacks("not_a_valid_status").getStatusCode().value());
        assertEquals(2L, adminCtrl.pendingCount().getBody().get("pendingCount"));
        assertEquals(4L, adminCtrl.unreadCount().getBody().get("unreadCount"));
        assertEquals(200, adminCtrl.listNotifications().getStatusCode().value());
        adminCtrl.updateCallback(1L, new UpdateSupportCallbackStatusRequest(SupportCallbackStatus.CLOSED));

        SupportCallbackController userCtrl = new SupportCallbackController(svc);
        userCtrl.requestCallback(null);
        userCtrl.requestCallback(new CreateSupportCallbackRequest("Call evening"));
        verify(svc, times(2)).requestCallback(any());
    }

    @Test
    void supportCallbackServiceCoversAllMissedBranches() {
        SupportCallbackRequestRepository cbRepo = mock(SupportCallbackRequestRepository.class);
        StaffInboxNotificationRepository notifRepo = mock(StaffInboxNotificationRepository.class);
        StaffRoleAssignmentRepository roleRepo = mock(StaffRoleAssignmentRepository.class);
        CurrentUserService currentUserSvc = mock(CurrentUserService.class);
        SubscriptionService subSvc = mock(SubscriptionService.class);
        EmailService emailSvc = mock(EmailService.class);

        SupportCallbackService service = new SupportCallbackService(
                cbRepo, notifRepo, roleRepo, currentUserSvc, subSvc, emailSvc);

        // User with blank first/last, preferredName set, secondaryPhone fallback
        User u = new User();
        u.setId(10L);
        u.setEmail("member@example.com");
        u.setPreferredName("  PrefName ");
        u.setPhoneNumber("  ");
        u.setSecondaryPhone("+1-2025550199");

        when(currentUserSvc.getCurrentUser()).thenReturn(u);
        when(subSvc.hasActiveSubscription(10L)).thenReturn(true);
        when(cbRepo.findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(10L), eq(SupportCallbackStatus.PENDING), any())).thenReturn(Optional.empty());
        when(cbRepo.save(any())).thenAnswer(inv -> {
            SupportCallbackRequest r = inv.getArgument(0);
            r.setId(77L);
            return r;
        });

        StaffRoleAssignment sNull = new StaffRoleAssignment();
        sNull.setEmail(null);
        StaffRoleAssignment sBlank = new StaffRoleAssignment();
        sBlank.setEmail("   ");
        StaffRoleAssignment s1 = new StaffRoleAssignment();
        s1.setEmail("Admin@SocialPairly.com");
        s1.setRoleCode("ADMIN");
        StaffRoleAssignment sDup = new StaffRoleAssignment();
        sDup.setEmail("admin@socialpairly.com");
        sDup.setRoleCode("SUPER_ADMIN");
        when(roleRepo.findByRoleCodeInAndActiveTrue(anySet())).thenReturn(List.of(sNull, sBlank, s1, sDup));

        SupportCallbackRequestDto dto1 = service.requestCallback(null);
        assertEquals("PrefName", dto1.userName());

        // User with no name and no preferredName -> falls back to email; no staff rows; blank note
        User u2 = new User();
        u2.setId(11L);
        u2.setEmail("noname@example.com");
        u2.setHomePhone("+1-2025550188");
        when(currentUserSvc.getCurrentUser()).thenReturn(u2);
        when(subSvc.hasActiveSubscription(11L)).thenReturn(true);
        when(cbRepo.findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(11L), eq(SupportCallbackStatus.PENDING), any())).thenReturn(Optional.empty());
        when(roleRepo.findByRoleCodeInAndActiveTrue(anySet())).thenReturn(List.of());

        SupportCallbackRequestDto dto2 = service.requestCallback(new CreateSupportCallbackRequest("   "));
        assertEquals("noname@example.com", dto2.userName());

        // firstNonBlank((String[]) null) via reflection
        assertNull(ReflectionTestUtils.invokeMethod(SupportCallbackService.class, "firstNonBlank", (Object) null));

        // listCallbacks null vs non-null
        when(cbRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(cbRepo.findByStatusOrderByCreatedAtDesc(SupportCallbackStatus.PENDING)).thenReturn(List.of());
        assertTrue(service.listCallbacks(null).isEmpty());
        assertTrue(service.listCallbacks(SupportCallbackStatus.PENDING).isEmpty());
        assertEquals(0L, service.countPending());

        // updateStatus null status -> BadRequestException
        SupportCallbackRequest existing = new SupportCallbackRequest();
        existing.setId(77L);
        when(cbRepo.findById(77L)).thenReturn(Optional.of(existing));
        assertThrows(BadRequestException.class,
                () -> service.updateStatus(77L, new UpdateSupportCallbackStatusRequest(null)));

        // updateStatus -> PENDING and actor with null role & no staff assignment
        User actor = new User();
        actor.setId(1L);
        actor.setFirstName("First");
        actor.setLastName("Last");
        actor.setEmail("staff@example.com");
        actor.setRole(null);
        when(currentUserSvc.getCurrentUser()).thenReturn(actor);
        when(roleRepo.findByEmailIgnoreCaseAndActiveTrue("staff@example.com")).thenReturn(Optional.empty());
        service.updateStatus(77L, new UpdateSupportCallbackStatusRequest(SupportCallbackStatus.PENDING));
        assertNull(existing.getResolvedAt());

        // updateStatus -> CONTACTED with empty unread notifications
        actor.setRole(Role.ADMIN);
        when(notifRepo.findByRelatedCallbackIdAndReadFalse(77L)).thenReturn(List.of());
        service.updateStatus(77L, new UpdateSupportCallbackStatusRequest(SupportCallbackStatus.CONTACTED));
        assertNotNull(existing.getResolvedAt());

        // listMyNotifications empty & non-empty
        when(notifRepo.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc("staff@example.com"))
                .thenReturn(List.of());
        assertTrue(service.listMyNotifications().isEmpty());

        StaffInboxNotification n = new StaffInboxNotification();
        n.setId(1L);
        n.setRelatedCallbackId(77L);
        when(notifRepo.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc("staff@example.com"))
                .thenReturn(List.of(n));
        when(cbRepo.findAllById(anySet())).thenReturn(List.of(existing));
        assertEquals(1, service.listMyNotifications().size());
        assertEquals(0L, service.countMyUnreadNotifications());
    }

    @Test
    void cookieSessionCsrfAndPhoneNumberNormalizerCoverAllBranches() {
        CookieSessionCsrfRequestMatcher mDefault = new CookieSessionCsrfRequestMatcher(null);
        CookieSessionCsrfRequestMatcher mBlank = new CookieSessionCsrfRequestMatcher("   ");
        CookieSessionCsrfRequestMatcher mCustom = new CookieSessionCsrfRequestMatcher("sp_cookie");

        assertFalse(mDefault.matches(null));

        MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/api/users/me");
        assertFalse(mDefault.matches(getReq));

        MockHttpServletRequest postBearer = new MockHttpServletRequest("POST", "/api/users/me");
        postBearer.addHeader("Authorization", "Bearer valid-jwt");
        assertFalse(mCustom.matches(postBearer));

        MockHttpServletRequest postBlankBearer = new MockHttpServletRequest("POST", "/api/users/me");
        postBlankBearer.addHeader("Authorization", "Bearer    ");
        assertFalse(mCustom.matches(postBlankBearer)); // no cookies -> false

        MockHttpServletRequest postShortBearer = new MockHttpServletRequest("POST", "/api/users/me");
        postShortBearer.addHeader("Authorization", "Bearer ");
        postShortBearer.setCookies(new Cookie("other", "1"), new Cookie("sp_cookie", ""));
        assertFalse(mCustom.matches(postShortBearer));

        MockHttpServletRequest postBasic = new MockHttpServletRequest("POST", "/api/users/me");
        postBasic.addHeader("Authorization", "Basic abcdef");
        postBasic.setCookies(new Cookie("sp_cookie", null));
        assertFalse(mCustom.matches(postBasic));

        MockHttpServletRequest postCookie = new MockHttpServletRequest("POST", "/api/users/me");
        postCookie.setCookies(new Cookie("sp_cookie", "jwt-val"));
        assertTrue(mCustom.matches(postCookie));
        assertFalse(mBlank.matches(postCookie));

        // PhoneNumberNormalizer branches
        assertFalse(PhoneNumberNormalizer.isEstablished(null));
        assertFalse(PhoneNumberNormalizer.isEstablished("oauth:123456789"));
        assertFalse(PhoneNumberNormalizer.isEstablished("0000000000"));
        assertFalse(PhoneNumberNormalizer.isEstablished("12345"));
        assertTrue(PhoneNumberNormalizer.isEstablished("+91-9876543210"));

        assertFalse(PhoneNumberNormalizer.sameNumber(null, "+91-9876543210", "+91"));
        assertFalse(PhoneNumberNormalizer.sameNumber("+91-9876543210", null, "+91"));
        assertFalse(PhoneNumberNormalizer.sameNumber("   ", "+91-9876543210", "+91"));
        assertFalse(PhoneNumberNormalizer.sameNumber("+91-9876543210", "   ", "+91"));

        // Unregistered dial code fallback in splitE164 (starts with +7, 11 digits total)
        PhoneNumberNormalizer.PhoneParts p7 = PhoneNumberNormalizer.parse("+79991234567", "+91");
        assertEquals("+7", p7.countryCode());
        // Registered +91 with wrong length in E.164 falls through to length check / fallback
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("+12345", "+91"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("+123456789012345678", "+91"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("9876543210", ""));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("9876543210", "+"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("+91-000", "+91"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("+599-12345", "+91"));
        assertEquals("+599-123456789", PhoneNumberNormalizer.toStorageFormat("+599-123456789", "+91"));
    }

    @Test
    void authEmployeeUserPlanAndMediaMissedBranchesAreCovered() throws Exception {
        // 1. AuthService.register Bug 1598 validation branches
        UserRepository userRepo = mock(UserRepository.class);
        UserProfileRepository profileRepo = mock(UserProfileRepository.class);
        RefCountryRepository countryRepo = mock(RefCountryRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthenticationManager authMgr = mock(AuthenticationManager.class);
        JwtUtil jwt = mock(JwtUtil.class);
        OtpService otp = mock(OtpService.class);
        UserService userSvcMock = mock(UserService.class);
        EmailService emailSvc = mock(EmailService.class);
        LoginAccountSecurityService secSvc = mock(LoginAccountSecurityService.class);
        EmployeeAccessService empAccessMock = mock(EmployeeAccessService.class);

        AuthService authService = new AuthService(
                userRepo, profileRepo, countryRepo, encoder, authMgr, jwt, otp, userSvcMock, emailSvc, secSvc, empAccessMock);
        ReflectionTestUtils.setField(authService, "defaultCountryCode", "+91");

        String long61 = "A".repeat(61);
        String len45 = "B".repeat(45);
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest(null, "Doe", null, "a@b.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", "   ", null, "a@b.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest(long61, "Doe", null, "a@b.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", long61, null, "a@b.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", "Doe", long61, "a@b.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest(len45, len45, len45, "a@b.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", "Doe", null, null, "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", "Doe", null, "a".repeat(115) + "@example.com", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", "Doe", null, "+91-9876543210", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane", "Doe", null, "bad-email", "+91-9876543210", "Pass@123", "Pass@123", null, true, true, true, true, false)));

        // 2. EmployeeAccessService extra branches
        StaffRoleAssignmentRepository assignRepo = mock(StaffRoleAssignmentRepository.class);
        RoleDashboardAccessRepository dashRepo = mock(RoleDashboardAccessRepository.class);
        CurrentUserService curSvc = mock(CurrentUserService.class);
        EmployeeAccessService empSvc = new EmployeeAccessService(assignRepo, dashRepo, userRepo, curSvc);

        assertNull(empSvc.applyEmployeeAccessOnAuth(null));
        User noEmailUser = new User();
        assertSame(noEmailUser, empSvc.applyEmployeeAccessOnAuth(noEmailUser));

        User legacyAdmin = new User();
        legacyAdmin.setId(50L);
        legacyAdmin.setEmail("legacy@external.com");
        legacyAdmin.setRole(Role.ADMIN);
        when(assignRepo.findByEmailIgnoreCase("legacy@external.com")).thenReturn(Optional.empty());
        when(userRepo.findById(50L)).thenReturn(Optional.of(legacyAdmin));
        empSvc.applyEmployeeAccessOnAuth(legacyAdmin);

        empSvc.ensureSeedSuperAdmin(null);
        empSvc.ensureSeedSuperAdmin(noEmailUser);
        StaffRoleAssignment unlinkedSeed = new StaffRoleAssignment();
        unlinkedSeed.setEmail("seed@socialpairly.com");
        when(assignRepo.findByEmailIgnoreCase("seed@socialpairly.com")).thenReturn(Optional.of(unlinkedSeed));
        User seedUser = new User();
        seedUser.setId(51L);
        seedUser.setEmail("seed@socialpairly.com");
        empSvc.ensureSeedSuperAdmin(seedUser);
        assertEquals(51L, unlinkedSeed.getUserId());

        assertNull(empSvc.resolveEmployeeRoleCode(null, Role.USER));
        assertTrue(empSvc.resolveAllowedDashboards("u@x.com", Role.USER, "   ").isEmpty());
        assertFalse(empSvc.resolveAllowedDashboards("a@x.com", Role.ADMIN, "   ").isEmpty());
        when(dashRepo.findByRoleCodeAndCanAccessTrue("FINANCE")).thenReturn(List.of());
        assertFalse(empSvc.resolveAllowedDashboards("a@x.com", Role.ADMIN, "FINANCE").isEmpty());
        assertTrue(empSvc.resolveAllowedDashboards("a@x.com", Role.USER, "FINANCE").isEmpty());

        assertFalse(empSvc.hasDashboardAccess(null, DashboardKeys.DASHBOARD));
        User plainUser = new User();
        plainUser.setRole(Role.USER);
        assertFalse(empSvc.hasDashboardAccess(plainUser, DashboardKeys.DASHBOARD));
        assertFalse(empSvc.hasDashboardAccess(legacyAdmin, "NOT_A_DASHBOARD"));

        // Setup SUPER_ADMIN current user for admin operations
        User superActor = new User();
        superActor.setId(1L);
        superActor.setEmail("super@socialpairly.com");
        superActor.setRole(Role.ADMIN);
        StaffRoleAssignment superRow = new StaffRoleAssignment();
        superRow.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        superRow.setActive(true);
        when(curSvc.getCurrentUser()).thenReturn(superActor);
        when(assignRepo.findByEmailIgnoreCaseAndActiveTrue("super@socialpairly.com")).thenReturn(Optional.of(superRow));
        RoleDashboardAccess ra = new RoleDashboardAccess();
        ra.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        ra.setDashboardKey(DashboardKeys.ROLE_ACCESS);
        ra.setCanAccess(true);
        when(dashRepo.findByRoleCodeAndCanAccessTrue(EmployeeRoleCodes.SUPER_ADMIN)).thenReturn(List.of(ra));

        StaffRoleAssignment orphanRow = new StaffRoleAssignment();
        orphanRow.setEmail("orphan@socialpairly.com");
        orphanRow.setRoleCode("TESTER");
        when(assignRepo.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(orphanRow));
        when(userRepo.findByEmail("orphan@socialpairly.com")).thenReturn(Optional.empty());
        assertEquals(1, empSvc.listAssignments().size());

        when(assignRepo.findById(99L)).thenReturn(Optional.of(orphanRow));
        when(assignRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        empSvc.updateAssignment(99L, new UpdateEmployeeRoleRequest(EmployeeRoleCodes.SUPER_ADMIN, null, null));

        assertThrows(BadRequestException.class, () -> empSvc.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest("BAD_ROLE", DashboardKeys.DASHBOARD, true)));
        assertThrows(BadRequestException.class, () -> empSvc.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest("FINANCE", "BAD_KEY", true)));
        assertThrows(BadRequestException.class, () -> empSvc.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest(EmployeeRoleCodes.SUPER_ADMIN, DashboardKeys.DASHBOARD, false)));
        assertThrows(BadRequestException.class, () -> empSvc.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest("FINANCE", DashboardKeys.ROLE_ACCESS, true)));

        // 3. PlanUpgradeService sorting & token parsing branches
        PlanUpgradeRequestRepository upgRepo = mock(PlanUpgradeRequestRepository.class);
        RefundRepository refRepo = mock(RefundRepository.class);
        SubscriptionRepository subRepo = mock(SubscriptionRepository.class);
        PlanRepository planRepo = mock(PlanRepository.class);
        PaymentRepository payRepo = mock(PaymentRepository.class);
        PlanUpgradeService upgSvc = new PlanUpgradeService(upgRepo, refRepo, subRepo, planRepo, payRepo, userRepo, curSvc);

        User member = new User();
        member.setId(20L);
        member.setUserTokens(10);
        when(curSvc.getCurrentUser()).thenReturn(member);
        Plan curPlan = new Plan();
        curPlan.setPlanName("Starter");
        curPlan.setAmount(BigDecimal.valueOf(10));
        curPlan.setTokensIncluded("100");
        Subscription activeSub = new Subscription();
        activeSub.setPlan(curPlan);
        when(subRepo.findActiveSubscriptionsForUser(eq(20L), any())).thenReturn(List.of(activeSub));

        Plan pA = new Plan();
        pA.setPlanName("Gold A");
        pA.setAmount(BigDecimal.valueOf(50));
        pA.setDurationDays(null);
        pA.setDescription("Includes 2,500 tokens");
        Plan pB = new Plan();
        pB.setPlanName(null);
        pB.setAmount(BigDecimal.valueOf(50));
        pB.setDurationDays(30);
        pB.setTokensIncluded("UNLIMITED");
        when(planRepo.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(pB, pA));
        assertEquals(2, upgSvc.getEligibleUpgradePlans().size());

        // 4. UserMediaService name formatting & duplicate backfill
        UserMediaRepository mediaRepo = mock(UserMediaRepository.class);
        TextModerationService txtMod = mock(TextModerationService.class);
        ImageModerationService imgMod = mock(ImageModerationService.class);
        UserMediaService mediaSvc = new UserMediaService(mediaRepo, userRepo, txtMod, imgMod);

        User mediaOwner = new User();
        mediaOwner.setId(30L);
        mediaOwner.setPreferredName("  Pref ");
        UserMedia legacyMedia = new UserMedia();
        legacyMedia.setMediaType(MediaType.PHOTO);
        legacyMedia.setContentHash(null);
        legacyMedia.setMediaData("same-bytes".getBytes());
        when(mediaRepo.findByUserId(30L)).thenReturn(List.of(legacyMedia));
        MockMultipartFile dupFile = new MockMultipartFile("file", "a.jpg", "image/jpeg", "same-bytes".getBytes());
        assertThrows(BadRequestException.class, () -> mediaSvc.uploadMedia(mediaOwner, dupFile));
        assertNotNull(legacyMedia.getContentHash());

        assertEquals("Unknown User", ReflectionTestUtils.invokeMethod(UserMediaService.class, "formatAdminUserName", (User) null));
        assertEquals("Pref", ReflectionTestUtils.invokeMethod(UserMediaService.class, "formatAdminUserName", " Pref ", null, null, null));
        assertEquals("a@b.com", ReflectionTestUtils.invokeMethod(UserMediaService.class, "formatAdminUserName", " ", null, null, " a@b.com "));
        assertEquals("Unknown User", ReflectionTestUtils.invokeMethod(UserMediaService.class, "formatAdminUserName", null, "", "", "  "));
    }
}
