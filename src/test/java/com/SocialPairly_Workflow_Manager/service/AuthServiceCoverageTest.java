package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceCoverageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

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
    void registerShouldThrowWhenPhoneAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "secret123", "123 Main St",
                true, true, true, true, false);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+12025550123")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldSendEmailVerificationOtp() throws MessagingException {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "JANE@EXAMPLE.COM ", "+12025550123", "secret123", "secret123", "123 Main St",
                true, true, true, true, false);
        User saved = new User();
        saved.setEmail("jane@example.com");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+12025550123")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(otpService.generateAndStore(eq("EMAIL_VERIFY"), eq("jane@example.com"))).thenReturn("1234");
        when(jwtUtil.generateToken("jane@example.com", false)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        verify(emailService).sendEmailVerificationOtpEmail(saved, "1234");
        assertEquals("jwt-token", response.token());
    }

    @Test
    void loginOrRegisterGoogleUserShouldReturnExistingUser() {
        User existing = new User();
        existing.setEmail("google@example.com");
        existing.setFirstName("Existing");
        existing.setLastName("User");
        existing.setEmailVerified(true);

        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken("google@example.com", false)).thenReturn("existing-token");

        AuthResponse response = authService.loginOrRegisterGoogleUser(
                "Google@Example.com ", "Ignored", "Ignored");

        assertEquals("existing-token", response.token());
        assertEquals("google@example.com", response.user().email());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any());
    }

    @Test
    void loginOrRegisterGoogleUserShouldCreateNewUserAndSendWelcomeEmail() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setEmail("new@example.com");
            return user;
        });
        when(jwtUtil.generateToken("new@example.com", false)).thenReturn("new-token");

        AuthResponse response = authService.loginOrRegisterGoogleUser(
                "new@example.com", "New", "User");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();
        assertEquals("new@example.com", created.getEmail());
        assertEquals("New", created.getFirstName());
        assertEquals("User", created.getLastName());
        assertEquals(Role.USER, created.getRole());
        assertFalse(created.isProfileCompleted());
        assertTrue(created.getPhoneNumber().startsWith("oauth:"));
        assertEquals("", created.getPassword());

        verify(emailService).sendWelcomeEmail(any(User.class));
        assertEquals("new-token", response.token());
    }

    @Test
    void sendForgotPasswordOtpShouldLogPhoneDestinationWhenEmailHasNoAt() throws MessagingException {
        User user = new User();
        user.setEmail("not-an-email");
        user.setPhoneNumber("+15551234567");

        when(userService.findByIdentifier("not-an-email")).thenReturn(user);
        when(otpService.generateAndStore(eq("PASSWORD_RESET"), eq("not-an-email"))).thenReturn("654321");

        authService.sendForgotPasswordOtp(new ForgotPasswordSendOtpRequest("not-an-email"));

        verify(emailService).sendForgotPasswordOtpEmail(user, "654321");
    }

    @Test
    void resetPasswordShouldEncodeAndClearOtp() {
        User user = new User();
        user.setEmail("reset@example.com");
        user.setPassword("old");

        when(userService.findByIdentifier("reset@example.com")).thenReturn(user);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");
        when(userRepository.save(user)).thenReturn(user);

        Map<String, String> result = authService.resetPassword(
                new ForgotPasswordResetRequest("reset@example.com", "999999", "newpass", "newpass"));

        assertEquals("Password reset successfully", result.get("message"));
        assertEquals("encoded-new", user.getPassword());
        verify(otpService).assertVerified(eq("PASSWORD_RESET"), eq("reset@example.com"), eq("999999"));
        verify(otpService).clear(eq("PASSWORD_RESET"), eq("reset@example.com"));
    }

    @Test
    void verifyForgotPasswordOtpShouldDelegateToOtpService() {
        User user = new User();
        when(userService.findByIdentifier("verify@example.com")).thenReturn(user);

        Map<String, String> result = authService.verifyForgotPasswordOtp(
                new ForgotPasswordVerifyOtpRequest("verify@example.com", "111111"));

        assertEquals("OTP verified successfully", result.get("message"));
        verify(otpService).verify(eq("PASSWORD_RESET"), eq("verify@example.com"), eq("111111"));
    }
}
