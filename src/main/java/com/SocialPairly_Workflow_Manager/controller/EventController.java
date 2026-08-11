package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.EventParticipantProfileDto;
import com.SocialPairly_Workflow_Manager.dto.EventReactionRequest;
import com.SocialPairly_Workflow_Manager.dto.EventReactionResponseDto;
import com.SocialPairly_Workflow_Manager.dto.EventRsvpRequest;
import com.SocialPairly_Workflow_Manager.dto.UserEventInvitationDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventTileDto;
import com.SocialPairly_Workflow_Manager.dto.UserEventWorkspaceDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.UserEventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final UserEventService userEventService;
    private final CurrentUserService currentUserService;

    public EventController(UserEventService userEventService, CurrentUserService currentUserService) {
        this.userEventService = userEventService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<UserEventTileDto>> listMyEvents() {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userEventService.listMyEvents(user));
    }

    @GetMapping("/{eventId}/invitation")
    public ResponseEntity<UserEventInvitationDto> getInvitation(@PathVariable Long eventId) {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userEventService.getInvitation(eventId, user));
    }

    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<UserEventTileDto> respondToInvitation(
            @PathVariable Long eventId,
            @Valid @RequestBody EventRsvpRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        log.info("User {} RSVP to event {} accept={}", user.getId(), eventId, request.accept());
        return ResponseEntity.ok(userEventService.respondToInvitation(eventId, user, request.accept()));
    }

    @GetMapping("/{eventId}/workspace")
    public ResponseEntity<UserEventWorkspaceDto> getWorkspace(@PathVariable Long eventId) {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userEventService.getWorkspace(eventId, user));
    }

    @GetMapping("/{eventId}/profiles")
    public ResponseEntity<List<EventParticipantProfileDto>> listProfiles(@PathVariable Long eventId) {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userEventService.listParticipantProfiles(eventId, user));
    }

    @GetMapping("/{eventId}/profiles/{targetUserId}")
    public ResponseEntity<EventParticipantProfileDto> getProfile(
            @PathVariable Long eventId,
            @PathVariable Long targetUserId
    ) {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userEventService.getParticipantProfile(eventId, user, targetUserId));
    }

    @PostMapping("/{eventId}/reactions")
    public ResponseEntity<EventReactionResponseDto> sendReaction(
            @PathVariable Long eventId,
            @Valid @RequestBody EventReactionRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        log.info("User {} reacting in event {} to user {}", user.getId(), eventId, request.toUserId());
        return ResponseEntity.ok(userEventService.sendReaction(
                eventId, user, request.toUserId(), request.reactionType()));
    }
}
