package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.CallbackTimeSlots;
import com.SocialPairly_Workflow_Manager.constants.EmployeeRoleCodes;
import com.SocialPairly_Workflow_Manager.dto.CreateSupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.dto.StaffInboxNotificationDto;
import com.SocialPairly_Workflow_Manager.dto.SupportCallbackRequestDto;
import com.SocialPairly_Workflow_Manager.dto.UpdateSupportCallbackStatusRequest;
import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ForbiddenException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.StaffInboxNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import com.SocialPairly_Workflow_Manager.repository.SupportCallbackRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupportCallbackService {

    public static final String NOTIFICATION_TYPE_CALLBACK = "CALLBACK_REQUEST";

    private static final Logger log = LoggerFactory.getLogger(SupportCallbackService.class);
    private static final Set<String> NOTIFY_ROLES = Set.of(
            EmployeeRoleCodes.SUPER_ADMIN,
            EmployeeRoleCodes.ADMIN,
            EmployeeRoleCodes.CUSTOMER_SUPPORT
    );
    private static final int DUPLICATE_WINDOW_MINUTES = 15;

    private final SupportCallbackRequestRepository callbackRepository;
    private final StaffInboxNotificationRepository notificationRepository;
    private final StaffRoleAssignmentRepository staffRoleAssignmentRepository;
    private final CurrentUserService currentUserService;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;

    public SupportCallbackService(SupportCallbackRequestRepository callbackRepository,
                                  StaffInboxNotificationRepository notificationRepository,
                                  StaffRoleAssignmentRepository staffRoleAssignmentRepository,
                                  CurrentUserService currentUserService,
                                  SubscriptionService subscriptionService,
                                  EmailService emailService) {
        this.callbackRepository = callbackRepository;
        this.notificationRepository = notificationRepository;
        this.staffRoleAssignmentRepository = staffRoleAssignmentRepository;
        this.currentUserService = currentUserService;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
    }

    @Transactional
    public SupportCallbackRequestDto requestCallback(CreateSupportCallbackRequest request) {
        User user = currentUserService.getCurrentUser();
        if (!subscriptionService.hasActiveSubscription(user.getId())) {
            throw new ForbiddenException("Callback requests are available to premium members only.");
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(DUPLICATE_WINDOW_MINUTES);
        callbackRepository
                .findFirstByUser_IdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        user.getId(), SupportCallbackStatus.PENDING, windowStart)
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "You already requested a callback recently. Our team will reach out shortly.");
                });

        String phone = firstNonBlank(user.getPhoneNumber(), user.getSecondaryPhone(), user.getHomePhone());
        if (phone == null) {
            throw new BadRequestException("Add a phone number to your profile before requesting a callback.");
        }

        LocalDate preferredDate = request != null
                ? CallbackTimeSlots.parsePreferredDate(request.preferredDate())
                : null;
        String normalizedSlot = request != null
                ? CallbackTimeSlots.normalizeSlot(request.timeSlot())
                : null;
        if (preferredDate == null || normalizedSlot == null) {
            throw new BadRequestException("Select both a day and a 2-hour time slot for the callback.");
        }

        SupportCallbackRequest row = new SupportCallbackRequest();
        row.setUser(user);
        row.setUserName(formatName(user));
        row.setUserEmail(user.getEmail());
        row.setPhoneNumber(phone.trim());
        row.setPreferredDate(preferredDate);
        row.setPreferredDay(CallbackTimeSlots.dayNameFor(preferredDate));
        row.setTimeSlot(normalizedSlot);
        if (request != null && request.note() != null && !request.note().isBlank()) {
            row.setNote(request.note().trim());
        }

        SupportCallbackRequest saved = callbackRepository.save(row);
        notifyStaff(saved);
        log.info("Premium callback requested id={} userId={} date={} slot={}",
                saved.getId(), user.getId(), saved.getPreferredDate(), saved.getTimeSlot());
        return SupportCallbackRequestDto.from(saved);
    }

    @Transactional(readOnly = true)
    public SupportCallbackRequestDto getMyActiveCallback() {
        User user = currentUserService.getCurrentUser();
        return callbackRepository
                .findFirstByUser_IdAndStatusOrderByCreatedAtDesc(user.getId(), SupportCallbackStatus.PENDING)
                .map(SupportCallbackRequestDto::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SupportCallbackRequestDto> listCallbacks(SupportCallbackStatus status) {
        List<SupportCallbackRequest> rows = status == null
                ? callbackRepository.findAllByOrderByCreatedAtDesc()
                : callbackRepository.findByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(SupportCallbackRequestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return callbackRepository.countByStatus(SupportCallbackStatus.PENDING);
    }

    @Transactional
    public SupportCallbackRequestDto updateStatus(Long id, UpdateSupportCallbackStatusRequest request) {
        SupportCallbackRequest row = callbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Callback request not found: " + id));
        SupportCallbackStatus next = request.status();
        if (next == null) {
            throw new BadRequestException("Status is required");
        }
        User actor = currentUserService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        row.setStatus(next);
        row.setStatusUpdatedByUserId(actor.getId());
        row.setStatusUpdatedByName(formatName(actor));
        row.setStatusUpdatedByEmail(actor.getEmail());
        row.setStatusUpdatedByRole(resolveStaffRole(actor));
        row.setStatusUpdatedAt(now);
        if (next == SupportCallbackStatus.PENDING) {
            row.setResolvedAt(null);
            row.setResolvedByEmail(null);
        } else {
            row.setResolvedAt(now);
            row.setResolvedByEmail(actor.getEmail());
            markCallbackNotificationsRead(row.getId());
        }
        SupportCallbackRequest saved = callbackRepository.save(row);
        log.info("Support callback id={} -> {} by userId={} role={}",
                saved.getId(), next, actor.getId(), saved.getStatusUpdatedByRole());
        return SupportCallbackRequestDto.from(saved);
    }

    /**
     * Active inbox only: unread callback notifications.
     * Read rows stay in {@code staff_inbox_notifications} for audit; handled callbacks
     * remain in {@code support_callback_requests} with status/actor timestamps.
     */
    @Transactional(readOnly = true)
    public List<StaffInboxNotificationDto> listMyNotifications() {
        User user = currentUserService.getCurrentUser();
        List<StaffInboxNotification> rows =
                notificationRepository.findByRecipientEmailIgnoreCaseAndTypeAndReadFalseOrderByCreatedAtDesc(
                        user.getEmail(), NOTIFICATION_TYPE_CALLBACK);
        Set<Long> callbackIds = rows.stream()
                .map(StaffInboxNotification::getRelatedCallbackId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SupportCallbackRequest> callbacks = callbackIds.isEmpty()
                ? Map.of()
                : callbackRepository.findAllById(callbackIds).stream()
                        .collect(Collectors.toMap(SupportCallbackRequest::getId, Function.identity()));
        return rows.stream()
                .map(n -> StaffInboxNotificationDto.from(n, callbacks.get(n.getRelatedCallbackId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countMyUnreadNotifications() {
        User user = currentUserService.getCurrentUser();
        return notificationRepository.countByRecipientEmailIgnoreCaseAndTypeAndReadFalse(
                user.getEmail(), NOTIFICATION_TYPE_CALLBACK);
    }

    /** Handled for one staff member means handled for all: clear every recipient's unread copy. */
    private void markCallbackNotificationsRead(Long callbackId) {
        List<StaffInboxNotification> unread = notificationRepository.findByRelatedCallbackIdAndReadFalse(callbackId);
        if (unread.isEmpty()) {
            return;
        }
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private String resolveStaffRole(User actor) {
        return staffRoleAssignmentRepository.findByEmailIgnoreCaseAndActiveTrue(actor.getEmail())
                .map(StaffRoleAssignment::getRoleCode)
                .orElseGet(() -> actor.getRole() != null ? actor.getRole().name() : null);
    }

    private void notifyStaff(SupportCallbackRequest callback) {
        List<StaffRoleAssignment> staff = staffRoleAssignmentRepository.findByRoleCodeInAndActiveTrue(NOTIFY_ROLES);
        Map<String, StaffRoleAssignment> uniqueByEmail = new LinkedHashMap<>();
        for (StaffRoleAssignment assignment : staff) {
            if (assignment.getEmail() == null || assignment.getEmail().isBlank()) {
                continue;
            }
            String email = assignment.getEmail().trim().toLowerCase(Locale.ROOT);
            uniqueByEmail.putIfAbsent(email, assignment);
        }

        String scheduleSummary = CallbackTimeSlots.formatScheduleSummary(
                callback.getPreferredDate(),
                callback.getPreferredDay(),
                callback.getTimeSlot()
        );
        String title = scheduleSummary != null
                ? "Premium callback scheduled (" + CallbackTimeSlots.labelFor(callback.getTimeSlot()) + ")"
                : "Premium callback requested";
        String message = scheduleSummary != null
                ? callback.getUserName() + " (" + callback.getPhoneNumber()
                        + ") selected slot: " + scheduleSummary + "."
                : callback.getUserName() + " (" + callback.getPhoneNumber()
                        + ") requested a support callback.";

        List<StaffInboxNotification> notifications = new ArrayList<>();
        for (StaffRoleAssignment assignment : uniqueByEmail.values()) {
            StaffInboxNotification notification = new StaffInboxNotification();
            notification.setRecipientUserId(assignment.getUserId());
            notification.setRecipientEmail(assignment.getEmail());
            notification.setRecipientRoleCode(assignment.getRoleCode());
            notification.setType(NOTIFICATION_TYPE_CALLBACK);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setRelatedCallbackId(callback.getId());
            notification.setRead(false);
            notifications.add(notification);
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }

        emailService.sendPremiumCallbackRequestEmails(
                uniqueByEmail.keySet().stream().toList(),
                callback
        );
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
