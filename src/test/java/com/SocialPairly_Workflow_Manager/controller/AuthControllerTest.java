package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.TokenDto;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.SocialPairly_Workflow_Manager.security.JwtTokenBlacklistService;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.SocialPairly_Workflow_Manager.service.AuthRateLimitService;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

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

    private AuthController authController;

    private MockHttpServletRequest httpRequest;
    private MockHttpServletResponse httpResponse;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                authService, currentUserService, authRateLimitService,
                authCookieService, jwtUtil, tokenBlacklistService, googleIdTokenVerifier);
        httpRequest = new MockHttpServletRequest();
        httpResponse = new MockHttpServletResponse();
    }

    private static UserDto sampleUserDto(long id, String firstName, String lastName, String email) {
        return new UserDto(
                id, firstName, lastName, null, firstName, email, "1234567", "London",
                null, false, false, false, false, false, false, false, false, false, null, false, "", 0);
    }

    @Test
    void registerShouldDelegateToAuthService() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "1234567", "secret", "secret", "London",
                true, true, true, true, false);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        AuthResponse authResponse = new AuthResponse("token", userDto);

        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.register(request, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());
        verify(authCookieService).writeAuthCookie(httpResponse, "token", false);
    }

    @Test
    void loginShouldDelegateToAuthService() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret", null);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        AuthResponse authResponse = new AuthResponse("token", userDto);

        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());
        verify(authRateLimitService).check(
                eq(AuthRateLimitService.ACTION_LOGIN), any(), eq("ada@example.com"));
        verify(authCookieService).writeAuthCookie(httpResponse, "token", false);
    }

    @Test
    void loginShouldWriteRememberMeCookieWhenRequested() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret", true);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        AuthResponse authResponse = new AuthResponse("remember-token", userDto);

        when(authService.login(request)).thenReturn(authResponse);

        authController.login(request, httpRequest, httpResponse);

        verify(authCookieService).writeAuthCookie(httpResponse, "remember-token", true);
    }

    @Test
    void logoutShouldRevokeTokenAndClearCookie() {
        when(authCookieService.resolveToken(httpRequest)).thenReturn("jwt-token");
        when(jwtUtil.isTokenValid("jwt-token")).thenReturn(true);
        Date expiry = new Date(System.currentTimeMillis() + 60_000);
        when(jwtUtil.extractExpiration("jwt-token")).thenReturn(expiry);

        ResponseEntity<Map<String, String>> response = authController.logout(httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out", response.getBody().get("message"));
        verify(tokenBlacklistService).revoke("jwt-token", expiry);
        verify(authCookieService).clearAuthCookie(httpResponse);
    }

    @Test
    void logoutShouldClearCookieEvenWithoutToken() {
        when(authCookieService.resolveToken(httpRequest)).thenReturn(null);

        ResponseEntity<Map<String, String>> response = authController.logout(httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tokenBlacklistService, never()).revoke(any(), any());
        verify(authCookieService).clearAuthCookie(httpResponse);
    }

    @Test
    void sendForgotPasswordOtpShouldReturnSuccessResponse() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doNothing().when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response =
                authController.sendForgotPasswordOtp(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "OTP sent successfully to your registered email or phone number",
                response.getBody().get("message"));
    }

    @Test
    void sendForgotPasswordOtpShouldReturnErrorResponseWhenExceptionThrown() throws Exception {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("test@example.com");
        doThrow(new MessagingException("mail server down")).when(authService).sendForgotPasswordOtp(request);

        ResponseEntity<Map<String, String>> response =
                authController.sendForgotPasswordOtp(request, httpRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to send OTP. Please try again later.", response.getBody().get("error"));
    }

    @Test
    void verifyForgotPasswordOtpShouldReturnSuccessResponse() {
        ForgotPasswordVerifyOtpRequest request =
                new ForgotPasswordVerifyOtpRequest("test@example.com", "123456");
        when(authService.verifyForgotPasswordOtp(request))
                .thenReturn(Map.of("message", "OTP verified successfully"));

        ResponseEntity<Map<String, String>> response =
                authController.verifyForgotPasswordOtp(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP verified successfully", response.getBody().get("message"));
    }

    @Test
    void resetPasswordShouldReturnSuccessResponse() {
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest(
                "test@example.com", "123456", "newpass", "newpass");
        when(authService.resetPassword(request))
                .thenReturn(Map.of("message", "Password reset successfully"));

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
        UserDto userDto = sampleUserDto(2L, "John", "Doe", "john@gmail.com");
        AuthResponse authResponse = new AuthResponse("google-token", userDto);

        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("john@gmail.com");
        when(payload.get("given_name")).thenReturn("John");
        when(payload.get("family_name")).thenReturn("Doe");
        when(payload.getEmailVerified()).thenReturn(true);
        when(authService.loginOrRegisterGoogleUser(
                "john@gmail.com", "John", "Doe", false, true)).thenReturn(authResponse);

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());
        verify(authCookieService).writeAuthCookie(httpResponse, "google-token", false);
    }

    @Test
    void verifyGoogleTokenShouldReturnUnauthorizedWhenTokenInvalid() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("invalid-token");

        when(googleIdTokenVerifier.verify("invalid-token")).thenReturn(null);

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Invalid Google Identity Token.", body.get("error"));
        verify(authService, never()).loginOrRegisterGoogleUser(any(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void verifyGoogleTokenShouldReturnServerErrorWhenVerificationThrows() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("error-token");

        when(googleIdTokenVerifier.verify("error-token")).thenThrow(new IOException("network error"));

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Google Sign-In failed. Please try again.", body.get("error"));
    }

    @Test
    void verifyGoogleTokenShouldReturnBadRequestWhenTokenMissing() {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("  ");

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Missing Google identity token", body.get("error"));
    }

    @Test
    void verifyGoogleTokenShouldReturnUnauthorizedOnGeneralSecurityException() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("bad-token");

        when(googleIdTokenVerifier.verify("bad-token"))
                .thenThrow(new java.security.GeneralSecurityException("bad sig"));

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Invalid Google Identity Token.", body.get("error"));
    }

    @Test
    void verifyGoogleTokenShouldReturnBadRequestOnBadRequestException() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("valid-token");

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);

        when(googleIdTokenVerifier.verify("valid-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("");
        when(payload.get("given_name")).thenReturn(null);
        when(payload.get("family_name")).thenReturn(null);
        when(payload.getEmailVerified()).thenReturn(false);
        when(authService.loginOrRegisterGoogleUser("", null, null, false, false))
                .thenThrow(new com.SocialPairly_Workflow_Manager.exception.BadRequestException("Email is required"));

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Email is required", body.get("error"));
    }

    @Test
    void verifyGoogleTokenShouldPassRememberMeFlag() throws Exception {
        TokenDto tokenDto = new TokenDto();
        tokenDto.setIdToken("remember-token");
        tokenDto.setRememberMe(true);

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        UserDto userDto = sampleUserDto(3L, "Jane", "Doe", "jane@gmail.com");
        AuthResponse authResponse = new AuthResponse("tok", userDto);

        when(googleIdTokenVerifier.verify("remember-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("jane@gmail.com");
        when(payload.get("given_name")).thenReturn("Jane");
        when(payload.get("family_name")).thenReturn("Doe");
        when(payload.getEmailVerified()).thenReturn(true);
        when(authService.loginOrRegisterGoogleUser(
                "jane@gmail.com", "Jane", "Doe", true, true)).thenReturn(authResponse);

        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).loginOrRegisterGoogleUser("jane@gmail.com", "Jane", "Doe", true, true);
        verify(authCookieService).writeAuthCookie(httpResponse, "tok", true);
    }

    @Test
    void verifyAppleTokenShouldReturnNotImplemented() {
        com.SocialPairly_Workflow_Manager.dto.AppleAuthRequest appleRequest =
                new com.SocialPairly_Workflow_Manager.dto.AppleAuthRequest();
        appleRequest.setIdToken("id-token");

        ResponseEntity<?> response = authController.verifyAppleToken(appleRequest);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("Apple Sign-In"));
    }

    @Test
    void loginShouldUseXForwardedForWhenPresent() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret", null);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        when(authService.login(request)).thenReturn(new AuthResponse("token", userDto));
        httpRequest.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        authController.login(request, httpRequest, httpResponse);

        verify(authRateLimitService).check(
                eq(AuthRateLimitService.ACTION_LOGIN), eq("203.0.113.10"), eq("ada@example.com"));
    }

    @Test
    void loginShouldTreatBlankForwardedForAsMissing() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret", null);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        when(authService.login(request)).thenReturn(new AuthResponse("token", userDto));
        httpRequest.addHeader("X-Forwarded-For", "   ");
        httpRequest.setRemoteAddr("198.51.100.7");

        authController.login(request, httpRequest, httpResponse);

        verify(authRateLimitService).check(
                eq(AuthRateLimitService.ACTION_LOGIN), eq("198.51.100.7"), eq("ada@example.com"));
    }

    @Test
    void loginShouldUseUnknownWhenRequestNull() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret", null);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        when(authService.login(request)).thenReturn(new AuthResponse("token", userDto));

        authController.login(request, null, httpResponse);

        verify(authRateLimitService).check(
                eq(AuthRateLimitService.ACTION_LOGIN), eq("unknown"), eq("ada@example.com"));
    }

    @Test
    void loginShouldUseUnknownWhenRemoteAddrNull() {
        LoginRequest request = new LoginRequest("ada@example.com", "secret", null);
        UserDto userDto = sampleUserDto(1L, "Ada", "Lovelace", "ada@example.com");
        when(authService.login(request)).thenReturn(new AuthResponse("token", userDto));
        httpRequest.setRemoteAddr(null);

        authController.login(request, httpRequest, httpResponse);

        verify(authRateLimitService).check(
                eq(AuthRateLimitService.ACTION_LOGIN), eq("unknown"), eq("ada@example.com"));
    }

    @Test
    void verifyGoogleTokenShouldReturnBadRequestWhenTokenDtoNull() {
        ResponseEntity<?> response = authController.verifyGoogleToken(null, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void verifyGoogleTokenShouldReturnBadRequestWhenIdTokenNull() {
        TokenDto tokenDto = new TokenDto();
        ResponseEntity<?> response = authController.verifyGoogleToken(tokenDto, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void verifyEmailShouldDelegateToAuthService() {
        com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest request =
                new com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest("ada@example.com", "123456");
        when(authService.verifyEmail(request)).thenReturn(Map.of("message", "Email verified"));

        ResponseEntity<Map<String, String>> response = authController.verifyEmail(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Email verified", response.getBody().get("message"));
    }

    @Test
    void verifyPhoneShouldDelegateToAuthService() {
        com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest request =
                new com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest("+919876543210", "123456");
        when(authService.verifyPhone(request)).thenReturn(Map.of("message", "ok"));

        ResponseEntity<Map<String, String>> response = authController.verifyPhone(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void verifyPhoneWithFirebaseShouldDelegateToAuthService() {
        com.SocialPairly_Workflow_Manager.entity.User user = new com.SocialPairly_Workflow_Manager.entity.User();
        user.setId(9L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(authService.verifyPhoneWithFirebase(user, "firebase-id-token"))
                .thenReturn(Map.of("message", "Phone verified", "phoneVerified", true));

        ResponseEntity<Map<String, Object>> response = authController.verifyPhoneWithFirebase(
                new com.SocialPairly_Workflow_Manager.dto.FirebasePhoneVerificationRequest("firebase-id-token"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Phone verified", response.getBody().get("message"));
    }

    @Test
    void resendVerificationShouldDelegateToAuthService() {
        ForgotPasswordSendOtpRequest request = new ForgotPasswordSendOtpRequest("ada@example.com");
        when(authService.resendVerificationOtp("ada@example.com"))
                .thenReturn(Map.of("message", "OTP resent"));

        ResponseEntity<Map<String, String>> response =
                authController.resendVerification(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP resent", response.getBody().get("message"));
    }
}
