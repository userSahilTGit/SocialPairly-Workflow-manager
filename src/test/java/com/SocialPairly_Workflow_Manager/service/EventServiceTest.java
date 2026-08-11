package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.EventAdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.EventDetailDto;
import com.SocialPairly_Workflow_Manager.dto.EventSummaryDto;
import com.SocialPairly_Workflow_Manager.dto.EventUpsertRequest;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.EventDetailRepository;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventDetailRepository eventDetailRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EventService eventService;

    private User memberUser;
    private EventUpsertRequest upsertRequest;

    @BeforeEach
    void setUp() {
        memberUser = new User();
        memberUser.setId(10L);
        memberUser.setFirstName("Member");
        memberUser.setLastName("One");
        memberUser.setEmail("member@example.com");
        memberUser.setRole(Role.USER);

        upsertRequest = new EventUpsertRequest(
                "Mixer", "Description", "Venue", "Location",
                LocalDate.of(2026, 8, 1), LocalTime.of(18, 0),
                "ny", "36061", List.of("Dinner"), List.of(10L), false
        );
    }

    @Test
    void getAdminStatsShouldAggregateCounts() {
        when(userRepository.countByRole(Role.USER)).thenReturn(50L);
        when(eventDetailRepository.countByStatus(EventStatus.published)).thenReturn(8L);
        when(eventDetailRepository.countByStatus(EventStatus.draft)).thenReturn(3L);
        when(eventParticipantRepository.countByRsvpStatus(RsvpStatus.accepted)).thenReturn(20L);

        EventAdminStatsDto stats = eventService.getAdminStats();
        assertEquals(50, stats.totalUsers());
        assertEquals(8, stats.publishedEvents());
        assertEquals(3, stats.draftEvents());
        assertEquals(20, stats.acceptedMembers());
    }

    @Test
    void listEventsShouldFilterByStatus() {
        EventDetail draft = buildEvent(1L, EventStatus.draft);
        EventDetail published = buildEvent(2L, EventStatus.published);
        when(eventDetailRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draft, published));
        when(eventParticipantRepository.countByEventIdAndRsvpStatus(anyLong(), eq(RsvpStatus.accepted))).thenReturn(1L);
        when(eventParticipantRepository.countByEventId(anyLong())).thenReturn(2L);

        List<EventSummaryDto> all = eventService.listEvents("all");
        assertEquals(2, all.size());

        List<EventSummaryDto> drafts = eventService.listEvents("draft");
        assertEquals(1, drafts.size());
        assertEquals(EventStatus.draft, drafts.get(0).status());

        List<EventSummaryDto> invalidFilter = eventService.listEvents("invalid-status");
        assertEquals(2, invalidFilter.size());
    }

    @Test
    void getEventShouldReturnDetailWithParticipants() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        EventParticipant participant = buildParticipant(event, memberUser, RsvpStatus.accepted);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(List.of(participant));

        EventDetailDto dto = eventService.getEvent(1L);
        assertEquals("Mixer", dto.title());
        assertEquals(1, dto.participants().size());
        assertEquals(1, dto.confirmedCount());
    }

    @Test
    void getEventShouldThrowWhenNotFound() {
        when(eventDetailRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> eventService.getEvent(99L));
    }

    @Test
    void createDraftShouldPersistEventWithMembers() {
        when(eventDetailRepository.countByEventCodePrefix("NY-36061")).thenReturn(0L);
        when(userRepository.findAllById(anySet())).thenReturn(List.of(memberUser));
        when(eventDetailRepository.save(any(EventDetail.class))).thenAnswer(inv -> {
            EventDetail saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(eventDetailRepository.findById(5L)).thenAnswer(inv -> {
            EventDetail saved = buildEvent(5L, EventStatus.draft);
            saved.setParticipants(new ArrayList<>(List.of(buildParticipant(saved, memberUser, RsvpStatus.pending))));
            return Optional.of(saved);
        });
        when(eventParticipantRepository.findByEventIdWithUser(5L)).thenReturn(List.of());

        EventDetailDto dto = eventService.createDraft(upsertRequest);
        assertNotNull(dto);
        verify(eventDetailRepository).save(any(EventDetail.class));
    }

    @Test
    void createDraftShouldRejectEmptyMembers() {
        EventUpsertRequest emptyMembers = new EventUpsertRequest(
                upsertRequest.title(), upsertRequest.description(), upsertRequest.venueName(),
                upsertRequest.location(), upsertRequest.eventDate(), upsertRequest.eventTime(),
                upsertRequest.stateCode(), upsertRequest.fipsCode(), upsertRequest.eventPerks(),
                List.of(), upsertRequest.notifyParticipants()
        );
        assertThrows(BadRequestException.class, () -> eventService.createDraft(emptyMembers));
    }

    @Test
    void createDraftShouldRejectAdminMembers() {
        User admin = new User();
        admin.setId(10L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findAllById(anySet())).thenReturn(List.of(admin));
        assertThrows(BadRequestException.class, () -> eventService.createDraft(upsertRequest));
    }

    @Test
    void updateDraftShouldReplaceParticipants() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        event.setParticipants(new ArrayList<>(List.of(buildParticipant(event, memberUser, RsvpStatus.pending))));
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(memberUser));
        when(eventDetailRepository.save(event)).thenReturn(event);
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(event.getParticipants());

        eventService.updateEvent(1L, upsertRequest);
        verify(eventDetailRepository).save(event);
    }

    @Test
    void updatePublishedShouldMergeParticipantsAndNotify() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        EventParticipant existing = buildParticipant(event, memberUser, RsvpStatus.accepted);
        event.setParticipants(new ArrayList<>(List.of(existing)));

        User newMember = new User();
        newMember.setId(11L);
        newMember.setRole(Role.USER);
        newMember.setEmail("new@example.com");

        EventUpsertRequest notifyRequest = new EventUpsertRequest(
                upsertRequest.title(), upsertRequest.description(), upsertRequest.venueName(),
                upsertRequest.location(), upsertRequest.eventDate(), upsertRequest.eventTime(),
                upsertRequest.stateCode(), upsertRequest.fipsCode(), upsertRequest.eventPerks(),
                List.of(10L, 11L), true
        );

        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(List.of(existing));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(memberUser, newMember));
        when(userRepository.findById(11L)).thenReturn(Optional.of(newMember));
        when(eventDetailRepository.save(event)).thenReturn(event);
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(List.of(existing));

        eventService.updateEvent(1L, notifyRequest);
        verify(emailService).sendEventUpdateEmail(eq(memberUser), eq(event));
        verify(emailService).sendEventInvitationEmail(eq(newMember), eq(event));
    }

    @Test
    void updatePublishedShouldRejectEmptyMembers() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        event.setParticipants(new ArrayList<>());
        EventUpsertRequest emptyMembers = new EventUpsertRequest(
                upsertRequest.title(), upsertRequest.description(), upsertRequest.venueName(),
                upsertRequest.location(), upsertRequest.eventDate(), upsertRequest.eventTime(),
                upsertRequest.stateCode(), upsertRequest.fipsCode(), upsertRequest.eventPerks(),
                List.of(), false
        );
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> eventService.updateEvent(1L, emptyMembers));
    }

    @Test
    void updateEventShouldRejectNonEditableStatus() {
        EventDetail event = buildEvent(1L, EventStatus.completed);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> eventService.updateEvent(1L, upsertRequest));
    }

    @Test
    void deleteDraftShouldRemoveDraftEvents() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        eventService.deleteDraft(1L);
        verify(eventDetailRepository).delete(event);
    }

    @Test
    void deleteDraftShouldRejectPublishedEvents() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> eventService.deleteDraft(1L));
        verify(eventDetailRepository, never()).delete(any());
    }

    @Test
    void publishEventShouldSendInvitations() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        EventParticipant participant = buildParticipant(event, memberUser, RsvpStatus.pending);
        event.setParticipants(new ArrayList<>(List.of(participant)));

        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventDetailRepository.save(event)).thenReturn(event);
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(List.of(participant));
        when(eventParticipantRepository.save(participant)).thenReturn(participant);

        eventService.publishEvent(1L);

        verify(emailService).sendEventInvitationEmail(memberUser, event);
        assertEquals(RsvpStatus.pending, participant.getRsvpStatus());
    }

    @Test
    void publishEventShouldRejectNonDraft() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> eventService.publishEvent(1L));
    }

    @Test
    void publishEventShouldRejectEmptyParticipants() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        event.setParticipants(new ArrayList<>());
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> eventService.publishEvent(1L));
    }

    @Test
    void verifyEntryCodeShouldCheckInParticipant() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        EventParticipant participant = buildParticipant(event, memberUser, RsvpStatus.accepted);
        participant.setEntryCode("NY-36061-1-10-1234");

        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(1L, 10L)).thenReturn(Optional.of(participant));
        when(eventParticipantRepository.save(participant)).thenReturn(participant);

        var dto = eventService.verifyEntryCode(1L, 10L, "ny-36061-1-10-1234");
        assertEquals(VerificationStatus.Done, dto.verificationStatus());
        assertEquals(RsvpStatus.checked_in, participant.getRsvpStatus());
    }

    @Test
    void verifyEntryCodeShouldRejectMissingEntryCode() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        EventParticipant participant = buildParticipant(event, memberUser, RsvpStatus.pending);
        participant.setEntryCode(null);

        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(1L, 10L)).thenReturn(Optional.of(participant));

        assertThrows(BadRequestException.class, () -> eventService.verifyEntryCode(1L, 10L, "CODE"));
    }

    @Test
    void verifyEntryCodeShouldRejectInvalidCode() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        EventParticipant participant = buildParticipant(event, memberUser, RsvpStatus.accepted);
        participant.setEntryCode("VALID");

        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(1L, 10L)).thenReturn(Optional.of(participant));

        assertThrows(BadRequestException.class, () -> eventService.verifyEntryCode(1L, 10L, "WRONG"));
    }

    @Test
    void verifyEntryCodeShouldThrowWhenParticipantNotFound() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.verifyEntryCode(1L, 10L, "CODE"));
    }

    @Test
    void updatePublishedShouldRemoveParticipantsNotInList() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        User removedUser = new User();
        removedUser.setId(99L);
        removedUser.setRole(Role.USER);
        EventParticipant toRemove = buildParticipant(event, removedUser, RsvpStatus.pending);

        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdWithUser(1L))
                .thenReturn(List.of(toRemove))
                .thenReturn(List.of());
        when(userRepository.findAllById(anySet())).thenReturn(List.of(memberUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(memberUser));
        when(eventDetailRepository.save(event)).thenReturn(event);

        eventService.updateEvent(1L, upsertRequest);
        verify(eventParticipantRepository).delete(toRemove);
    }

    @Test
    void createDraftShouldRejectMissingUsers() {
        when(userRepository.findAllById(anySet())).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> eventService.createDraft(upsertRequest));
    }

    @Test
    void getEventShouldCountCheckedInAsConfirmed() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        EventParticipant checkedIn = buildParticipant(event, memberUser, RsvpStatus.checked_in);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(List.of(checkedIn));

        assertEquals(1, eventService.getEvent(1L).confirmedCount());
    }

    @Test
    void publishEventShouldRejectNullParticipants() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        event.setParticipants(null);
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> eventService.publishEvent(1L));
    }

    @Test
    void verifyEntryCodeShouldRejectBlankEntryCode() {
        EventDetail event = buildEvent(1L, EventStatus.published);
        EventParticipant participant = buildParticipant(event, memberUser, RsvpStatus.accepted);
        participant.setEntryCode("   ");
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(1L, 10L)).thenReturn(Optional.of(participant));

        assertThrows(BadRequestException.class, () -> eventService.verifyEntryCode(1L, 10L, "CODE"));
    }

    @Test
    void createDraftShouldHandleNullEventPerks() {
        EventUpsertRequest request = new EventUpsertRequest(
                upsertRequest.title(), upsertRequest.description(), upsertRequest.venueName(),
                upsertRequest.location(), upsertRequest.eventDate(), upsertRequest.eventTime(),
                upsertRequest.stateCode(), upsertRequest.fipsCode(), null,
                upsertRequest.memberUserIds(), upsertRequest.notifyParticipants()
        );
        when(eventDetailRepository.countByEventCodePrefix("NY-36061")).thenReturn(0L);
        when(userRepository.findAllById(anySet())).thenReturn(List.of(memberUser));
        when(eventDetailRepository.save(any(EventDetail.class))).thenAnswer(inv -> {
            EventDetail saved = inv.getArgument(0);
            saved.setId(6L);
            return saved;
        });
        when(eventDetailRepository.findById(6L)).thenAnswer(inv -> Optional.of(buildEvent(6L, EventStatus.draft)));
        when(eventParticipantRepository.findByEventIdWithUser(6L)).thenReturn(List.of());

        assertNotNull(eventService.createDraft(request));
    }

    @Test
    void listEventsShouldReturnAllForBlankStatusFilter() {
        EventDetail draft = buildEvent(1L, EventStatus.draft);
        when(eventDetailRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draft));
        when(eventParticipantRepository.countByEventIdAndRsvpStatus(anyLong(), eq(RsvpStatus.accepted))).thenReturn(0L);
        when(eventParticipantRepository.countByEventId(anyLong())).thenReturn(1L);

        assertEquals(1, eventService.listEvents("").size());
        assertEquals(1, eventService.listEvents(null).size());
    }

    @Test
    void updateDraftShouldPreserveExistingEventCode() {
        EventDetail event = buildEvent(1L, EventStatus.draft);
        event.setEventCode("EXISTING-CODE");
        event.setParticipants(new ArrayList<>(List.of(buildParticipant(event, memberUser, RsvpStatus.pending))));
        when(eventDetailRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(memberUser));
        when(eventDetailRepository.save(event)).thenReturn(event);
        when(eventParticipantRepository.findByEventIdWithUser(1L)).thenReturn(event.getParticipants());

        eventService.updateEvent(1L, upsertRequest);
        assertEquals("EXISTING-CODE", event.getEventCode());
    }

    private static EventDetail buildEvent(Long id, EventStatus status) {
        EventDetail event = new EventDetail();
        event.setId(id);
        event.setEventCode("NY-36061-1");
        event.setTitle("Mixer");
        event.setVenueName("Venue");
        event.setLocation("Location");
        event.setEventDate(LocalDate.of(2026, 8, 1));
        event.setEventTime(LocalTime.of(18, 0));
        event.setStatus(status);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        event.setParticipants(new ArrayList<>());
        return event;
    }

    private static EventParticipant buildParticipant(EventDetail event, User user, RsvpStatus status) {
        EventParticipant participant = new EventParticipant();
        participant.setId(100L);
        participant.setEvent(event);
        participant.setUser(user);
        participant.setRsvpStatus(status);
        participant.setVerificationStatus(VerificationStatus.Pending);
        participant.setAssignedAt(LocalDateTime.now());
        return participant;
    }

}
