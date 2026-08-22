package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
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
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private JwtUtil jwtUtil;

    @Mock
    private OtpService otpService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private LoginAccountSecurityService loginAccountSecurityService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("01234567890123456789012345678901", 3600000, 2592000000L);
        authService = new AuthService(userRepository, userProfileRepository, passwordEncoder, authenticationManager,
                jwtUtil, otpService, userService, emailService, loginAccountSecurityService);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "defaultCountryCode", "+1");
        lenient().doNothing().when(loginAccountSecurityService).assertAccountAllowsLogin(any(), any(), any());
        lenient().doNothing().when(loginAccountSecurityService).recordSuccessfulLogin(any(), any(), any());
    }

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest(
                "Jane",
                "Doe",
                "jane@example.com",
                "+12025550123",
                "secret123",
                "secret123",
                "123 Main St",
                true,
                true,
                true,
                true,
                false
        );

        when(userRepository.existsByEmail(eq("jane@example.com"))).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(otpService.generateAndStore(any(), any())).thenReturn("1234");

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
        assertEquals("+1-2025550123", savedUser.getPhoneNumber());
        assertEquals("encoded-password", savedUser.getPassword());
        assertFalse(savedUser.isProfileCompleted());
        assertTrue(savedUser.is18OrOlder());
        assertTrue(savedUser.isTermsAccepted());
        assertTrue(savedUser.isPrivacyAccepted());
        assertTrue(savedUser.isIdentityConsent());
        assertFalse(savedUser.isMarketingConsent());
        assertNotNull(savedUser.getProfile());
        assertEquals("STEP_1_ACCOUNT", savedUser.getProfile().getOnboardingStep());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Jane",
                "Doe",
                "jane@example.com",
                "+12025550123",
                "secret123",
                "secret123",
                "123 Main St",
                true,
                true,
                true,
                true,
                false
        );

        when(userRepository.existsByEmail(eq("jane@example.com"))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginWithEmail() {
        LoginRequest request = new LoginRequest("jane@example.com", "secret123", null);
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail(eq("jane@example.com"))).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        doNothing().when(loginAccountSecurityService).assertAccountAllowsLogin(any(), any(), any());
        doNothing().when(loginAccountSecurityService).recordSuccessfulLogin(any(), any(), any());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    void shouldLoginWithPhoneNumber() {
        LoginRequest request = new LoginRequest("+1-2025550123", "secret123", true);
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail(eq("+1-2025550123"))).thenReturn(Optional.empty());
        when(userService.findOptionalByPhoneIdentifier(eq("+1-2025550123"))).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(mock(org.springframework.security.core.Authentication.class));
        doNothing().when(loginAccountSecurityService).assertAccountAllowsLogin(any(), any(), any());
        doNothing().when(loginAccountSecurityService).recordSuccessfulLogin(any(), any(), any());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jane@example.com", response.user().email());
        // rememberMe true → longer TTL than session token
        long sessionTtl = jwtUtil.resolveExpirationMs(false);
        long rememberTtl = jwtUtil.resolveExpirationMs(true);
        assertTrue(rememberTtl > sessionTtl);
        assertTrue(jwtUtil.extractExpiration(response.token()).getTime() - System.currentTimeMillis()
                > sessionTtl - 60_000);
    }

    @Test
    void shouldLoginWithEmailCaseInsensitive() {
        LoginRequest request = new LoginRequest("Jane@Example.com", "secret123", false);
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail(eq("jane@example.com"))).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        doNothing().when(loginAccountSecurityService).assertAccountAllowsLogin(any(), any(), any());
        doNothing().when(loginAccountSecurityService).recordSuccessfulLogin(any(), any(), any());

        AuthResponse response = authService.login(request);
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    void shouldResetPasswordSuccessfullyWhenOtpVerified() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("old-password");

        doNothing().when(otpService).assertVerified(any(), eq("jane@example.com"), eq("1234"));
        doNothing().when(otpService).clear(any(), eq("jane@example.com"));
        when(userService.findOptionalByIdentifier("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> result = authService.resetPassword(
                new ForgotPasswordResetRequest("jane@example.com", "1234", "newpass", "newpass"));

        assertEquals("Password reset successfully", result.get("message"));
        assertEquals("encoded-newpass", user.getPassword());
    }
}
