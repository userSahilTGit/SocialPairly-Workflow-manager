package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.EmployeeRoleCodes;
import com.SocialPairly_Workflow_Manager.dto.AppRatingDto;
import com.SocialPairly_Workflow_Manager.dto.CreateAppRatingRequest;
import com.SocialPairly_Workflow_Manager.entity.AppRating;
import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.AppRatingRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffInboxNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AppRatingService {

    public static final String NOTIFICATION_TYPE_RATING = "APP_RATING";

    private static final Logger log = LoggerFactory.getLogger(AppRatingService.class);
    private static final Set<String> NOTIFY_ROLES = EmployeeRoleCodes.supportOpsRoles();

    private final AppRatingRepository ratingRepository;
    private final StaffInboxNotificationRepository notificationRepository;
    private final StaffRoleAssignmentRepository staffRoleAssignmentRepository;
    private final CurrentUserService currentUserService;

    public AppRatingService(AppRatingRepository ratingRepository,
                            StaffInboxNotificationRepository notificationRepository,
                            StaffRoleAssignmentRepository staffRoleAssignmentRepository,
                            CurrentUserService currentUserService) {
        this.ratingRepository = ratingRepository;
        this.notificationRepository = notificationRepository;
        this.staffRoleAssignmentRepository = staffRoleAssignmentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AppRatingDto submitRating(CreateAppRatingRequest request) {
        if (request == null || request.stars() == null) {
            throw new BadRequestException("A 1 to 5 star rating is required.");
        }
        int stars = request.stars();
        if (stars < 1 || stars > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars.");
        }

        User user = currentUserService.getCurrentUser();
        // One rating per member: later submits overwrite the previous review.
        AppRating row = ratingRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId())
                .orElseGet(AppRating::new);
        boolean isNew = row.getId() == null;
        row.setUser(user);
        row.setUserName(formatName(user));
        row.setUserEmail(user.getEmail());
        row.setStars(stars);
        if (request.suggestion() != null && !request.suggestion().isBlank()) {
            row.setSuggestion(request.suggestion().trim());
        } else {
            row.setSuggestion(null);
        }

        AppRating saved = ratingRepository.save(row);
        if (isNew) {
            notifyStaff(saved);
        }
        log.info("App rating {} id={} userId={} stars={}",
                isNew ? "created" : "updated", saved.getId(), user.getId(), stars);
        return AppRatingDto.from(saved);
    }

    @Transactional(readOnly = true)
    public AppRatingDto getMyLatestRating() {
        User user = currentUserService.getCurrentUser();
        return ratingRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId())
                .map(AppRatingDto::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AppRatingDto> listAllRatings() {
        return ratingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AppRatingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<AppRating> all = ratingRepository.findAll();
        long total = all.size();
        double average = total == 0
                ? 0.0
                : all.stream().mapToInt(AppRating::getStars).average().orElse(0.0);
        Map<String, Long> byStars = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            byStars.put(String.valueOf(i), ratingRepository.countByStars(i));
        }
        return Map.of(
                "totalRatings", total,
                "averageStars", Math.round(average * 10.0) / 10.0,
                "countsByStars", byStars
        );
    }

    @Transactional(readOnly = true)
    public long countMyUnreadRatingNotifications() {
        User user = currentUserService.getCurrentUser();
        return notificationRepository.countByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
                user.getEmail(), NOTIFICATION_TYPE_RATING);
    }

    /** Visiting the Ratings page clears the nav badge for this staff member. */
    @Transactional
    public long markMyRatingNotificationsRead() {
        User user = currentUserService.getCurrentUser();
        List<StaffInboxNotification> unread =
                notificationRepository.findByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
                        user.getEmail(), NOTIFICATION_TYPE_RATING);
        if (unread.isEmpty()) {
            return 0L;
        }
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    private void notifyStaff(AppRating rating) {
        List<StaffRoleAssignment> staff = staffRoleAssignmentRepository.findByRoleCodeInAndActiveTrue(NOTIFY_ROLES);
        Map<String, StaffRoleAssignment> uniqueByEmail = new LinkedHashMap<>();
        for (StaffRoleAssignment assignment : staff) {
            if (assignment.getEmail() == null || assignment.getEmail().isBlank()) {
                continue;
            }
            String email = assignment.getEmail().trim().toLowerCase(Locale.ROOT);
            uniqueByEmail.putIfAbsent(email, assignment);
        }

        String starsLabel = rating.getStars() + "★";
        String title = "New Rate Us feedback (" + starsLabel + ")";
        String message = rating.getUserName() + " rated SocialPairly " + starsLabel
                + (rating.getSuggestion() == null || rating.getSuggestion().isBlank()
                ? "."
                : ": " + rating.getSuggestion().trim());

        List<StaffInboxNotification> notifications = new ArrayList<>();
        for (StaffRoleAssignment assignment : uniqueByEmail.values()) {
            StaffInboxNotification notification = new StaffInboxNotification();
            notification.setRecipientUserId(assignment.getUserId());
            notification.setRecipientEmail(assignment.getEmail());
            notification.setRecipientRoleCode(assignment.getRoleCode());
            notification.setType(NOTIFICATION_TYPE_RATING);
            notification.setTitle(title);
            notification.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
            notification.setRead(false);
            notifications.add(notification);
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private static String formatName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) {
            return full;
        }
        if (user.getPreferredName() != null && !user.getPreferredName().isBlank()) {
            return user.getPreferredName().trim();
        }
        return user.getEmail();
    }
}
