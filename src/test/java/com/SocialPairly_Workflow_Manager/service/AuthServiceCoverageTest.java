package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private LoginAccountSecurityService loginAccountSecurityService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "defaultCountryCode", "+1");
        lenient().doNothing().when(loginAccountSecurityService).assertAccountAllowsLogin(any(), any(), any());
        lenient().doNothing().when(loginAccountSecurityService).recordSuccessfulLogin(any(), any(), any());
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

        when(userService.findOptionalByIdentifier("not-an-email")).thenReturn(Optional.of(user));
        when(otpService.generateAndStore(eq("PASSWORD_RESET"), eq("not-an-email"))).thenReturn("654321");

        authService.sendForgotPasswordOtp(new ForgotPasswordSendOtpRequest("not-an-email"));

        verify(emailService).sendForgotPasswordOtpEmail(user, "654321");
    }

    @Test
    void resetPasswordShouldEncodeAndClearOtp() {
        User user = new User();
        user.setEmail("reset@example.com");
        user.setPassword("old");

        when(userService.findOptionalByIdentifier("reset@example.com")).thenReturn(Optional.of(user));
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
        when(userService.findOptionalByIdentifier("verify@example.com")).thenReturn(Optional.of(user));

        Map<String, String> result = authService.verifyForgotPasswordOtp(
                new ForgotPasswordVerifyOtpRequest("verify@example.com", "111111"));

        assertEquals("OTP verified successfully", result.get("message"));
        verify(otpService).verify(eq("PASSWORD_RESET"), eq("verify@example.com"), eq("111111"));
    }

    @Test
    void verifyEmailShouldMarkVerifiedAndSetStep2WhenPhoneUnverified() {
        User user = new User();
        user.setId(1L);
        user.setEmail("ada@example.com");
        user.setPhoneVerified(false);
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userProfileRepository.save(profile)).thenReturn(profile);

        Map<String, String> result = authService.verifyEmail(
                new com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest("Ada@Example.com", "123456"));

        assertEquals("Email verified successfully", result.get("message"));
        assertTrue(user.isEmailVerified());
        assertEquals("STEP_2_VERIFY_EMAIL", profile.getOnboardingStep());
        verify(otpService).verify(eq("EMAIL_VERIFY"), eq("ada@example.com"), eq("123456"));
        verify(otpService).clear(eq("EMAIL_VERIFY"), eq("ada@example.com"));
    }

    @Test
    void verifyEmailShouldSetStep4WhenPhoneAlreadyVerified() {
        User user = new User();
        user.setId(1L);
        user.setEmail("ada@example.com");
        user.setPhoneVerified(true);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> result = authService.verifyEmail(
                new com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest("ada@example.com", "123456"));

        assertEquals("Email verified successfully", result.get("message"));
        assertEquals("STEP_4_COMPLETED", user.getProfile().getOnboardingStep());
    }

    @Test
    void verifyEmailShouldThrowWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException.class,
                () -> authService.verifyEmail(
                        new com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest("missing@example.com", "1")));
    }

    @Test
    void verifyPhoneShouldAlwaysThrowLegacyDisabled() {
        assertThrows(BadRequestException.class, () -> authService.verifyPhone(
                new com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest("+12025550123", "123456")));
    }

    @Test
    void resendVerificationOtpShouldSendEmailOtp() throws MessagingException {
        User user = new User();
        user.setEmail("ada@example.com");
        when(userService.findByIdentifier("ada@example.com")).thenReturn(user);
        when(otpService.generateAndStore(eq("EMAIL_VERIFY"), eq("ada@example.com"))).thenReturn("999999");

        Map<String, String> result = authService.resendVerificationOtp("ada@example.com");

        assertEquals("Verification code sent", result.get("message"));
        verify(emailService).sendEmailVerificationOtpEmail(user, "999999");
    }

    @Test
    void resendVerificationOtpShouldRejectPhoneIdentifier() {
        User user = new User();
        when(userService.findByIdentifier("+12025550123")).thenReturn(user);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.resendVerificationOtp("+12025550123"));
        assertTrue(ex.getMessage().contains("Firebase"));
    }

    @Test
    void resendVerificationOtpShouldWrapMessagingException() throws MessagingException {
        User user = new User();
        user.setEmail("ada@example.com");
        when(userService.findByIdentifier("ada@example.com")).thenReturn(user);
        when(otpService.generateAndStore(eq("EMAIL_VERIFY"), eq("ada@example.com"))).thenReturn("999999");
        doThrow(new MessagingException("smtp down"))
                .when(emailService).sendEmailVerificationOtpEmail(user, "999999");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.resendVerificationOtp("ada@example.com"));
        assertTrue(ex.getMessage().contains("Failed to send verification email"));
    }

    @Test
    void registerShouldRejectPasswordMismatch() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "other", "addr",
                true, true, true, true, false);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void registerShouldRejectMissingConsents() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "secret123", "addr",
                false, true, true, true, false);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void registerShouldRejectInvalidPhoneNumber() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "123", "secret123", "secret123", "addr",
                true, true, true, true, false);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void registerShouldSwallowEmailSendFailure() throws MessagingException {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "secret123", "addr",
                true, true, true, true, false);
        User saved = new User();
        saved.setEmail("jane@example.com");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(otpService.generateAndStore(eq("EMAIL_VERIFY"), eq("jane@example.com"))).thenReturn("1234");
        when(jwtUtil.generateToken("jane@example.com", false)).thenReturn("jwt-token");
        doThrow(new MessagingException("smtp down"))
                .when(emailService).sendEmailVerificationOtpEmail(saved, "1234");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
    }

    @Test
    void loginOrRegisterGoogleUserShouldRejectBlankEmail() {
        assertThrows(BadRequestException.class,
                () -> authService.loginOrRegisterGoogleUser("  ", "A", "B", false, true));
        assertThrows(BadRequestException.class,
                () -> authService.loginOrRegisterGoogleUser(null, "A", "B", false, true));
    }

    @Test
    void loginOrRegisterGoogleUserShouldDefaultBlankNamesAndRespectRememberMe() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("new@example.com", true)).thenReturn("remember-token");

        AuthResponse response = authService.loginOrRegisterGoogleUser(
                "new@example.com", "  ", null, true, true);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("User", captor.getValue().getFirstName());
        assertEquals("Account", captor.getValue().getLastName());
        assertEquals("remember-token", response.token());
    }

    @Test
    void loginOrRegisterGoogleUserShouldUpgradeEmailVerifiedWhenClaimTrue() {
        User existing = new User();
        existing.setEmail("google@example.com");
        existing.setEmailVerified(false);
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtUtil.generateToken("google@example.com", false)).thenReturn("tok");

        authService.loginOrRegisterGoogleUser("google@example.com", "G", "U", false, true);

        assertTrue(existing.isEmailVerified());
        verify(userRepository).save(existing);
    }

    @Test
    void loginOrRegisterGoogleUserShouldSwallowWelcomeEmailFailure() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("new@example.com", false)).thenReturn("tok");
        doThrow(new RuntimeException("mail fail")).when(emailService).sendWelcomeEmail(any(User.class));

        assertDoesNotThrow(() ->
                authService.loginOrRegisterGoogleUser("new@example.com", "N", "U", false, true));
    }

    @Test
    void verifyPhoneWithFirebaseShouldVerifyAndSetStep4WhenEmailVerified() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmailVerified(true);
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+12025550199"));
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);
            when(userRepository.existsByPhoneNumberAndIdNot(anyString(), eq(3L))).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userProfileRepository.save(profile)).thenReturn(profile);

            Map<String, Object> result = authService.verifyPhoneWithFirebase(user, "id-token");

            assertEquals("Phone verified successfully", result.get("message"));
            assertTrue(user.isPhoneVerified());
            assertEquals("+1-2025550199", user.getPhoneNumber());
            assertEquals("STEP_4_COMPLETED", profile.getOnboardingStep());
        }
    }

    @Test
    void verifyPhoneWithFirebaseShouldSetStep3WhenEmailUnverified() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmailVerified(false);
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+12025550199"));
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);
            when(userRepository.existsByPhoneNumberAndIdNot(anyString(), eq(3L))).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userProfileRepository.save(profile)).thenReturn(profile);

            authService.verifyPhoneWithFirebase(user, "id-token");

            assertEquals("STEP_3_VERIFY_PHONE", profile.getOnboardingStep());
        }
    }

    @Test
    void verifyPhoneWithFirebaseShouldRejectMissingPhoneClaim() throws Exception {
        User user = new User();
        user.setId(3L);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of());
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.verifyPhoneWithFirebase(user, "id-token"));
            assertTrue(ex.getMessage().contains("phone number"));
        }
    }

    @Test
    void verifyPhoneWithFirebaseShouldRejectPhoneOwnedByOtherUser() throws Exception {
        User user = new User();
        user.setId(3L);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+12025550199"));
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);
            when(userRepository.existsByPhoneNumberAndIdNot(anyString(), eq(3L))).thenReturn(true);

            assertThrows(BadRequestException.class,
                    () -> authService.verifyPhoneWithFirebase(user, "id-token"));
        }
    }

    @Test
    void verifyPhoneWithFirebaseShouldMapFirebaseAuthException() throws Exception {
        User user = new User();
        user.setId(3L);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseAuthException authException = mock(FirebaseAuthException.class);
        when(authException.getMessage()).thenReturn("bad token");

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("bad")).thenThrow(authException);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.verifyPhoneWithFirebase(user, "bad"));
            assertTrue(ex.getMessage().contains("Invalid Firebase token"));
        }
    }

    @Test
    void verifyPhoneWithFirebaseShouldRejectBlankPhoneClaim() throws Exception {
        User user = new User();
        user.setId(3L);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("phone_number", "  "));
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);

            assertThrows(BadRequestException.class,
                    () -> authService.verifyPhoneWithFirebase(user, "id-token"));
        }
    }

    @Test
    void verifyPhoneWithFirebaseShouldRejectInvalidPhoneInToken() throws Exception {
        User user = new User();
        user.setId(3L);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("phone_number", "123"));
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (org.mockito.MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("id-token")).thenReturn(token);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.verifyPhoneWithFirebase(user, "id-token"));
            assertTrue(ex.getMessage().contains("Invalid phone number"));
        }
    }

    @Test
    void registerShouldRejectEachMissingConsentIndependently() {
        assertThrows(BadRequestException.class, () -> authService.register(new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "secret123", "addr",
                true, false, true, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "secret123", "addr",
                true, true, false, true, false)));
        assertThrows(BadRequestException.class, () -> authService.register(new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "+12025550123", "secret123", "secret123", "addr",
                true, true, true, false, false)));
    }

    @Test
    void resendVerificationOtpShouldRejectNullIdentifierAsPhonePath() {
        User user = new User();
        when(userService.findByIdentifier(null)).thenReturn(user);

        assertThrows(BadRequestException.class, () -> authService.resendVerificationOtp(null));
    }

    @Test
    void loginOrRegisterGoogleUserShouldKeepVerifiedWhenClaimFalse() {
        User existing = new User();
        existing.setEmail("google@example.com");
        existing.setEmailVerified(true);
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken("google@example.com", false)).thenReturn("tok");

        authService.loginOrRegisterGoogleUser("google@example.com", "G", "U", false, false);

        assertTrue(existing.isEmailVerified());
        verify(userRepository, never()).save(existing);
    }

    @Test
    void loginOrRegisterGoogleUserThreeArgOverloadDelegates() {
        User existing = new User();
        existing.setEmail("google@example.com");
        existing.setEmailVerified(true);
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken("google@example.com", false)).thenReturn("tok");

        AuthResponse response = authService.loginOrRegisterGoogleUser("google@example.com", "G", "U");

        assertEquals("tok", response.token());
    }
}
