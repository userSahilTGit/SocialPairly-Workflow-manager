package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventAdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.EventDetailDto;
import com.SocialPairly_Workflow_Manager.dto.EventParticipantDto;
import com.SocialPairly_Workflow_Manager.dto.EventSummaryDto;
import com.SocialPairly_Workflow_Manager.dto.EventUpsertRequest;
import com.SocialPairly_Workflow_Manager.entity.EnrollmentType;
import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventParticipant;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserEventNotification;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.entity.VerificationStatus;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.EventDetailRepository;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.UserEventNotificationRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.util.GeoDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventDetailRepository eventDetailRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserEventNotificationRepository userEventNotificationRepository;
    private final EmailService emailService;

    public EventService(EventDetailRepository eventDetailRepository,
                        EventParticipantRepository eventParticipantRepository,
                        UserRepository userRepository,
                        UserProfileRepository userProfileRepository,
                        UserEventNotificationRepository userEventNotificationRepository,
                        EmailService emailService) {
        this.eventDetailRepository = eventDetailRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userEventNotificationRepository = userEventNotificationRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public EventAdminStatsDto getAdminStats() {
        long totalUsers = userRepository.countByRole(Role.USER);
        long published = eventDetailRepository.countByStatus(EventStatus.published);
        long drafts = eventDetailRepository.countByStatus(EventStatus.draft);
        long accepted = eventParticipantRepository.countByRsvpStatus(RsvpStatus.accepted);
        return new EventAdminStatsDto(totalUsers, published, drafts, accepted);
    }

    @Transactional(readOnly = true)
    public List<EventSummaryDto> listEvents(String statusFilter) {
        List<EventDetail> events = eventDetailRepository.findAllByOrderByCreatedAtDesc();
        return events.stream()
                .filter(event -> matchesStatusFilter(event, statusFilter))
                .map(event -> {
                    int confirmed = (int) eventParticipantRepository.countByEventIdAndRsvpStatus(
                            event.getId(), RsvpStatus.accepted);
                    int members = (int) eventParticipantRepository.countByEventId(event.getId());
                    return EventSummaryDto.from(event, members, confirmed);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDetailDto getEvent(Long eventId) {
        EventDetail event = findEventOrThrow(eventId);
        List<EventParticipant> participants = eventParticipantRepository.findByEventIdWithUser(eventId);
        int confirmed = (int) participants.stream()
                .filter(p -> p.getRsvpStatus() == RsvpStatus.accepted || p.getRsvpStatus() == RsvpStatus.checked_in)
                .count();
        List<EventParticipantDto> participantDtos = participants.stream()
                .map(p -> EventParticipantDto.from(p, event.getEventCode()))
                .toList();
        return EventDetailDto.from(event, participantDtos, confirmed);
    }

    @Transactional
    public EventDetailDto createDraft(EventUpsertRequest request) {
        EnrollmentType enrollmentType = resolveEnrollmentType(request.enrollmentType());
        EventDetail event = buildEventEntity(request, enrollmentType);
        event.setStatus(EventStatus.draft);
        if (enrollmentType == EnrollmentType.SELECTED) {
            assignParticipants(event, request.memberUserIds(), "SELECTED");
        } else {
            ensureParticipantsCollection(event).clear();
            applyOpenCapacity(event, request);
        }
        EventDetail saved = eventDetailRepository.save(event);
        log.info("Created draft event id={} code={} type={}", saved.getId(), saved.getEventCode(), enrollmentType);
        return getEvent(saved.getId());
    }

    @Transactional
    public EventDetailDto updateEvent(Long eventId, EventUpsertRequest request) {
        EventDetail event = findEventOrThrow(eventId);
        EnrollmentType enrollmentType = resolveEnrollmentType(
                request.enrollmentType() != null ? request.enrollmentType()
                        : (event.getEnrollmentType() != null ? event.getEnrollmentType().name() : "SELECTED"));
        applyEventFields(event, request, enrollmentType);

        if (event.getStatus() == EventStatus.draft) {
            event.getParticipants().clear();
            if (enrollmentType == EnrollmentType.SELECTED) {
                assignParticipants(event, request.memberUserIds(), "SELECTED");
            } else {
                applyOpenCapacity(event, request);
            }
        } else if (event.getStatus() == EventStatus.published) {
            if (enrollmentType == EnrollmentType.SELECTED) {
                mergeParticipants(event, request.memberUserIds());
            }
            // OPEN published: capacity/token fields update only; do not rebuild radius list here
        } else {
            throw new BadRequestException("This event cannot be edited");
        }

        EventDetail saved = eventDetailRepository.save(event);

        if (event.getStatus() == EventStatus.published && Boolean.TRUE.equals(request.notifyParticipants())) {
            notifyParticipantsOfUpdate(saved);
        }

        log.info("Updated event id={} status={} type={}", saved.getId(), saved.getStatus(), enrollmentType);
        return getEvent(saved.getId());
    }

    @Transactional
    public void deleteDraft(Long eventId) {
        EventDetail event = findEventOrThrow(eventId);
        if (event.getStatus() != EventStatus.draft) {
            throw new BadRequestException("Only draft events can be deleted");
        }
        eventDetailRepository.delete(event);
        log.info("Deleted draft event id={}", eventId);
    }

    @Transactional
    public EventDetailDto publishEvent(Long eventId) {
        EventDetail event = findEventOrThrow(eventId);
        if (event.getStatus() != EventStatus.draft) {
            throw new BadRequestException("Only draft events can be published");
        }

        EnrollmentType type = event.getEnrollmentType() != null ? event.getEnrollmentType() : EnrollmentType.SELECTED;

        if (type == EnrollmentType.OPEN) {
            if (event.getVenueLatitude() == null || event.getVenueLongitude() == null) {
                throw new BadRequestException("Open events require venue latitude and longitude");
            }
            // Publish even with zero nearby users — later registrants in radius are invited dynamically.
            List<User> nearby = findUsersWithinRadius(
                    event.getVenueLatitude(),
                    event.getVenueLongitude(),
                    event.getNotifyRadiusMiles() != null ? event.getNotifyRadiusMiles() : 100);
            // Never replace the PersistentBag (orphanRemoval) — clear and add in place.
            ensureParticipantsCollection(event).clear();
            if (!nearby.isEmpty()) {
                assignUsers(event, nearby, "RADIUS");
            }
            int capacity = openCapacity(event);
            if (capacity > 0) {
                event.setMemberCount(capacity);
            } else {
                event.setMemberCount(Math.max(nearby.size(), 0));
            }
        } else {
            if (event.getParticipants() == null || event.getParticipants().isEmpty()) {
                throw new BadRequestException("Cannot publish an event without members");
            }
        }

        if (event.getMatchingOpensAt() == null && event.getEventDate() != null && event.getEventTime() != null) {
            event.setMatchingOpensAt(LocalDateTime.of(event.getEventDate(), event.getEventTime()));
        }

        event.setStatus(EventStatus.published);
        eventDetailRepository.save(event);

        List<EventParticipant> participants = eventParticipantRepository.findByEventIdWithUser(eventId);
        for (EventParticipant participant : participants) {
            participant.setRsvpStatus(RsvpStatus.pending);
            eventParticipantRepository.save(participant);
            // Email + in-app for every targeted user (paid and unpaid)
            notifyUserOfInvitation(participant.getUser(), event);
        }

        log.info("Published event id={} code={} type={} members={}",
                event.getId(), event.getEventCode(), type, participants.size());
        return getEvent(eventId);
    }

    /**
     * Invite a user into any published OPEN events whose venue is within their profile radius.
     * Used when location is saved and when they open Home, so new registrants still get a slot chance.
     */
    @Transactional
    public int enrollUserIntoMatchingOpenEvents(User user) {
        if (user == null || user.getRole() != Role.USER) {
            return 0;
        }
        UserProfile profile = user.getProfile();
        if (profile == null || profile.getLatitude() == null || profile.getLongitude() == null) {
            return 0;
        }

        List<EventDetail> openEvents = eventDetailRepository
                .findByStatusOrderByEventDateDescEventTimeDesc(EventStatus.published)
                .stream()
                .filter(e -> e.getEnrollmentType() == EnrollmentType.OPEN)
                .filter(e -> e.getVenueLatitude() != null && e.getVenueLongitude() != null)
                .filter(e -> e.getEventDate() == null || !e.getEventDate().isBefore(java.time.LocalDate.now()))
                .toList();

        int invited = 0;
        for (EventDetail event : openEvents) {
            int radius = event.getNotifyRadiusMiles() != null ? event.getNotifyRadiusMiles() : 100;
            if (!GeoDistance.withinMiles(
                    event.getVenueLatitude(), event.getVenueLongitude(),
                    profile.getLatitude(), profile.getLongitude(),
                    radius)) {
                continue;
            }
            if (eventParticipantRepository.findByEventIdAndUserId(event.getId(), user.getId()).isPresent()) {
                continue;
            }

            EventParticipant participant = new EventParticipant();
            participant.setEvent(event);
            participant.setUser(user);
            participant.setRsvpStatus(RsvpStatus.pending);
            participant.setVerificationStatus(VerificationStatus.Pending);
            participant.setInvitedVia("RADIUS");
            eventParticipantRepository.save(participant);
            notifyUserOfInvitation(user, event);
            invited++;
            log.info("Dynamically invited userId={} to open eventId={}", user.getId(), event.getId());
        }
        return invited;
    }

    @Transactional
    public EventParticipantDto verifyEntryCode(Long eventId, Long userId, String entryCode) {
        EventDetail event = findEventOrThrow(eventId);
        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found for this event"));

        if (participant.getEntryCode() == null || participant.getEntryCode().isBlank()) {
            throw new BadRequestException("This participant has not accepted the invitation yet");
        }
        if (!participant.getEntryCode().equalsIgnoreCase(entryCode.trim())) {
            throw new BadRequestException("Invalid entry code");
        }

        participant.setVerificationStatus(VerificationStatus.Done);
        participant.setRsvpStatus(RsvpStatus.checked_in);
        eventParticipantRepository.save(participant);
        log.info("Verified entry code for eventId={} userId={}", eventId, userId);
        return EventParticipantDto.from(participant, event.getEventCode());
    }

    private void notifyUserOfInvitation(User user, EventDetail event) {
        try {
            emailService.sendEventInvitationEmail(user, event);
        } catch (Exception ex) {
            log.warn("Failed to email invitation to userId={} eventId={}: {}", user.getId(), event.getId(), ex.getMessage());
        }
        upsertInviteNotification(user.getId(), event);
    }

    private void upsertInviteNotification(Long userId, EventDetail event) {
        UserEventNotification existing = userEventNotificationRepository
                .findByUserIdAndEventIdAndKind(userId, event.getId(), "EVENT_INVITE")
                .orElse(null);
        if (existing != null) {
            existing.setTitle("Event invitation: " + event.getTitle());
            existing.setBody(buildInviteBody(event));
            existing.setSeen(false);
            existing.setDismissed(false);
            userEventNotificationRepository.save(existing);
            return;
        }
        UserEventNotification notification = new UserEventNotification();
        notification.setUserId(userId);
        notification.setEventId(event.getId());
        notification.setKind("EVENT_INVITE");
        notification.setTitle("Event invitation: " + event.getTitle());
        notification.setBody(buildInviteBody(event));
        userEventNotificationRepository.save(notification);
    }

    private static String buildInviteBody(EventDetail event) {
        String whenWhere = event.getVenueName() + " • " + event.getEventDate() + " at " + event.getEventTime();
        if (event.getEnrollmentType() == EnrollmentType.OPEN) {
            return whenWhere + ". Complete unlock steps on Home to book your slot.";
        }
        return whenWhere + ". Open Home to review and accept your invitation.";
    }

    private List<User> findUsersWithinRadius(double venueLat, double venueLng, int radiusMiles) {
        return userProfileRepository.findUserProfilesWithCoordinates(Role.USER).stream()
                .filter(p -> GeoDistance.withinMiles(venueLat, venueLng, p.getLatitude(), p.getLongitude(), radiusMiles))
                .map(UserProfile::getUser)
                .filter(u -> u != null && u.getRole() == Role.USER)
                .toList();
    }

    private EventDetail buildEventEntity(EventUpsertRequest request, EnrollmentType enrollmentType) {
        EventDetail event = new EventDetail();
        applyEventFields(event, request, enrollmentType);
        return event;
    }

    private void applyEventFields(EventDetail event, EventUpsertRequest request, EnrollmentType enrollmentType) {
        if (event.getEventCode() == null || event.getEventCode().isBlank()) {
            String stateCode = request.stateCode().trim().toUpperCase(Locale.US);
            String fipsCode = request.fipsCode().trim();
            String prefix = stateCode + "-" + fipsCode;
            long existing = eventDetailRepository.countByEventCodePrefix(prefix);
            event.setEventCode(prefix + "-" + (existing + 1));
        }

        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setVenueName(request.venueName().trim());
        event.setLocation(request.location().trim());
        event.setEventDate(request.eventDate());
        event.setEventTime(request.eventTime());
        event.setEventPerks(request.eventPerks() != null ? new ArrayList<>(request.eventPerks()) : new ArrayList<>());
        event.setEnrollmentType(enrollmentType);
        event.setTokenCost(request.tokenCost() != null ? Math.max(0, request.tokenCost()) : 0);
        event.setMaleSlotLimit(request.maleSlotLimit());
        event.setFemaleSlotLimit(request.femaleSlotLimit());
        event.setVenueLatitude(request.venueLatitude());
        event.setVenueLongitude(request.venueLongitude());
        int radius = request.notifyRadiusMiles() != null ? request.notifyRadiusMiles() : 100;
        if (radius < 1 || radius > 100) {
            throw new BadRequestException("notifyRadiusMiles must be between 1 and 100");
        }
        event.setNotifyRadiusMiles(radius);
        if (event.getEventDate() != null && event.getEventTime() != null) {
            event.setMatchingOpensAt(LocalDateTime.of(event.getEventDate(), event.getEventTime()));
        }

        if (enrollmentType == EnrollmentType.SELECTED) {
            List<Long> members = request.memberUserIds() != null ? request.memberUserIds() : List.of();
            event.setMemberCount(members.size());
        } else {
            applyOpenCapacity(event, request);
        }
    }

    private void applyOpenCapacity(EventDetail event, EventUpsertRequest request) {
        int capacity = 0;
        if (request.maleSlotLimit() != null) {
            capacity += Math.max(0, request.maleSlotLimit());
        }
        if (request.femaleSlotLimit() != null) {
            capacity += Math.max(0, request.femaleSlotLimit());
        }
        if (capacity > 0) {
            event.setMemberCount(capacity);
        } else if (event.getMemberCount() == null) {
            event.setMemberCount(30);
        }
        if (request.venueLatitude() == null || request.venueLongitude() == null) {
            throw new BadRequestException("Open events require venueLatitude and venueLongitude");
        }
    }

    private static int openCapacity(EventDetail event) {
        int capacity = 0;
        if (event.getMaleSlotLimit() != null) {
            capacity += Math.max(0, event.getMaleSlotLimit());
        }
        if (event.getFemaleSlotLimit() != null) {
            capacity += Math.max(0, event.getFemaleSlotLimit());
        }
        return capacity;
    }

    private void notifyParticipantsOfUpdate(EventDetail event) {
        List<EventParticipant> participants = eventParticipantRepository.findByEventIdWithUser(event.getId());
        for (EventParticipant participant : participants) {
            try {
                emailService.sendEventUpdateEmail(participant.getUser(), event);
            } catch (Exception ex) {
                log.warn("Failed to email update to userId={} eventId={}", participant.getUser().getId(), event.getId());
            }
        }
        log.info("Sent update notifications for event id={} to {} participants", event.getId(), participants.size());
    }

    private void mergeParticipants(EventDetail event, List<Long> memberUserIds) {
        Set<Long> desiredIds = new HashSet<>(memberUserIds != null ? memberUserIds : List.of());
        if (desiredIds.isEmpty()) {
            throw new BadRequestException("At least one member must be selected");
        }

        validateMemberUsers(desiredIds);

        List<EventParticipant> existing = eventParticipantRepository.findByEventIdWithUser(event.getId());
        Map<Long, EventParticipant> existingByUserId = existing.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));

        List<EventParticipant> toRemove = existing.stream()
                .filter(p -> !desiredIds.contains(p.getUser().getId()))
                .toList();
        for (EventParticipant removed : toRemove) {
            eventParticipantRepository.delete(removed);
        }

        for (Long userId : desiredIds) {
            if (!existingByUserId.containsKey(userId)) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new BadRequestException("One or more selected users were not found"));
                EventParticipant participant = new EventParticipant();
                participant.setEvent(event);
                participant.setUser(user);
                participant.setRsvpStatus(RsvpStatus.pending);
                participant.setVerificationStatus(VerificationStatus.Pending);
                participant.setInvitedVia("SELECTED");
                eventParticipantRepository.save(participant);
                notifyUserOfInvitation(user, event);
            }
        }
        event.setMemberCount(desiredIds.size());
    }

    private void assignParticipants(EventDetail event, List<Long> memberUserIds, String invitedVia) {
        Set<Long> uniqueIds = new HashSet<>(memberUserIds != null ? memberUserIds : List.of());
        if (uniqueIds.isEmpty()) {
            throw new BadRequestException("At least one member must be selected");
        }
        List<User> users = validateMemberUsers(uniqueIds);
        assignUsers(event, users, invitedVia);
        event.setMemberCount(users.size());
    }

    private void assignUsers(EventDetail event, List<User> users, String invitedVia) {
        List<EventParticipant> participants = ensureParticipantsCollection(event);
        // Managed entities must mutate the existing collection (cascade orphanRemoval).
        if (!participants.isEmpty()) {
            participants.clear();
        }
        for (User user : users) {
            EventParticipant participant = new EventParticipant();
            participant.setEvent(event);
            participant.setUser(user);
            participant.setRsvpStatus(RsvpStatus.pending);
            participant.setVerificationStatus(VerificationStatus.Pending);
            participant.setInvitedVia(invitedVia);
            participants.add(participant);
        }
    }

    private static List<EventParticipant> ensureParticipantsCollection(EventDetail event) {
        if (event.getParticipants() == null) {
            event.setParticipants(new ArrayList<>());
        }
        return event.getParticipants();
    }

    private List<User> validateMemberUsers(Set<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new BadRequestException("One or more selected users were not found");
        }
        for (User user : users) {
            if (user.getRole() == Role.ADMIN) {
                throw new BadRequestException("Admin users cannot be added to events");
            }
        }
        return users;
    }

    private static EnrollmentType resolveEnrollmentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EnrollmentType.SELECTED;
        }
        try {
            return EnrollmentType.valueOf(raw.trim().toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("enrollmentType must be OPEN or SELECTED");
        }
    }

    private EventDetail findEventOrThrow(Long eventId) {
        return eventDetailRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private boolean matchesStatusFilter(EventDetail event, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "all".equalsIgnoreCase(statusFilter)) {
            return true;
        }
        try {
            EventStatus status = EventStatus.valueOf(statusFilter.toLowerCase());
            return event.getStatus() == status;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }
}
