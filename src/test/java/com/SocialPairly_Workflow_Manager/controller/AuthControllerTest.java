package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.TokenDto;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, googleIdTokenVerifier);
    }

    @Test
    void registerShouldDelegateToAuthService() {
        RegisterRequest request = new RegisterRequest("Ada", "Lovelace", "ada@example.com", "1234567", "secret", "London");
        UserDto userDto = new UserDto(1L, "Ada", "Lovelace", "ada@example.com", "1234567", "London", null, false, "");
        AuthResponse authResponse = new AuthResponse("token", userDto);

        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());
    }

    @Test
    void loginShouldDelegateToAuthService() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret");
        UserDto userDto = new UserDto(1L, "Ada", "Lovelace", "ada@example.com", "1234567", "London", null, false, "");
        AuthResponse authResponse = new AuthResponse("token", userDto);

        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());
    }

    @Test
    void sendForgotPasswordOtpShouldReturnSuccessResponse() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doNothing().when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response = authController.sendForgotPasswordOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP sent successfully to your Registered email or Phone number", response.getBody().get("message"));
    }

    @Test
    void sendForgotPasswordOtpShouldReturnErrorResponseWhenExceptionThrown() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doThrow(new MessagingException("mail server down")).when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response = authController.sendForgotPasswordOtp(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("Failed to send OTP"));
        assertTrue(response.getBody().get("error").contains("mail server down"));
    }

    @Test
    void verifyForgotPasswordOtpShouldReturnSuccessResponse() {
        ForgotPasswordVerifyOtpRequest request = new ForgotPasswordVerifyOtpRequest("test@example.com", "123456");
        when(authService.verifyForgotPasswordOtp(request)).thenReturn(Map.of("message", "OTP verified successfully"));

        ResponseEntity<Map<String, String>> response = authController.verifyForgotPasswordOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP verified successfully", response.getBody().get("message"));
    }

    @Test
    void resetPasswordShouldReturnSuccessResponse() {
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest(
                "test@example.com", "123456", "newpass", "newpass");
        when(authService.resetPassword(request)).thenReturn(Map.of("message", "Password reset successfully"));

        ResponseEntity<Map<String, String>> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset successfully", response.getBody().get("message"));
    }

    @Test
    void verifyGoogleTokenShouldReturnAuthResponseWhenTokenValid() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("valid-google-token");

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        UserDto userDto = new UserDto(2L, "John", "Doe", "john@gmail.com", null, null, null, false, "");
        AuthResponse authResponse = new AuthResponse("google-token", userDto);

        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("john@gmail.com");
        when(payload.get("given_name")).thenReturn("John");
        when(payload.get("family_name")).thenReturn("Doe");
        when(authService.loginOrRegisterGoogleUser("john@gmail.com", "John", "Doe")).thenReturn(authResponse);

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());
    }

    @Test
    void verifyGoogleTokenShouldReturnUnauthorizedWhenTokenInvalid() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("invalid-token");

        when(googleIdTokenVerifier.verify("invalid-token")).thenReturn(null);

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid Google Identity Token.", response.getBody());
        verify(authService, never()).loginOrRegisterGoogleUser(any(), any(), any());
    }

    @Test
    void verifyGoogleTokenShouldReturnServerErrorWhenVerificationThrows() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("error-token");

        when(googleIdTokenVerifier.verify("error-token")).thenThrow(new IOException("network error"));

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Authentication processing exception"));
        assertTrue(response.getBody().toString().contains("network error"));
    }
}
