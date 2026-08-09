package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleRequest;
import com.SocialPairly_Workflow_Manager.dto.PersonalityLifestyleResponse;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.PersonalityOnboardingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding/personality")
public class PersonalityOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(PersonalityOnboardingController.class);

    private final CurrentUserService currentUserService;
    private final PersonalityOnboardingService personalityOnboardingService;

    public PersonalityOnboardingController(
            CurrentUserService currentUserService,
            PersonalityOnboardingService personalityOnboardingService
    ) {
        this.currentUserService = currentUserService;
        this.personalityOnboardingService = personalityOnboardingService;
    }

    @GetMapping
    public ResponseEntity<PersonalityLifestyleResponse> getPersonality() {
        User user = currentUserService.getCurrentUser();
        log.info("Loading personality onboarding for userId={}", user.getId());
        return ResponseEntity.ok(personalityOnboardingService.getPersonality(user));
    }

    @PutMapping
    public ResponseEntity<PersonalityLifestyleResponse> savePersonality(
            @Valid @RequestBody PersonalityLifestyleRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        log.info("Saving personality onboarding for userId={} action={}", user.getId(), request.action());
        return ResponseEntity.ok(personalityOnboardingService.savePersonality(user, request));
    }
}
