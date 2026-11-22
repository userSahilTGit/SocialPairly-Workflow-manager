package com.SocialPairly_Workflow_Manager.constants;

import com.SocialPairly_Workflow_Manager.controller.AdminCallbackAlertController;
import com.SocialPairly_Workflow_Manager.controller.SupportCallbackController;
import com.SocialPairly_Workflow_Manager.dto.CreateSupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.dto.StaffInboxNotificationDto;
import com.SocialPairly_Workflow_Manager.dto.SupportCallbackRequestDto;
import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffInboxNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import com.SocialPairly_Workflow_Manager.repository.SupportCallbackRequestRepository;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.EmailService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import com.SocialPairly_Workflow_Manager.service.SupportCallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CallbackTimeSlotsTest {

    @Test
    void slotNormalizationLabelsAndDateValidationCoverAllBranches() {
        assertEquals(12, CallbackTimeSlots.allSlots().size());
        assertEquals(12, CallbackTimeSlots.catalog().size());

        assertNull(CallbackTimeSlots.normalizeSlot(null));
        assertNull(CallbackTimeSlots.normalizeSlot("   "));
        assertEquals("14:00-16:00", CallbackTimeSlots.normalizeSlot("14:00-16:00"));
        assertEquals("14:00-16:00", CallbackTimeSlots.normalizeSlot("14:00–16:00"));
        assertEquals("14:00-16:00", CallbackTimeSlots.normalizeSlot("02:00 PM - 04:00 PM"));
        assertThrows(BadRequestException.class, () -> CallbackTimeSlots.normalizeSlot("09:00-11:00"));

        assertNull(CallbackTimeSlots.labelFor(null));
        assertNull(CallbackTimeSlots.labelFor("  "));
        assertEquals("02:00 PM - 04:00 PM", CallbackTimeSlots.labelFor("14:00-16:00"));
        assertEquals("CUSTOM", CallbackTimeSlots.labelFor("CUSTOM"));

        assertNull(CallbackTimeSlots.parsePreferredDate(null));
        assertNull(CallbackTimeSlots.parsePreferredDate("  "));
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertEquals(tomorrow, CallbackTimeSlots.parsePreferredDate(tomorrow.toString()));
        assertThrows(BadRequestException.class,
                () -> CallbackTimeSlots.parsePreferredDate(LocalDate.now().minusDays(5).toString()));
        assertThrows(BadRequestException.class,
                () -> CallbackTimeSlots.parsePreferredDate(LocalDate.now().plusDays(30).toString()));
        assertThrows(BadRequestException.class,
                () -> CallbackTimeSlots.parsePreferredDate("23-11-2026"));

        assertNull(CallbackTimeSlots.dayNameFor(null));
        assertNotNull(CallbackTimeSlots.dayNameFor(tomorrow));

        assertNull(CallbackTimeSlots.formatScheduleSummary(null, null, null));
        assertTrue(CallbackTimeSlots.formatScheduleSummary(tomorrow, "  ", "14:00-16:00")
                .contains("02:00 PM - 04:00 PM"));
        assertEquals("Monday", CallbackTimeSlots.formatScheduleSummary(null, "Monday", null));
        assertEquals("02:00 PM - 04:00 PM", CallbackTimeSlots.formatScheduleSummary(null, null, "14:00-16:00"));
    }

    @Test
    void scheduledCallbackServiceControllersAndEmailCoverAllSlotBranches() {
        SupportCallbackRequestRepository cbRepo = mock(SupportCallbackRequestRepository.class);
        StaffInboxNotificationRepository notifRepo = mock(StaffInboxNotificationRepository.class);
        StaffRoleAssignmentRepository roleRepo = mock(StaffRoleAssignmentRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        EmailService emailService = mock(EmailService.class);

        SupportCallbackService service = new SupportCallbackService(
                cbRepo, notifRepo, roleRepo, currentUserService, subscriptionService, emailService);

        User member = new User();
        member.setId(42L);
        member.setFirstName("Rohan");
        member.setLastName("Verma");
        member.setEmail("rohan@example.com");
        member.setPhoneNumber("+919876543210");

        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(subscriptionService.hasActiveSubscription(42L)).thenReturn(true);
        when(cbRepo.findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(42L), eq(SupportCallbackStatus.PENDING), any())).thenReturn(Optional.empty());
        when(cbRepo.save(any(SupportCallbackRequest.class))).thenAnswer(inv -> {
            SupportCallbackRequest row = inv.getArgument(0);
            row.setId(501L);
            return row;
        });

        StaffRoleAssignment cs = new StaffRoleAssignment();
        cs.setEmail("support@socialpairly.com");
        cs.setRoleCode(EmployeeRoleCodes.CUSTOMER_SUPPORT);
        cs.setUserId(7L);
        when(roleRepo.findByRoleCodeInAndActiveTrue(anyCollection())).thenReturn(List.of(cs));

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Missing slot, missing date, or both -> BadRequestException
        assertThrows(BadRequestException.class,
                () -> service.requestCallback(new CreateSupportCallbackRequest("note")));
        assertThrows(BadRequestException.class,
                () -> service.requestCallback(new CreateSupportCallbackRequest("note", tomorrow.toString(), null)));
        assertThrows(BadRequestException.class,
                () -> service.requestCallback(new CreateSupportCallbackRequest("note", null, "14:00-16:00")));

        // Valid 2-hour slot booking
        SupportCallbackRequestDto dto = service.requestCallback(
                new CreateSupportCallbackRequest("Call regarding match", tomorrow.toString(), "14:00-16:00"));
        assertEquals(tomorrow, dto.preferredDate());
        assertEquals("14:00-16:00", dto.timeSlot());
        assertEquals("02:00 PM - 04:00 PM", dto.timeSlotLabel());
        assertNotNull(dto.preferredDay());

        // Active callback lookup (present + absent)
        SupportCallbackRequest activeRow = new SupportCallbackRequest();
        activeRow.setId(501L);
        activeRow.setUser(member);
        activeRow.setUserName("Rohan Verma");
        activeRow.setUserEmail("rohan@example.com");
        activeRow.setPhoneNumber("+919876543210");
        activeRow.setPreferredDate(tomorrow);
        activeRow.setPreferredDay(CallbackTimeSlots.dayNameFor(tomorrow));
        activeRow.setTimeSlot("14:00-16:00");
        when(cbRepo.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(42L, SupportCallbackStatus.PENDING))
                .thenReturn(Optional.of(activeRow))
                .thenReturn(Optional.empty());

        SupportCallbackController userCtrl = new SupportCallbackController(service);
        assertEquals(200, userCtrl.activeCallback().getStatusCode().value());
        assertEquals(204, userCtrl.activeCallback().getStatusCode().value());
        assertEquals(12, userCtrl.listCallbackSlots().getBody().size());

        // AdminCallbackAlertController endpoints (summary, notifications, callbacks)
        when(cbRepo.countByStatus(SupportCallbackStatus.PENDING)).thenReturn(1L);
        when(notifRepo.countByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
                eq("rohan@example.com"), eq(SupportCallbackService.NOTIFICATION_TYPE_CALLBACK))).thenReturn(1L);
        when(notifRepo.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc("rohan@example.com")).thenReturn(List.of());
        when(cbRepo.findByStatusOrderByCreatedAtDesc(SupportCallbackStatus.PENDING)).thenReturn(List.of(activeRow));

        AdminCallbackAlertController alertCtrl = new AdminCallbackAlertController(service);
        assertEquals(1L, alertCtrl.summary().getBody().get("pendingCount"));
        assertTrue(alertCtrl.notifications().getBody().isEmpty());
        assertEquals(1, alertCtrl.pendingCallbacks().getBody().size());

        // StaffInboxNotificationDto with scheduled callback
        StaffInboxNotification inboxRow = new StaffInboxNotification();
        inboxRow.setId(10L);
        StaffInboxNotificationDto inboxDto = StaffInboxNotificationDto.from(inboxRow, activeRow);
        assertEquals("14:00-16:00", inboxDto.callbackTimeSlot());
        assertEquals("02:00 PM - 04:00 PM", inboxDto.callbackTimeSlotLabel());

        // EmailService.sendPremiumCallbackRequestEmails with scheduled slot
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        EmailService realEmail = new EmailService(mailSender, mock(PlanRepository.class));
        ReflectionTestUtils.setField(realEmail, "senderEmail", "noreply@socialpairly.com");
        ReflectionTestUtils.setField(realEmail, "supportEmail", "customercare@socialpairly.com");
        ReflectionTestUtils.setField(realEmail, "appName", "SocialPairly");
        ReflectionTestUtils.setField(realEmail, "frontendUrl", "http://localhost:3000");
        realEmail.sendPremiumCallbackRequestEmails(List.of("support@socialpairly.com"), activeRow);
        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));
    }
}
