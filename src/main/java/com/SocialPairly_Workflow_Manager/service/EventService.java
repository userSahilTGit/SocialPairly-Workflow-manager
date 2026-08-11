package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventAdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.EventDetailDto;
import com.SocialPairly_Workflow_Manager.dto.EventParticipantDto;
import com.SocialPairly_Workflow_Manager.dto.EventSummaryDto;
import com.SocialPairly_Workflow_Manager.dto.EventUpsertRequest;
import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.EventParticipant;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.VerificationStatus;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.EventDetailRepository;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmailService emailService;

    public EventService(EventDetailRepository eventDetailRepository,
                        EventParticipantRepository eventParticipantRepository,
                        UserRepository userRepository,
                        EmailService emailService) {
        this.eventDetailRepository = eventDetailRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.userRepository = userRepository;
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
        EventDetail event = buildEventEntity(request);
        event.setStatus(EventStatus.draft);
        assignParticipants(event, request.memberUserIds());
        EventDetail saved = eventDetailRepository.save(event);
        log.info("Created draft event id={} code={}", saved.getId(), saved.getEventCode());
        return getEvent(saved.getId());
    }

    @Transactional
    public EventDetailDto updateEvent(Long eventId, EventUpsertRequest request) {
        EventDetail event = findEventOrThrow(eventId);
        applyEventFields(event, request);

        if (event.getStatus() == EventStatus.draft) {
            event.getParticipants().clear();
            assignParticipants(event, request.memberUserIds());
        } else if (event.getStatus() == EventStatus.published) {
            mergeParticipants(event, request.memberUserIds());
        } else {
            throw new BadRequestException("This event cannot be edited");
        }

        EventDetail saved = eventDetailRepository.save(event);

        if (event.getStatus() == EventStatus.published && Boolean.TRUE.equals(request.notifyParticipants())) {
            notifyParticipantsOfUpdate(saved);
        }

        log.info("Updated event id={} status={}", saved.getId(), saved.getStatus());
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
        if (event.getParticipants() == null || event.getParticipants().isEmpty()) {
            throw new BadRequestException("Cannot publish an event without members");
        }

        event.setStatus(EventStatus.published);
        eventDetailRepository.save(event);

        List<EventParticipant> participants = eventParticipantRepository.findByEventIdWithUser(eventId);
        for (EventParticipant participant : participants) {
            participant.setRsvpStatus(RsvpStatus.pending);
            eventParticipantRepository.save(participant);
            emailService.sendEventInvitationEmail(participant.getUser(), event);
        }

        log.info("Published event id={} code={} members={}", event.getId(), event.getEventCode(), event.getParticipants().size());
        return getEvent(eventId);
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

    private EventDetail buildEventEntity(EventUpsertRequest request) {
        EventDetail event = new EventDetail();
        applyEventFields(event, request);
        return event;
    }

    private void applyEventFields(EventDetail event, EventUpsertRequest request) {
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
        event.setMemberCount(request.memberUserIds().size());
    }

    private void notifyParticipantsOfUpdate(EventDetail event) {
        List<EventParticipant> participants = eventParticipantRepository.findByEventIdWithUser(event.getId());
        for (EventParticipant participant : participants) {
            emailService.sendEventUpdateEmail(participant.getUser(), event);
        }
        log.info("Sent update notifications for event id={} to {} participants", event.getId(), participants.size());
    }

    private void mergeParticipants(EventDetail event, List<Long> memberUserIds) {
        Set<Long> desiredIds = new HashSet<>(memberUserIds);
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
                eventParticipantRepository.save(participant);
                emailService.sendEventInvitationEmail(user, event);
            }
        }
    }

    private void assignParticipants(EventDetail event, List<Long> memberUserIds) {
        Set<Long> uniqueIds = new HashSet<>(memberUserIds);
        if (uniqueIds.isEmpty()) {
            throw new BadRequestException("At least one member must be selected");
        }

        List<User> users = validateMemberUsers(uniqueIds);

        List<EventParticipant> participants = new ArrayList<>();
        for (User user : users) {
            EventParticipant participant = new EventParticipant();
            participant.setEvent(event);
            participant.setUser(user);
            participant.setRsvpStatus(RsvpStatus.pending);
            participant.setVerificationStatus(VerificationStatus.Pending);
            participants.add(participant);
        }
        event.setParticipants(participants);
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
