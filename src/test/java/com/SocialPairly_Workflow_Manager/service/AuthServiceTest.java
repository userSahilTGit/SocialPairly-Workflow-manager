package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private JwtUtil jwtUtil;

    private final OtpService otpService = new OtpService();

    @Mock
    private UserService userService;

    @Mock
    private JavaMailSender mailSender;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("01234567890123456789012345678901", 3600000);
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager,
                jwtUtil, otpService, userService, mailSender);
    }

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest(
                "Jane",
                "Doe",
                "jane@example.com",
                "+12025550123",
                "secret123",
                "123 Main St"
        );

        when(userRepository.existsByEmail(eq("jane@example.com"))).thenReturn(false);
        when(userRepository.existsByPhoneNumber(eq("+12025550123"))).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("jane@example.com", response.user().email());

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        User savedUser = savedCaptor.getValue();
        assertEquals("Jane", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals("jane@example.com", savedUser.getEmail());
        assertEquals("+12025550123", savedUser.getPhoneNumber());
        assertEquals("encoded-password", savedUser.getPassword());
        assertFalse(savedUser.isProfileCompleted());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Jane",
                "Doe",
                "jane@example.com",
                "+12025550123",
                "secret123",
                "123 Main St"
        );

        when(userRepository.existsByEmail(eq("jane@example.com"))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginWithEmail() {
        LoginRequest request = new LoginRequest("jane@example.com", "secret123");
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail(eq("jane@example.com"))).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    void shouldLoginWithPhoneNumber() {
        LoginRequest request = new LoginRequest("+12025550123", "secret123");
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail(eq("+12025550123"))).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(eq("+12025550123"))).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(mock(org.springframework.security.core.Authentication.class));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    void shouldResetPasswordSuccessfullyWhenOtpVerified() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("old-password");

        String otp = otpService.generateAndStore("jane@example.com");
        otpService.verify("jane@example.com", otp);

        when(userService.findByIdentifier("jane@example.com")).thenReturn(user);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> result = authService.resetPassword(
                new ForgotPasswordResetRequest("jane@example.com", otp, "newpass", "newpass"));

        assertEquals("Password reset successfully", result.get("message"));
        assertEquals("encoded-newpass", user.getPassword());
    }
}
