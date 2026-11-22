package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.controller.AdminRatingsController;
import com.SocialPairly_Workflow_Manager.controller.AppRatingController;
import com.SocialPairly_Workflow_Manager.dto.CreateAppRatingRequest;
import com.SocialPairly_Workflow_Manager.entity.AppRating;
import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.AppRatingRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffInboxNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppRatingServiceTest {

    @Mock private AppRatingRepository ratingRepository;
    @Mock private StaffInboxNotificationRepository notificationRepository;
    @Mock private StaffRoleAssignmentRepository staffRoleAssignmentRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private AppRatingService service;

    private User member;

    @BeforeEach
    void setUp() {
        member = new User();
        member.setId(21L);
        member.setFirstName("Rahul");
        member.setLastName("Seena");
        member.setEmail("rahul@example.com");
    }

    @Test
    void submitRatingPersistsStarsAndSuggestion() {
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(ratingRepository.findFirstByUser_IdOrderByCreatedAtDesc(21L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(AppRating.class))).thenAnswer(inv -> {
            AppRating row = inv.getArgument(0);
            row.setId(9L);
            return row;
        });
        StaffRoleAssignment admin = new StaffRoleAssignment();
        admin.setEmail("admin@socialpairly.com");
        admin.setRoleCode("ADMIN");
        admin.setUserId(2L);
        when(staffRoleAssignmentRepository.findByRoleCodeInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(admin));

        var dto = service.submitRating(new CreateAppRatingRequest(5, "  Love the matching!  "));

        assertEquals(9L, dto.id());
        assertEquals(5, dto.stars());
        assertEquals("Love the matching!", dto.suggestion());
        assertEquals("Rahul Seena", dto.userName());
        assertEquals("rahul@example.com", dto.userEmail());
        verify(notificationRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    void submitRatingOverwritesExistingReviewWithoutNewStaffNotification() {
        when(currentUserService.getCurrentUser()).thenReturn(member);
        AppRating existing = new AppRating();
        existing.setId(9L);
        existing.setUser(member);
        existing.setUserName("Rahul Seena");
        existing.setUserEmail("rahul@example.com");
        existing.setStars(3);
        existing.setSuggestion("Okay");
        when(ratingRepository.findFirstByUser_IdOrderByCreatedAtDesc(21L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(AppRating.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.submitRating(new CreateAppRatingRequest(5, "Much better now"));

        assertEquals(9L, dto.id());
        assertEquals(5, dto.stars());
        assertEquals("Much better now", dto.suggestion());
        verify(notificationRepository, never()).saveAll(any());
        verify(staffRoleAssignmentRepository, never()).findByRoleCodeInAndActiveTrue(anyCollection());
    }

    @Test
    void submitRatingRejectsInvalidStars() {
        assertThrows(BadRequestException.class, () -> service.submitRating(null));
        assertThrows(BadRequestException.class, () -> service.submitRating(new CreateAppRatingRequest(0, null)));
        assertThrows(BadRequestException.class, () -> service.submitRating(new CreateAppRatingRequest(6, "x")));
    }

    @Test
    void myLatestAndListAndSummaryCoverBranches() {
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(ratingRepository.findFirstByUser_IdOrderByCreatedAtDesc(21L)).thenReturn(Optional.empty());
        assertNull(service.getMyLatestRating());

        AppRating existing = new AppRating();
        existing.setId(3L);
        existing.setUser(member);
        existing.setUserName("Rahul Seena");
        existing.setUserEmail("rahul@example.com");
        existing.setStars(4);
        existing.setSuggestion("Great app");
        when(ratingRepository.findFirstByUser_IdOrderByCreatedAtDesc(21L)).thenReturn(Optional.of(existing));
        assertEquals(4, service.getMyLatestRating().stars());

        when(ratingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(existing));
        assertEquals(1, service.listAllRatings().size());

        when(ratingRepository.findAll()).thenReturn(List.of());
        Map<String, Object> emptySummary = service.summary();
        assertEquals(0L, emptySummary.get("totalRatings"));
        assertEquals(0.0, emptySummary.get("averageStars"));

        when(ratingRepository.findAll()).thenReturn(List.of(existing));
        when(ratingRepository.countByStars(5)).thenReturn(0L);
        when(ratingRepository.countByStars(4)).thenReturn(1L);
        when(ratingRepository.countByStars(3)).thenReturn(0L);
        when(ratingRepository.countByStars(2)).thenReturn(0L);
        when(ratingRepository.countByStars(1)).thenReturn(0L);
        Map<String, Object> summary = service.summary();
        assertEquals(1L, summary.get("totalRatings"));
        assertEquals(4.0, summary.get("averageStars"));
    }

    @Test
    void unreadRatingNotificationsCanBeCountedAndCleared() {
        when(currentUserService.getCurrentUser()).thenReturn(member);
        when(notificationRepository.countByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
                "rahul@example.com", AppRatingService.NOTIFICATION_TYPE_RATING)).thenReturn(2L);
        assertEquals(2L, service.countMyUnreadRatingNotifications());

        StaffInboxNotification unread = new StaffInboxNotification();
        unread.setRead(false);
        when(notificationRepository.findByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
                "rahul@example.com", AppRatingService.NOTIFICATION_TYPE_RATING))
                .thenReturn(List.of(unread));
        assertEquals(1L, service.markMyRatingNotificationsRead());
        assertTrue(unread.isRead());
        verify(notificationRepository).saveAll(List.of(unread));
    }

    @Test
    void controllersWireThroughService() {
        AppRatingService svc = mock(AppRatingService.class);
        when(svc.submitRating(any())).thenReturn(null);
        when(svc.getMyLatestRating()).thenReturn(null);
        when(svc.listAllRatings()).thenReturn(List.of());
        when(svc.summary()).thenReturn(Map.of("totalRatings", 0L));
        when(svc.countMyUnreadRatingNotifications()).thenReturn(0L);
        when(svc.markMyRatingNotificationsRead()).thenReturn(0L);

        AppRatingController memberCtrl = new AppRatingController(svc);
        assertEquals(200, memberCtrl.submit(new CreateAppRatingRequest(5, "Nice")).getStatusCode().value());
        assertEquals(204, memberCtrl.myLatest().getStatusCode().value());

        AdminRatingsController adminCtrl = new AdminRatingsController(svc);
        assertEquals(200, adminCtrl.list().getStatusCode().value());
        assertEquals(200, adminCtrl.summary().getStatusCode().value());
        assertEquals(200, adminCtrl.unreadCount().getStatusCode().value());
        assertEquals(200, adminCtrl.markRead().getStatusCode().value());
    }

    @Test
    void entityLifecycleDefaultsCreatedAt() {
        AppRating row = new AppRating();
        row.onCreate();
        assertNotNull(row.getCreatedAt());
        assertNotNull(row.getUpdatedAt());
        row.onUpdate();
        assertNotNull(row.getUpdatedAt());
    }
}
