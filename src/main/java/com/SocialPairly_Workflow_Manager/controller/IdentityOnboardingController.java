package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundResponse;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.IdentityOnboardingService;
import com.SocialPairly_Workflow_Manager.util.IdentityPerf;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding/identity")
public class IdentityOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(IdentityOnboardingController.class);

    private final CurrentUserService currentUserService;
    private final IdentityOnboardingService identityOnboardingService;

    public IdentityOnboardingController(
            CurrentUserService currentUserService,
            IdentityOnboardingService identityOnboardingService
    ) {
        this.currentUserService = currentUserService;
        this.identityOnboardingService = identityOnboardingService;
    }

    @GetMapping
    public ResponseEntity<IdentityBackgroundResponse> getIdentity() {
        User user = currentUserService.getCurrentUser();
        log.info("Loading identity onboarding for userId={}", user.getId());
        return IdentityPerf.timed("getIdentity", user.getId(),
                () -> ResponseEntity.ok(identityOnboardingService.getIdentity(user)));
    }

    @PutMapping
    public ResponseEntity<IdentityBackgroundResponse> saveIdentity(
            @Valid @RequestBody IdentityBackgroundRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        log.info("Saving identity onboarding for userId={} action={}", user.getId(), request.action());
        String op = request.action() == null ? "saveIdentity" : "saveIdentity:" + request.action().trim();
        return IdentityPerf.timed(op, user.getId(),
                () -> ResponseEntity.ok(identityOnboardingService.saveIdentity(user, request)));
    }

    @GetMapping("/reference-data")
    public ResponseEntity<Map<String, Object>> getReferenceData() {
        User user = currentUserService.getCurrentUser();
        return IdentityPerf.timed("getReferenceData", user.getId(),
                () -> ResponseEntity.ok(identityOnboardingService.getReferenceData()));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docPurpose") String docPurpose
    ) {
        User user = currentUserService.getCurrentUser();
        log.info("Uploading identity document for userId={} purpose={}", user.getId(), docPurpose);
        return ResponseEntity.ok(identityOnboardingService.uploadDocument(user, file, docPurpose));
    }

    @GetMapping("/documents/{id}/stream")
    public ResponseEntity<byte[]> streamDocument(@PathVariable("id") Long id) {
        User user = currentUserService.getCurrentUser();
        log.info("Streaming identity document id={} for userId={}", id, user.getId());
        IdentityOnboardingService.DocumentStreamResult result =
                identityOnboardingService.streamDocument(user, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header("Cache-Control", "no-store")
                .body(result.bytes());
    }

    @PostMapping("/verification/start")
    public ResponseEntity<Map<String, Object>> startVerification() {
        User user = currentUserService.getCurrentUser();
        log.info("Starting identity verification for userId={}", user.getId());
        return ResponseEntity.ok(identityOnboardingService.startVerification(user));
    }

    @PostMapping("/verification/selfie")
    public ResponseEntity<Map<String, Object>> submitSelfie(@RequestBody(required = false) Map<String, Object> body) {
        User user = currentUserService.getCurrentUser();
        Long documentId = null;
        boolean faceMatchConfirmed = false;
        if (body != null && body.get("documentId") != null) {
            Object raw = body.get("documentId");
            if (raw instanceof Number number) {
                documentId = number.longValue();
            } else {
                documentId = Long.parseLong(raw.toString());
            }
        }
        if (body != null && body.get("faceMatchConfirmed") != null) {
            Object raw = body.get("faceMatchConfirmed");
            if (raw instanceof Boolean b) {
                faceMatchConfirmed = b;
            } else {
                faceMatchConfirmed = Boolean.parseBoolean(raw.toString());
            }
        }
        return ResponseEntity.ok(identityOnboardingService.submitSelfie(user, documentId, faceMatchConfirmed));
    }

    @PostMapping("/verification/webhook")
    public ResponseEntity<Map<String, Object>> verificationWebhook(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(identityOnboardingService.handleVerificationWebhook(body));
    }

    /**
     * Phase 1: background screening is not offered. Keep the route so older clients
     * fail fast instead of hanging on a consent dialog / save gate.
     * Phase 2 will re-enable acceptBackgroundConsent.
     */
    @PostMapping("/background-consent")
    public ResponseEntity<Map<String, Object>> backgroundConsent(@RequestBody Map<String, Object> body) {
        throw new BadRequestException(
                "Background screening consent is not available in Phase 1");
    }
}
