package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.GlobalExceptionHandler;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.SocialPairly_Workflow_Manager.security.JwtTokenBlacklistService;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.SocialPairly_Workflow_Manager.service.AuthRateLimitService;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP/API-level coverage for forgot-password endpoints (DEV-010 / AC10 / DEV-002).
 */
@ExtendWith(MockitoExtension.class)
class AuthForgotPasswordApiTest {

    private static final String SUCCESS_MESSAGE =
            AuthService.FORGOT_PASSWORD_DISPATCH_MESSAGE;

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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authService,
                currentUserService,
                authRateLimitService,
                authCookieService,
                jwtUtil,
                tokenBlacklistService,
                googleIdTokenVerifier);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /forgot-password/send-otp returns 200 when account exists")
    void sendOtpReturnsSuccess() throws Exception {
        doNothing().when(authService).sendForgotPasswordOtp(any());

        mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"member@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE));

        verify(authRateLimitService).check(
                eq(AuthRateLimitService.ACTION_FORGOT_SEND), anyString(), eq("member@example.com"));
        verify(authService).sendForgotPasswordOtp(any());
    }

    @Test
    @DisplayName("POST /forgot-password/send-otp returns same 200 for unknown account (AC11)")
    void sendOtpDoesNotRevealUnregisteredIdentifier() throws Exception {
        doNothing().when(authService).sendForgotPasswordOtp(any());

        mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"missing@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE));

        verify(authService).sendForgotPasswordOtp(any());
    }

    @Test
    @DisplayName("POST /forgot-password/send-otp accepts valid phone identifier")
    void sendOtpAcceptsPhoneIdentifier() throws Exception {
        doNothing().when(authService).sendForgotPasswordOtp(any());

        mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"9876543210"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE));
    }

    @Test
    @DisplayName("POST /forgot-password/send-otp rejects malformed identifier")
    void sendOtpRejectsInvalidIdentifier() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"not-valid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(authService, never()).sendForgotPasswordOtp(any());
    }

    @Test
    @DisplayName("POST /forgot-password/verify-otp returns 200 when OTP valid")
    void verifyOtpSuccess() throws Exception {
        when(authService.verifyForgotPasswordOtp(any()))
                .thenReturn(Map.of("message", "OTP verified successfully"));

        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"member@example.com","otp":"1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    @DisplayName("POST /forgot-password/verify-otp returns 400 Invalid OTP for unknown account (no 404)")
    void verifyOtpUnknownAccountDoesNotRevealExistence() throws Exception {
        when(authService.verifyForgotPasswordOtp(any()))
                .thenThrow(new BadRequestException("Invalid OTP"));

        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"missing@example.com","otp":"1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /forgot-password/reset returns 200 on success")
    void resetPasswordSuccess() throws Exception {
        when(authService.resetPassword(any()))
                .thenReturn(Map.of("message", "Password reset successfully"));

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier":"member@example.com",
                                  "otp":"1234",
                                  "newPassword":"secret1",
                                  "confirmPassword":"secret1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    @DisplayName("POST /forgot-password/reset returns 400 (not 404) when reset cannot complete")
    void resetPasswordUnknownAccountDoesNotRevealExistence() throws Exception {
        when(authService.resetPassword(any()))
                .thenThrow(new BadRequestException(
                        "Unable to reset password. Please request a new OTP and try again."));

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier":"missing@example.com",
                                  "otp":"1234",
                                  "newPassword":"secret1",
                                  "confirmPassword":"secret1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /forgot-password/reset rejects short passwords")
    void resetPasswordRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier":"member@example.com",
                                  "otp":"1234",
                                  "newPassword":"123",
                                  "confirmPassword":"123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(authService, never()).resetPassword(any());
    }
}
