package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.AuthResponse;
import com.SocialPairly_Workflow_Manager.dto.FirebasePhoneVerificationRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordResetRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordSendOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.ForgotPasswordVerifyOtpRequest;
import com.SocialPairly_Workflow_Manager.dto.LoginRequest;
import com.SocialPairly_Workflow_Manager.dto.AppleAuthRequest;
import com.SocialPairly_Workflow_Manager.dto.RegisterRequest;
import com.SocialPairly_Workflow_Manager.dto.TokenDto;
import com.SocialPairly_Workflow_Manager.dto.VerifyContactRequest;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.SocialPairly_Workflow_Manager.security.JwtTokenBlacklistService;
import com.SocialPairly_Workflow_Manager.security.JwtUtil;
import com.SocialPairly_Workflow_Manager.service.AuthRateLimitService;
import com.SocialPairly_Workflow_Manager.service.AuthService;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final AuthRateLimitService authRateLimitService;
    private final AuthCookieService authCookieService;
    private final JwtUtil jwtUtil;
    private final JwtTokenBlacklistService tokenBlacklistService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthController(
            AuthService authService,
            CurrentUserService currentUserService,
            AuthRateLimitService authRateLimitService,
            AuthCookieService authCookieService,
            JwtUtil jwtUtil,
            JwtTokenBlacklistService tokenBlacklistService,
            GoogleIdTokenVerifier googleIdTokenVerifier
    ) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.authRateLimitService = authRateLimitService;
        this.authCookieService = authCookieService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse httpResponse
    ) {
        log.info("Register request received for email={}", request.email());
        AuthResponse authResponse = authService.register(request);
        authCookieService.writeAuthCookie(httpResponse, authResponse.token(), false);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        log.info("Login request received for identifier={}", request.identifier());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_LOGIN,
                clientIp(httpRequest),
                request.identifier());
        AuthResponse authResponse = authService.login(request, clientIp(httpRequest));
        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        authCookieService.writeAuthCookie(httpResponse, authResponse.token(), rememberMe);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Clears the auth cookie and revokes the current JWT so Remember Me / stolen tokens
     * cannot be reused after logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String token = authCookieService.resolveToken(httpRequest);
        if (token != null && !token.isBlank()) {
            try {
                if (jwtUtil.isTokenValid(token)) {
                    Date expiresAt = jwtUtil.extractExpiration(token);
                    tokenBlacklistService.revoke(token, expiresAt);
                }
            } catch (Exception e) {
                log.debug("Logout token revoke skipped: {}", e.toString());
            }
        }
        authCookieService.clearAuthCookie(httpResponse);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<Map<String, String>> sendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordSendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Forgot-password OTP request received for identifier={}", request.identifier());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_FORGOT_SEND,
                clientIp(httpRequest),
                request.identifier());
        try {
            authService.sendForgotPasswordOtp(request);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent successfully to your registered email or phone number"));
        } catch (jakarta.mail.MessagingException e) {
            log.error("Failed to send forgot password OTP for identifier={}", request.identifier(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to send OTP. Please try again later."));
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<Map<String, String>> verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordVerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("OTP verification request received for identifier={}", request.identifier());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_FORGOT_VERIFY,
                clientIp(httpRequest),
                request.identifier());
        return ResponseEntity.ok(authService.verifyForgotPasswordOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequest request) {
        log.info("Reset password request received for identifier={}", request.identifier());
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyContactRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Email verification request for identifier={}", request.identifier());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_VERIFY_EMAIL,
                clientIp(httpRequest),
                request.identifier());
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<Map<String, String>> verifyPhone(
            @Valid @RequestBody VerifyContactRequest request) {
        log.info("Legacy phone verification request for identifier={}", request.identifier());
        return ResponseEntity.ok(authService.verifyPhone(request));
    }

    @PostMapping("/phone-verification/firebase")
    public ResponseEntity<Map<String, Object>> verifyPhoneWithFirebase(
            @Valid @RequestBody FirebasePhoneVerificationRequest request) {
        User user = currentUserService.getCurrentUser();
        log.info("Firebase phone verification for userId={}", user.getId());
        return ResponseEntity.ok(authService.verifyPhoneWithFirebase(user, request.idToken()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ForgotPasswordSendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Resend verification OTP for identifier={}", request.identifier());
        authRateLimitService.check(
                AuthRateLimitService.ACTION_RESEND_VERIFICATION,
                clientIp(httpRequest),
                request.identifier());
        return ResponseEntity.ok(authService.resendVerificationOtp(request.identifier()));
    }

    @PostMapping("/apple")
    public ResponseEntity<?> verifyAppleToken(@RequestBody AppleAuthRequest request) {
        try {
            // Production: validate Apple identity token with Apple's JWKS.
            // Placeholder keeps the OAuth button wired for local/dev flows.
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Apple Sign-In validation is not fully configured yet."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> verifyGoogleToken(
            @RequestBody TokenDto tokenDto,
            HttpServletResponse httpResponse
    ) {
        try {
            if (tokenDto == null || tokenDto.getIdToken() == null || tokenDto.getIdToken().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Missing Google identity token"));
            }

            GoogleIdToken idToken = googleIdTokenVerifier.verify(tokenDto.getIdToken());

            if (idToken != null) {
                Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");
                boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
                boolean rememberMe = Boolean.TRUE.equals(tokenDto.getRememberMe());

                AuthResponse authResponse = authService.loginOrRegisterGoogleUser(
                        email, firstName, lastName, rememberMe, emailVerified);
                authCookieService.writeAuthCookie(httpResponse, authResponse.token(), rememberMe);

                return ResponseEntity.ok(authResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid Google Identity Token."));
            }
        } catch (com.SocialPairly_Workflow_Manager.exception.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (java.security.GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Google token verification failed: {}", e.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid Google Identity Token."));
        } catch (Exception e) {
            log.error("Google Sign-In failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Google Sign-In failed. Please try again."));
        }
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
