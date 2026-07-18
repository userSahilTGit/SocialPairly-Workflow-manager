package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerAdditionalTests {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void sendForgotPasswordOtpShouldReturnErrorResponseWhenExceptionThrown() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doThrow(new MessagingException("fail")).when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response = authController.sendForgotPasswordOtp(request);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().get("error").contains("Failed to send OTP"));
    }

    @Test
    void sendForgotPasswordOtpShouldReturnSuccessResponse() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doNothing().when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response = authController.sendForgotPasswordOtp(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("OTP sent successfully to your Registered email or Phone number", response.getBody().get("message"));
    }

    @Test
    void verifyForgotPasswordOtpShouldReturnSuccessResponse() {
        ForgotPasswordVerifyOtpRequest request = new ForgotPasswordVerifyOtpRequest("test@example.com", "123456");
        when(authService.verifyForgotPasswordOtp(request)).thenReturn(Map.of("message", "OTP verified successfully"));

        ResponseEntity<Map<String, String>> response = authController.verifyForgotPasswordOtp(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("OTP verified successfully", response.getBody().get("message"));
    }

    @Test
    void resetPasswordShouldReturnSuccessResponse() {
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest("test@example.com", "123456", "newpass", "newpass");
        when(authService.resetPassword(request)).thenReturn(Map.of("message", "Password reset successfully"));

        ResponseEntity<Map<String, String>> response = authController.resetPassword(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Password reset successfully", response.getBody().get("message"));
    }
}
