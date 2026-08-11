package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.ReactionType;
import com.SocialPairly_Workflow_Manager.entity.RsvpStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.UserEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private UserEventService userEventService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EventController eventController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void listMyEventsShouldReturnTiles() {
        UserEventTileDto tile = new UserEventTileDto(
                10L, "CODE", "Title", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0),
                List.of("Dinner"), RsvpStatus.pending, 5, "CODE-1", null
        );
        when(userEventService.listMyEvents(user)).thenReturn(List.of(tile));

        ResponseEntity<List<UserEventTileDto>> response = eventController.listMyEvents();
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getInvitationShouldReturnInvitation() {
        UserEventInvitationDto invitation = new UserEventInvitationDto(
                10L, "CODE", "Title", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0),
                List.of("Dinner"), 3, List.of()
        );
        when(userEventService.getInvitation(10L, user)).thenReturn(invitation);

        ResponseEntity<UserEventInvitationDto> response = eventController.getInvitation(10L);
        assertSame(invitation, response.getBody());
    }

    @Test
    void respondToInvitationShouldDelegateToService() {
        UserEventTileDto tile = new UserEventTileDto(
                10L, "CODE", "Title", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0),
                List.of(), RsvpStatus.accepted, 2, "CODE-1", "ENTRY"
        );
        when(userEventService.respondToInvitation(10L, user, true)).thenReturn(tile);

        ResponseEntity<UserEventTileDto> response = eventController.respondToInvitation(
                10L, new EventRsvpRequest(true));

        assertSame(tile, response.getBody());
    }

    @Test
    void getWorkspaceShouldReturnWorkspace() {
        UserEventWorkspaceDto workspace = new UserEventWorkspaceDto(
                10L, "CODE", "Title", "Venue", "Location",
                LocalDate.of(2026, 6, 1), LocalTime.of(18, 0),
                List.of(), "CODE-1", "ENTRY", 50, 4, 0
        );
        when(userEventService.getWorkspace(10L, user)).thenReturn(workspace);

        ResponseEntity<UserEventWorkspaceDto> response = eventController.getWorkspace(10L);
        assertEquals(50, response.getBody().tokenBalance());
    }

    @Test
    void listProfilesShouldReturnProfiles() {
        EventParticipantProfileDto profile = new EventParticipantProfileDto(
                2L, "CODE-2", "Other", 30, "City", "City, ST", true,
                "M", null, "Dev", null, null, null, null,
                Set.of(), null, List.of(), List.of()
        );
        when(userEventService.listParticipantProfiles(10L, user)).thenReturn(List.of(profile));

        ResponseEntity<List<EventParticipantProfileDto>> response = eventController.listProfiles(10L);
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getProfileShouldReturnSingleProfile() {
        EventParticipantProfileDto profile = new EventParticipantProfileDto(
                2L, "CODE-2", "Other", 30, "City", "City, ST", true,
                "M", null, "Dev", null, null, null, null,
                Set.of(), null, List.of(), List.of()
        );
        when(userEventService.getParticipantProfile(10L, user, 2L)).thenReturn(profile);

        ResponseEntity<EventParticipantProfileDto> response = eventController.getProfile(10L, 2L);
        assertEquals(2L, response.getBody().userId());
    }

    @Test
    void sendReactionShouldDelegateToService() {
        EventReactionResponseDto reactionResponse = new EventReactionResponseDto(
                ReactionType.Heart, 3, 47
        );
        when(userEventService.sendReaction(10L, user, 2L, ReactionType.Heart)).thenReturn(reactionResponse);

        ResponseEntity<EventReactionResponseDto> response = eventController.sendReaction(
                10L, new EventReactionRequest(2L, ReactionType.Heart));

        assertEquals(47, response.getBody().tokenBalance());
    }
}
