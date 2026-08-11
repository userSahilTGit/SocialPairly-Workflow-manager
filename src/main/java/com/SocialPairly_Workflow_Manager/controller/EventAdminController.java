package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.EventAdminStatsDto;
import com.SocialPairly_Workflow_Manager.dto.EventCandidateDto;
import com.SocialPairly_Workflow_Manager.dto.EventDetailDto;
import com.SocialPairly_Workflow_Manager.dto.EventSummaryDto;
import com.SocialPairly_Workflow_Manager.dto.EventUpsertRequest;
import com.SocialPairly_Workflow_Manager.dto.EventParticipantDto;
import com.SocialPairly_Workflow_Manager.dto.VerifyEntryCodeRequest;
import com.SocialPairly_Workflow_Manager.service.EventCandidateService;
import com.SocialPairly_Workflow_Manager.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/events")
public class EventAdminController {

    private static final Logger log = LoggerFactory.getLogger(EventAdminController.class);

    private final EventService eventService;
    private final EventCandidateService eventCandidateService;

    public EventAdminController(EventService eventService, EventCandidateService eventCandidateService) {
        this.eventService = eventService;
        this.eventCandidateService = eventCandidateService;
    }

    @GetMapping("/stats")
    public ResponseEntity<EventAdminStatsDto> getStats() {
        return ResponseEntity.ok(eventService.getAdminStats());
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryDto>> listEvents(
            @RequestParam(required = false, defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(eventService.listEvents(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PostMapping
    public ResponseEntity<EventDetailDto> createDraft(@Valid @RequestBody EventUpsertRequest request) {
        log.info("Admin creating draft event title={}", request.title());
        return ResponseEntity.ok(eventService.createDraft(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDetailDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventUpsertRequest request
    ) {
        log.info("Admin updating event id={} notify={}", id, request.notifyParticipants());
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDraft(@PathVariable Long id) {
        log.info("Admin deleting draft event id={}", id);
        eventService.deleteDraft(id);
        return ResponseEntity.ok(Map.of("message", "Draft event deleted"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<EventDetailDto> publishEvent(@PathVariable Long id) {
        log.info("Admin publishing event id={}", id);
        return ResponseEntity.ok(eventService.publishEvent(id));
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<EventCandidateDto>> searchCandidates(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String profession,
            @RequestParam(required = false) String educationLevel,
            @RequestParam(required = false) String maritalStatus
    ) {
        return ResponseEntity.ok(eventCandidateService.searchCandidates(
                query, verifiedOnly, gender, religion, minAge, maxAge,
                location, profession, educationLevel, maritalStatus
        ));
    }

    @PostMapping("/{eventId}/participants/{userId}/verify")
    public ResponseEntity<EventParticipantDto> verifyEntryCode(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @Valid @RequestBody VerifyEntryCodeRequest request
    ) {
        log.info("Admin verifying entry code eventId={} userId={}", eventId, userId);
        return ResponseEntity.ok(eventService.verifyEntryCode(eventId, userId, request.entryCode()));
    }

    @GetMapping("/perks")
    public ResponseEntity<List<String>> getPredefinedPerks() {
        return ResponseEntity.ok(List.of(
                "Dinner Included",
                "Drinks Provided",
                "Table Reservation",
                "15 Women - 15 Men Ratio",
                "VIP Lounge Access",
                "Cocktail Attire"
        ));
    }
}
