package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<Map<String, String>> sendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordSendOtpRequest request) {
        return ResponseEntity.ok(authService.sendForgotPasswordOtp(request));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<Map<String, String>> verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordVerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyForgotPasswordOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}