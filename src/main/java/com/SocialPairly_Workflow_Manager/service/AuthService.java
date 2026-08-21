package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final UserService userService;
    private final EmailService emailService;
    private final LoginAccountSecurityService loginAccountSecurityService;

    @Value("${app.sms.default-country-code:+91}")
    private String defaultCountryCode;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       OtpService otpService,
                       UserService userService,
                       EmailService emailService,
                       LoginAccountSecurityService loginAccountSecurityService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.userService = userService;
        this.emailService = emailService;
        this.loginAccountSecurityService = loginAccountSecurityService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user email={} phone={}", request.email(), request.phoneNumber());
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (!request.is18OrOlder() || !request.termsAccepted()
                || !request.privacyAccepted() || !request.identityConsent()) {
            throw new BadRequestException("Required consents must be accepted");
        }
        if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
            throw new BadRequestException("Email is already registered");
        }

        String phoneStorage;
        try {
            phoneStorage = PhoneNumberNormalizer.toStorageFormat(request.phoneNumber(), defaultCountryCode);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid phone number: " + ex.getMessage());
        }

        if (phoneNumberExists(request.phoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPhoneNumber(phoneStorage);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAddress(request.address());
        user.setRole(Role.USER);
        user.setProfileCompleted(false);
        user.set18OrOlder(request.is18OrOlder());
        user.setTermsAccepted(request.termsAccepted());
        user.setPrivacyAccepted(request.privacyAccepted());
        user.setIdentityConsent(request.identityConsent());
        user.setMarketingConsent(request.marketingConsent());
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setUserTokens(UserTokenService.DEFAULT_NEW_USER_TOKENS);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setOnboardingStep("STEP_1_ACCOUNT");
        user.setProfile(profile);

        User saved = userRepository.save(user);

        String emailOtp = otpService.generateAndStore(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_EMAIL_VERIFY,
                saved.getEmail());
        try {
            emailService.sendEmailVerificationOtpEmail(saved, emailOtp);
        } catch (MessagingException e) {
            log.warn("Failed to send email verification OTP to {}: {}", saved.getEmail(), e.getMessage());
        }
        // Phone SMS is handled by Firebase on the client (optional verification)

        String token = jwtUtil.generateToken(saved.getEmail(), false);
        return new AuthResponse(token, UserDto.from(saved));
    }

    @Transactional
    public Map<String, String> verifyEmail(VerifyContactRequest request) {
        String email = request.identifier().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for the given email"));
        otpService.verify(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_EMAIL_VERIFY,
                email,
                request.otp());
        user.setEmailVerified(true);
        UserProfile profile = ensureProfile(user);
        if (!user.isPhoneVerified()) {
            profile.setOnboardingStep("STEP_2_VERIFY_EMAIL");
        } else {
            profile.setOnboardingStep("STEP_4_COMPLETED");
        }
        userProfileRepository.save(profile);
        userRepository.save(user);
        otpService.clear(com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_EMAIL_VERIFY, email);
        return Map.of("message", "Email verified successfully");
    }

    @Transactional
    public Map<String, String> verifyPhone(VerifyContactRequest request) {
        throw new BadRequestException(
                "Legacy phone OTP is disabled. Use Firebase phone verification.");
    }

    /**
     * Verifies a Firebase Phone Auth ID token for the authenticated SocialPairly user,
     * then marks phone_verified=true and stores the E.164 number from the token.
     */
    @Transactional
    public Map<String, Object> verifyPhoneWithFirebase(User currentUser, String idToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            Object phoneClaim = decoded.getClaims().get("phone_number");
            if (phoneClaim == null || phoneClaim.toString().isBlank()) {
                throw new BadRequestException("Firebase token does not contain a verified phone number");
            }
            String phoneE164 = normalizePhoneE164OrThrow(phoneClaim.toString());
            String phoneStorage = PhoneNumberNormalizer.toStorageFormat(phoneE164, defaultCountryCode);

            if (phoneNumberExistsForOtherUser(phoneStorage, currentUser.getId())) {
                throw new BadRequestException("This phone number is already registered to another account");
            }

            currentUser.setPhoneNumber(phoneStorage);
            currentUser.setPhoneVerified(true);
            UserProfile profile = ensureProfile(currentUser);
            if (currentUser.isEmailVerified()) {
                profile.setOnboardingStep("STEP_4_COMPLETED");
            } else {
                profile.setOnboardingStep("STEP_3_VERIFY_PHONE");
            }
            userProfileRepository.save(profile);
            userRepository.save(currentUser);
            log.info("Firebase phone verified for userId={} phone={}", currentUser.getId(), phoneE164);
            return Map.of(
                    "message", "Phone verified successfully",
                    "user", UserDto.from(currentUser)
            );
        } catch (FirebaseAuthException e) {
            log.warn("Firebase idToken verification failed: {}", e.getMessage());
            throw new BadRequestException("Invalid Firebase token. Please try again.");
        }
    }

    public Map<String, String> resendVerificationOtp(String identifier) {
        User user = userService.findByIdentifier(identifier);
        boolean isEmail = identifier != null && identifier.contains("@");
        if (!isEmail) {
            throw new BadRequestException(
                    "Phone SMS is handled by Firebase on the client. Resend from the verification UI.");
        }
        String otp = otpService.generateAndStore(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_EMAIL_VERIFY,
                user.getEmail());
        try {
            emailService.sendEmailVerificationOtpEmail(user, otp);
        } catch (MessagingException e) {
            throw new BadRequestException("Failed to send verification email: " + e.getMessage());
        }
        return Map.of("message", "Verification code sent");
    }

    private String normalizePhoneE164OrThrow(String raw) {
        try {
            return PhoneNumberNormalizer.toE164(raw, defaultCountryCode);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid phone number: " + ex.getMessage());
        }
    }

    private boolean phoneNumberExists(String raw) {
        return PhoneNumberNormalizer.lookupKeys(raw, defaultCountryCode).stream()
                .anyMatch(userRepository::existsByPhoneNumber);
    }

    private boolean phoneNumberExistsForOtherUser(String raw, Long userId) {
        return PhoneNumberNormalizer.lookupKeys(raw, defaultCountryCode).stream()
                .anyMatch(key -> userRepository.existsByPhoneNumberAndIdNot(key, userId));
    }

    private UserProfile ensureProfile(User user) {
        if (user.getProfile() != null) {
            return user.getProfile();
        }
        return userProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setOnboardingStep("STEP_1_ACCOUNT");
            user.setProfile(profile);
            return profile;
        });
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String identifier = request.identifier().trim();
        log.info("Authenticating login request for identifier={}", identifier);
        User user = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userService.findOptionalByPhoneIdentifier(identifier))
                .orElseThrow(() -> new BadCredentialsException(
                        LoginAccountSecurityService.GENERIC_CREDENTIALS_MESSAGE));

        loginAccountSecurityService.assertAccountAllowsLogin(user, identifier, clientIp);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        } catch (BadCredentialsException ex) {
            loginAccountSecurityService.recordFailedPasswordAttempt(user, identifier, clientIp);
            throw ex;
        }

        loginAccountSecurityService.recordSuccessfulLogin(user, identifier, clientIp);

        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        String token = jwtUtil.generateToken(user.getEmail(), rememberMe);
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional
    public AuthResponse loginOrRegisterGoogleUser(String email, String firstName, String lastName) {
        return loginOrRegisterGoogleUser(email, firstName, lastName, false, true);
    }

    @Transactional
    public AuthResponse loginOrRegisterGoogleUser(
            String email,
            String firstName,
            String lastName,
            boolean rememberMe,
            boolean emailVerifiedClaim
    ) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account did not provide an email address");
        }

        String normalizedEmail = email.toLowerCase().trim();
        String safeFirst = (firstName != null && !firstName.isBlank()) ? firstName.trim() : "User";
        String safeLast = (lastName != null && !lastName.isBlank()) ? lastName.trim() : "Account";

        // 1. Look up user by email via your UserRepository
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    // 2. Fallback: Implicit Sign-Up if registration record is empty
                    User newUser = new User();
                    newUser.setEmail(normalizedEmail);
                    newUser.setFirstName(safeFirst);
                    newUser.setLastName(safeLast);
                    newUser.setRole(Role.USER);
                    newUser.setProfileCompleted(false);
                    // phone_number is UNIQUE + NOT NULL — never reuse "" for OAuth users
                    newUser.setPhoneNumber(oauthPhonePlaceholder(normalizedEmail));
                    newUser.setPassword("");
                    newUser.setEmailVerified(emailVerifiedClaim);
                    newUser.setPhoneVerified(false);
                    newUser.setUserTokens(UserTokenService.DEFAULT_NEW_USER_TOKENS);
                    User saved = userRepository.save(newUser);
                    try {
                        emailService.sendWelcomeEmail(saved);
                    } catch (Exception mailEx) {
                        log.warn("Welcome email failed for google user {}: {}", normalizedEmail, mailEx.getMessage());
                    }
                    return saved;
                });

        loginAccountSecurityService.assertAccountAllowsLogin(user, normalizedEmail, null);

        if (emailVerifiedClaim && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        // 3. Generate your local system's secure context token string
        String systemJwtToken = jwtUtil.generateToken(user.getEmail(), rememberMe);

        // 4. Return matching traditional AuthResponse block format using your Record/DTO structure
        return new AuthResponse(systemJwtToken, UserDto.from(user));
    }

    /** Unique placeholder so Google/Apple sign-ups do not collide on users_details.phone_number. */
    private static String oauthPhonePlaceholder(String email) {
        String compact = Integer.toHexString(email.hashCode());
        String value = "oauth:" + compact;
        return value.length() <= 20 ? value : value.substring(0, 20);
    }

    public static final String UNREGISTERED_IDENTIFIER_MESSAGE =
            "This is not your registered Email ID. Please try with your registered Email ID.";

    /**
     * Starts forgot-password OTP delivery. Unknown identifiers are rejected with a clear message.
     */
    public void sendForgotPasswordOtp(ForgotPasswordSendOtpRequest request) throws MessagingException {
        log.info("Sending forgot password OTP for identifier={}", request.identifier());
        User user = userService.findOptionalByIdentifier(request.identifier())
                .orElseThrow(() -> new BadRequestException(UNREGISTERED_IDENTIFIER_MESSAGE));
        String otp = otpService.generateAndStore(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_PASSWORD_RESET,
                request.identifier());
        emailService.sendForgotPasswordOtpEmail(user, otp);

        String destination = user.getEmail().contains("@")
                ? user.getEmail()
                : user.getPhoneNumber();
        log.info("Password reset OTP sent to {} for user {}", destination, user.getEmail());
    }

    public Map<String, String> verifyForgotPasswordOtp(ForgotPasswordVerifyOtpRequest request) {
        log.info("Verifying forgot password OTP for identifier={}", request.identifier());
        if (userService.findOptionalByIdentifier(request.identifier()).isEmpty()) {
            // Same client-facing signal as a wrong OTP — do not reveal account existence.
            throw new BadRequestException("Invalid OTP");
        }
        otpService.verify(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_PASSWORD_RESET,
                request.identifier(),
                request.otp());
        return Map.of("message", "OTP verified successfully");
    }

    @Transactional
    public Map<String, String> resetPassword(ForgotPasswordResetRequest request) {
        log.info("Resetting password for identifier={}", request.identifier());
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        otpService.assertVerified(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_PASSWORD_RESET,
                request.identifier(),
                request.otp());
        User user = userService.findOptionalByIdentifier(request.identifier())
                .orElseThrow(() -> new BadRequestException(
                        "Unable to reset password. Please request a new OTP and try again."));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        otpService.clear(
                com.SocialPairly_Workflow_Manager.entity.AuthOtpCode.PURPOSE_PASSWORD_RESET,
                request.identifier());
        return Map.of("message", "Password reset successfully");
    }
}