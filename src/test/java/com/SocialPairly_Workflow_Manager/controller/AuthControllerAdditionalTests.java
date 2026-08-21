package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.SocialPairly_Workflow_Manager.security.JwtTokenBlacklistService;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.SocialPairly_Workflow_Manager.service.AuthRateLimitService;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerAdditionalTests {

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthRateLimitService authRateLimitService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtTokenBlacklistService tokenBlacklistService;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @InjectMocks
    private AuthController authController;

    @Test
    void sendForgotPasswordOtpShouldReturnErrorResponseWhenExceptionThrown() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doThrow(new MessagingException("fail")).when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response =
                authController.sendForgotPasswordOtp(request, new MockHttpServletRequest());

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().get("error").contains("Failed to send OTP"));
    }

    @Test
    void sendForgotPasswordOtpShouldReturnSuccessResponse() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doNothing().when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response =
                authController.sendForgotPasswordOtp(request, new MockHttpServletRequest());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "OTP sent successfully to your registered email or phone number",
                response.getBody().get("message"));
    }

    @Test
    void verifyForgotPasswordOtpShouldReturnSuccessResponse() {
        ForgotPasswordVerifyOtpRequest request = new ForgotPasswordVerifyOtpRequest("test@example.com", "123456");
        when(authService.verifyForgotPasswordOtp(request)).thenReturn(Map.of("message", "OTP verified successfully"));

        ResponseEntity<Map<String, String>> response =
                authController.verifyForgotPasswordOtp(request, new MockHttpServletRequest());

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OTP verified successfully", response.getBody().get("message"));
    }

    @Test
    void resetPasswordShouldReturnSuccessResponse() {
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest("test@example.com", "123456", "newpass", "newpass");
        when(authService.resetPassword(request)).thenReturn(Map.of("message", "Password reset successfully"));

        ResponseEntity<Map<String, String>> response = authController.resetPassword(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Password reset successfully", response.getBody().get("message"));
    }
}
