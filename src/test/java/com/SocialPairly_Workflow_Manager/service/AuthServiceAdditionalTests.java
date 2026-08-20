package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceAdditionalTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.SocialPairly_Workflow_Manager.repository.UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpService otpService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "defaultCountryCode", "+1");
    }

    @Test
    void registerShouldThrowWhenEmailTaken() {
        RegisterRequest request = new RegisterRequest("A", "B", "test@example.com", "1234567", "pass123", "pass123", "addr", true, true, true, true, false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void loginShouldThrowWhenIdentifierMissing() {
        LoginRequest request = new LoginRequest("missing@example.com", "pass123", null);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(userService.findOptionalByPhoneIdentifier("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void resetPasswordShouldThrowWhenPasswordsDontMatch() {
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest("test@example.com", "123456", "pass1", "pass2");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
    }

    @Test
    void sendForgotPasswordOtpShouldUseEmailService() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");

        when(userService.findByIdentifier("test@example.com")).thenReturn(user);
        when(otpService.generateAndStore(eq("PASSWORD_RESET"), eq("test@example.com"))).thenReturn("123456");

        authService.sendForgotPasswordOtp(new ForgotPasswordSendOtpRequest("test@example.com"));

        verify(emailService).sendForgotPasswordOtpEmail(user, "123456");
    }

    @Test
    void verifyForgotPasswordOtpShouldReturnSuccessMessage() {
        when(userService.findByIdentifier("test@example.com")).thenReturn(new User());
        doNothing().when(otpService).verify(eq("PASSWORD_RESET"), eq("test@example.com"), eq("123456"));

        Map<String, String> result = authService.verifyForgotPasswordOtp(new ForgotPasswordVerifyOtpRequest("test@example.com", "123456"));

        assertEquals("OTP verified successfully", result.get("message"));
    }
}
