package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.ProfileCompletionService;
import com.SocialPairly_Workflow_Manager.service.ProfileService;
import com.SocialPairly_Workflow_Manager.service.UserMediaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final ProfileService profileService;
    private final CurrentUserService currentUserService;
    private final UserMediaService userMediaService;
    private final ProfileCompletionService profileCompletionService;

    public ProfileController(ProfileService profileService,
                             CurrentUserService currentUserService,
                             UserMediaService userMediaService,
                             ProfileCompletionService profileCompletionService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
        this.userMediaService = userMediaService;
        this.profileCompletionService = profileCompletionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        User user = currentUserService.getCurrentUser();
        log.info("Fetching profile view for userId={}", user.getId());
        UserProfile profile = profileService.getProfile(user);

        Map<String, Object> body = new HashMap<>();
        body.put("user", UserDto.from(user));
        body.put("profile", ProfileDto.from(profile));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/completion")
    public ResponseEntity<Map<String, Object>> getProfileCompletion() {
        User user = currentUserService.getCurrentUser();
        log.info("Fetching profile completion for userId={}", user.getId());
        UserProfile profile = profileService.getProfile(user);
        return ResponseEntity.ok(profileCompletionService.calculate(user, profile));
    }

    @PutMapping
    public ResponseEntity<ProfileDto> updateProfile(@Valid @RequestBody ProfileRequest request) {
        User user = currentUserService.getCurrentUser();
        log.info("Updating profile via API for userId={}", user.getId());
        return ResponseEntity.ok(ProfileDto.from(profileService.updateProfile(user, request)));
    }

    @PostMapping("/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        User user = currentUserService.getCurrentUser();
        log.info("Uploading profile photo (BLOB) for userId={}", user.getId());
        MediaUploadResponseDto saved = userMediaService.uploadProfilePhoto(user, file);
        profileService.setProfilePhoto(user, saved.getMediaUrl());
        return ResponseEntity.ok(Map.of("url", saved.getMediaUrl()));
    }
}