package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.EmployeeRoleCodes;
import com.SocialPairly_Workflow_Manager.dto.CreateSupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.dto.UpdateSupportCallbackStatusRequest;
import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ForbiddenException;
import com.SocialPairly_Workflow_Manager.repository.StaffInboxNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import com.SocialPairly_Workflow_Manager.repository.SupportCallbackRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportCallbackServiceTest {

    @Mock private SupportCallbackRequestRepository callbackRepository;
    @Mock private StaffInboxNotificationRepository notificationRepository;
    @Mock private StaffRoleAssignmentRepository staffRoleAssignmentRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private EmailService emailService;

    @InjectMocks
    private SupportCallbackService service;

    private User premiumUser;

    @BeforeEach
    void setUp() {
        premiumUser = new User();
        premiumUser.setId(11L);
        premiumUser.setFirstName("Priya");
        premiumUser.setLastName("Shah");
        premiumUser.setEmail("priya@example.com");
        premiumUser.setPhoneNumber("+916282902802");
    }

    @Test
    void requestCallbackRequiresPremium() {
        when(currentUserService.getCurrentUser()).thenReturn(premiumUser);
        when(subscriptionService.hasActiveSubscription(11L)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> service.requestCallback(new CreateSupportCallbackRequest(null)));
        verifyNoInteractions(callbackRepository);
    }

    @Test
    void requestCallbackCreatesRowAndNotifiesStaff() {
        when(currentUserService.getCurrentUser()).thenReturn(premiumUser);
        when(subscriptionService.hasActiveSubscription(11L)).thenReturn(true);
        when(callbackRepository.findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(11L), eq(SupportCallbackStatus.PENDING), any())).thenReturn(Optional.empty());
        when(callbackRepository.save(any(SupportCallbackRequest.class))).thenAnswer(inv -> {
            SupportCallbackRequest row = inv.getArgument(0);
            row.setId(99L);
            return row;
        });

        StaffRoleAssignment superAdmin = new StaffRoleAssignment();
        superAdmin.setEmail("super@socialpairly.com");
        superAdmin.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        superAdmin.setUserId(1L);
        StaffRoleAssignment admin = new StaffRoleAssignment();
        admin.setEmail("admin@socialpairly.com");
        admin.setRoleCode(EmployeeRoleCodes.ADMIN);
        admin.setUserId(2L);
        StaffRoleAssignment cs = new StaffRoleAssignment();
        cs.setEmail("care@socialpairly.com");
        cs.setRoleCode(EmployeeRoleCodes.CUSTOMER_SUPPORT);
        cs.setUserId(3L);
        when(staffRoleAssignmentRepository.findByRoleCodeInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(superAdmin, admin, cs));

        var result = service.requestCallback(new CreateSupportCallbackRequest("Please call after 6pm"));

        assertEquals(99L, result.id());
        assertEquals(SupportCallbackStatus.PENDING, result.status());
        assertEquals("+916282902802", result.phoneNumber());
        verify(notificationRepository).saveAll(argThat(list -> ((List<?>) list).size() == 3));
        verify(emailService).sendPremiumCallbackRequestEmails(anyList(), any(SupportCallbackRequest.class));
    }

    @Test
    void requestCallbackBlocksDuplicatePending() {
        when(currentUserService.getCurrentUser()).thenReturn(premiumUser);
        when(subscriptionService.hasActiveSubscription(11L)).thenReturn(true);
        when(callbackRepository.findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(11L), eq(SupportCallbackStatus.PENDING), any()))
                .thenReturn(Optional.of(new SupportCallbackRequest()));

        assertThrows(BadRequestException.class,
                () -> service.requestCallback(new CreateSupportCallbackRequest(null)));
    }

    @Test
    void updateStatusRecordsStaffActorAndMarksAllRecipientsRead() {
        SupportCallbackRequest row = new SupportCallbackRequest();
        row.setId(5L);
        row.setStatus(SupportCallbackStatus.PENDING);
        when(callbackRepository.findById(5L)).thenReturn(Optional.of(row));
        when(currentUserService.getCurrentUser()).thenReturn(premiumUser);
        when(callbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        StaffRoleAssignment cs = new StaffRoleAssignment();
        cs.setRoleCode(EmployeeRoleCodes.CUSTOMER_SUPPORT);
        when(staffRoleAssignmentRepository.findByEmailIgnoreCaseAndActiveTrue("priya@example.com"))
                .thenReturn(Optional.of(cs));
        StaffInboxNotification forAdmin = new StaffInboxNotification();
        StaffInboxNotification forSupport = new StaffInboxNotification();
        when(notificationRepository.findByRelatedCallbackIdAndReadFalse(5L))
                .thenReturn(List.of(forAdmin, forSupport));

        var updated = service.updateStatus(5L, new UpdateSupportCallbackStatusRequest(SupportCallbackStatus.CONTACTED));

        assertEquals(SupportCallbackStatus.CONTACTED, updated.status());
        assertEquals("priya@example.com", updated.resolvedByEmail());
        assertNotNull(updated.resolvedAt());
        assertEquals(11L, updated.statusUpdatedByUserId());
        assertEquals("Priya Shah", updated.statusUpdatedByName());
        assertEquals(EmployeeRoleCodes.CUSTOMER_SUPPORT, updated.statusUpdatedByRole());
        assertNotNull(updated.statusUpdatedAt());
        assertTrue(forAdmin.isRead());
        assertTrue(forSupport.isRead());
        verify(notificationRepository).saveAll(List.of(forAdmin, forSupport));
    }

    @Test
    void reopenRecordsActorButLeavesNotificationsAlone() {
        SupportCallbackRequest row = new SupportCallbackRequest();
        row.setId(6L);
        row.setStatus(SupportCallbackStatus.CLOSED);
        when(callbackRepository.findById(6L)).thenReturn(Optional.of(row));
        when(currentUserService.getCurrentUser()).thenReturn(premiumUser);
        when(callbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(staffRoleAssignmentRepository.findByEmailIgnoreCaseAndActiveTrue(anyString())).thenReturn(Optional.empty());

        var updated = service.updateStatus(6L, new UpdateSupportCallbackStatusRequest(SupportCallbackStatus.PENDING));

        assertEquals(SupportCallbackStatus.PENDING, updated.status());
        assertNull(updated.resolvedAt());
        assertEquals(11L, updated.statusUpdatedByUserId());
        verify(notificationRepository, never()).findByRelatedCallbackIdAndReadFalse(any());
    }

    @Test
    void listMyNotificationsIncludesHandlerDetails() {
        when(currentUserService.getCurrentUser()).thenReturn(premiumUser);
        StaffInboxNotification n = new StaffInboxNotification();
        n.setId(1L);
        n.setRelatedCallbackId(7L);
        n.setRead(true);
        StaffInboxNotification orphan = new StaffInboxNotification();
        orphan.setId(2L);
        when(notificationRepository.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc("priya@example.com"))
                .thenReturn(List.of(n, orphan));
        SupportCallbackRequest cb = new SupportCallbackRequest();
        cb.setId(7L);
        cb.setStatus(SupportCallbackStatus.CLOSED);
        cb.setStatusUpdatedByUserId(2L);
        cb.setStatusUpdatedByName("Asha Admin");
        cb.setStatusUpdatedByRole(EmployeeRoleCodes.ADMIN);
        when(callbackRepository.findAllById(Set.of(7L))).thenReturn(List.of(cb));

        var result = service.listMyNotifications();

        assertEquals(2, result.size());
        assertEquals(SupportCallbackStatus.CLOSED, result.get(0).callbackStatus());
        assertEquals("Asha Admin", result.get(0).handledByName());
        assertEquals(2L, result.get(0).handledByUserId());
        assertNull(result.get(1).callbackStatus());
    }
}
