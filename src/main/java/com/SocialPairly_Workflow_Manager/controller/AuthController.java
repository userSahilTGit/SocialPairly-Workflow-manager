package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.TokenDto;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    // Replace with your actual Google Client ID from the Google Developer Console
    private static final String GOOGLE_CLIENT_ID = "1043168194153-i82qfvqg1jsk804qaa7ipkkov67b8pt4.apps.googleusercontent.com";

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(AuthService authService) {
        this(authService, createDefaultVerifier());
    }

    // Constructor for testability — allows injecting a mock GoogleIdTokenVerifier (not used by Spring)
    AuthController(AuthService authService, GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.authService = authService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    private static GoogleIdTokenVerifier createDefaultVerifier() {
        try {
            return new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    new GsonFactory()
            )
            .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
            .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GoogleIdTokenVerifier", e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email={}", request.email());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for identifier={}", request.identifier());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<Map<String, String>> sendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordSendOtpRequest request) {
        log.info("Forgot-password OTP request received for identifier={}", request.identifier());
        try {
            authService.sendForgotPasswordOtp(request);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully to your Registered email or Phone number"));
        } catch (Exception e) {
            log.error("Failed to send forgot password OTP for identifier={}", request.identifier(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<Map<String, String>> verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordVerifyOtpRequest request) {
        log.info("OTP verification request received for identifier={}", request.identifier());
        return ResponseEntity.ok(authService.verifyForgotPasswordOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequest request) {
        log.info("Reset password request received for identifier={}", request.identifier());
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/google")
    public ResponseEntity<?> verifyGoogleToken(@RequestBody TokenDto tokenDto) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(tokenDto.getIdToken());
            
            if (idToken != null) {
                Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");

                // Execute transactional profile resolution pipeline
                AuthResponse authResponse = authService.loginOrRegisterGoogleUser(email, firstName, lastName);

                // Returns your standard schema directly to React!
                return ResponseEntity.ok(authResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google Identity Token.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Authentication processing exception: " + e.getMessage());
        }
    }
}