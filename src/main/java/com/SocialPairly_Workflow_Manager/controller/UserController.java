package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.ChangePasswordRequest;
import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.dto.UpdatePhoneRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.service.AuthRateLimitService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.SubscriptionService;
import com.SocialPairly_Workflow_Manager.service.UserService;
import com.SocialPairly_Workflow_Manager.service.UserTokenService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthRateLimitService authRateLimitService;

    public UserController(
            CurrentUserService currentUserService,
            UserService userService,
            UserProfileRepository userProfileRepository,
            SubscriptionService subscriptionService,
            UserTokenService userTokenService,
            AuthRateLimitService authRateLimitService
    ) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
        this.subscriptionService = subscriptionService;
        this.userTokenService = userTokenService;
        this.authRateLimitService = authRateLimitService;
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

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserService.getCurrentUser();
        log.info("Change password request for userId={}", user.getId());
        return ResponseEntity.ok(userService.changePassword(user, request));
    }

    @PostMapping("/me/delete/send-otp")
    public ResponseEntity<Map<String, String>> sendDeleteAccountOtp(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser();
        log.info("Send account-delete OTP for userId={}", user.getId());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_DELETE_SEND_OTP,
                clientIp(httpRequest),
                String.valueOf(user.getId()));
        return ResponseEntity.ok(userService.sendAccountDeleteOtp(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser();
        log.info("Delete account request for userId={}", user.getId());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_DELETE_VERIFY,
                clientIp(httpRequest),
                String.valueOf(user.getId()));
        userService.deleteAccount(user, request);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
