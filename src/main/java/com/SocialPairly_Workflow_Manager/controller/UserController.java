package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.dto.UpdatePhoneRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import com.SocialPairly_Workflow_Manager.service.UserService;
import com.SocialPairly_Workflow_Manager.service.UserTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final SubscriptionService subscriptionService;
    private final UserTokenService userTokenService;

    public UserController(
            CurrentUserService currentUserService,
            UserService userService,
            UserProfileRepository userProfileRepository,
            SubscriptionService subscriptionService,
            UserTokenService userTokenService
    ) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
        this.subscriptionService = subscriptionService;
        this.userTokenService = userTokenService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        User user = currentUserService.getCurrentUser();
        subscriptionService.findPrimaryActiveSubscription(user.getId())
                .ifPresent(sub -> userTokenService.ensureSubscriptionTokensCredited(user, sub));
        log.debug("Fetching current user profile for userId={}", user.getId());
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return ResponseEntity.ok(UserDto.from(user, profile));
    }

    @PutMapping("/me/phone")
    public ResponseEntity<Map<String, Object>> updatePhone(@Valid @RequestBody UpdatePhoneRequest request) {
        User user = currentUserService.getCurrentUser();
        log.info("Update phone request for userId={}", user.getId());
        return ResponseEntity.ok(userService.updatePhone(user, request.phoneNumber()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        User user = currentUserService.getCurrentUser();
        log.info("Delete account request for userId={}", user.getId());
        userService.deleteAccount(user, request);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
