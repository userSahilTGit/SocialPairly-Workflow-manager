package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.EventStatus;
import com.SocialPairly_Workflow_Manager.entity.ReactionType;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.EventCandidateService;
import com.SocialPairly_Workflow_Manager.service.EventService;
import com.SocialPairly_Workflow_Manager.service.UserEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventCandidateService eventCandidateService;

    @InjectMocks
    private EventAdminController eventAdminController;

    private EventUpsertRequest upsertRequest;
    private EventDetailDto detailDto;
    private EventSummaryDto summaryDto;

    @BeforeEach
    void setUp() {
        upsertRequest = new EventUpsertRequest(
                "Title", "Desc", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0),
                "NY", "36061", List.of("Dinner"), List.of(1L), false
        );
        detailDto = new EventDetailDto(
                1L, "NY-36061-1", "Title", "Desc", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0), EventStatus.draft,
                List.of("Dinner"), 1, 0, List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        summaryDto = new EventSummaryDto(
                1L, "NY-36061-1", "Title", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0), EventStatus.draft,
                1, 0, List.of("Dinner"), LocalDateTime.now()
        );
    }

    @Test
    void getStatsShouldDelegateToService() {
        EventAdminStatsDto stats = new EventAdminStatsDto(10, 5, 2, 8);
        when(eventService.getAdminStats()).thenReturn(stats);

        ResponseEntity<EventAdminStatsDto> response = eventAdminController.getStats();
        assertEquals(200, response.getStatusCode().value());
        assertSame(stats, response.getBody());
    }

    @Test
    void listEventsShouldDelegateToService() {
        when(eventService.listEvents("draft")).thenReturn(List.of(summaryDto));

        ResponseEntity<List<EventSummaryDto>> response = eventAdminController.listEvents("draft");
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getEventShouldDelegateToService() {
        when(eventService.getEvent(1L)).thenReturn(detailDto);

        ResponseEntity<EventDetailDto> response = eventAdminController.getEvent(1L);
        assertSame(detailDto, response.getBody());
    }

    @Test
    void createDraftShouldDelegateToService() {
        when(eventService.createDraft(upsertRequest)).thenReturn(detailDto);

        ResponseEntity<EventDetailDto> response = eventAdminController.createDraft(upsertRequest);
        assertSame(detailDto, response.getBody());
    }

    @Test
    void updateEventShouldDelegateToService() {
        when(eventService.updateEvent(1L, upsertRequest)).thenReturn(detailDto);

        ResponseEntity<EventDetailDto> response = eventAdminController.updateEvent(1L, upsertRequest);
        assertSame(detailDto, response.getBody());
    }

    @Test
    void deleteDraftShouldReturnMessage() {
        ResponseEntity<Map<String, String>> response = eventAdminController.deleteDraft(1L);
        assertEquals("Draft event deleted", response.getBody().get("message"));
        verify(eventService).deleteDraft(1L);
    }

    @Test
    void publishEventShouldDelegateToService() {
        when(eventService.publishEvent(1L)).thenReturn(detailDto);

        ResponseEntity<EventDetailDto> response = eventAdminController.publishEvent(1L);
        assertSame(detailDto, response.getBody());
    }

    @Test
    void searchCandidatesShouldDelegateToService() {
        EventCandidateDto candidate = new EventCandidateDto(
                1L, "Alice", "alice@example.com", "F", 28, null,
                "Engineer", "Austin", "TX", "Single", "BS", null, true
        );
        when(eventCandidateService.searchCandidates(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of(candidate));

        ResponseEntity<List<EventCandidateDto>> response = eventAdminController.searchCandidates(
                "alice", true, "F", null, 25, 35, "Austin", "Engineer", "BS", "Single");

        assertEquals(1, response.getBody().size());
    }

    @Test
    void verifyEntryCodeShouldDelegateToService() {
        EventParticipantDto participantDto = new EventParticipantDto(
                1L, 10L, "User", "u@example.com", null, null, null,
                "CODE-10", RsvpStatus.checked_in, "ENTRY", null, LocalDateTime.now()
        );
        when(eventService.verifyEntryCode(1L, 10L, "ABC")).thenReturn(participantDto);

        ResponseEntity<EventParticipantDto> response = eventAdminController.verifyEntryCode(
                1L, 10L, new VerifyEntryCodeRequest("ABC"));

        assertSame(participantDto, response.getBody());
    }

    @Test
    void getPredefinedPerksShouldReturnList() {
        ResponseEntity<List<String>> response = eventAdminController.getPredefinedPerks();
        assertFalse(response.getBody().isEmpty());
        assertTrue(response.getBody().contains("Dinner Included"));
    }
}
