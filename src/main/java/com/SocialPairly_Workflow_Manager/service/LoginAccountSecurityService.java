package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.LoginSecurityEvent;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.AccountSecurityException;
import com.SocialPairly_Workflow_Manager.repository.LoginSecurityEventRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Password-login lock / disable policy by subscription + email-verification tier.
 * Persist every relevant action to {@link LoginSecurityEvent} for fraud detection.
 */
@Service
public class LoginAccountSecurityService {

    private static final Logger log = LoggerFactory.getLogger(LoginAccountSecurityService.class);

    public static final String GENERIC_CREDENTIALS_MESSAGE = "Invalid credentials";
    public static final String LOCKED_SUBSCRIBED_MESSAGE =
            "Your account is temporarily locked due to multiple failed login attempts. Please try again later.";
    public static final String DISABLED_SUBSCRIBED_MESSAGE =
            "Your account has been disabled. Please reach out to our Administrator to get your account active again.";

    public static final String CODE_LOCKED = "ACCOUNT_LOCKED";
    public static final String CODE_DISABLED = "ACCOUNT_DISABLED";

    private static final int FAILED_ATTEMPTS_BEFORE_LOCK = 3;
    private static final int LOCKS_24H_SUBSCRIBED_DISABLE = 5;
    private static final int LOCKS_24H_UNSUB_VERIFIED_DISABLE = 3;

    private final UserRepository userRepository;
    private final LoginSecurityEventRepository loginSecurityEventRepository;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;

    public LoginAccountSecurityService(
            UserRepository userRepository,
            LoginSecurityEventRepository loginSecurityEventRepository,
            SubscriptionService subscriptionService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.loginSecurityEventRepository = loginSecurityEventRepository;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
    }

    public enum SecurityTier {
        UNSUBSCRIBED_UNVERIFIED,
        UNSUBSCRIBED_VERIFIED,
        SUBSCRIBED
    }

    public SecurityTier resolveTier(User user) {
        if (subscriptionService.hasActiveSubscription(user.getId())) {
            return SecurityTier.SUBSCRIBED;
        }
        if (user.isEmailVerified()) {
            return SecurityTier.UNSUBSCRIBED_VERIFIED;
        }
        return SecurityTier.UNSUBSCRIBED_UNVERIFIED;
    }

    /**
     * Rejects disabled / currently locked accounts before password check.
     * Clears expired locks so the user gets a fresh attempt window.
     */
    @Transactional(noRollbackFor = {
            BadCredentialsException.class,
            AccountSecurityException.class
    })
    public void assertAccountAllowsLogin(User user, String identifier, String clientIp) {
        if (user.isAccountDisabled()) {
            rejectDisabled(user, identifier, clientIp);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = user.getAccountLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            rejectLocked(user, identifier, clientIp);
        }

        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            clearExpiredLock(user, identifier, clientIp);
        }
    }

    @Transactional
    public void recordSuccessfulLogin(User user, String identifier, String clientIp) {
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        persistEvent(user.getId(), identifier, LoginSecurityEvent.LOGIN_SUCCESS,
                resolveTier(user).name(), clientIp, null);
    }

    /**
     * Increments failed attempts; locks after 3 failures; may permanently disable per tier rules.
     * Must commit even when throwing auth failures so lock/disable state and audit events persist.
     */
    @Transactional(noRollbackFor = {
            BadCredentialsException.class,
            AccountSecurityException.class
    })
    public void recordFailedPasswordAttempt(User user, String identifier, String clientIp) {
        SecurityTier tier = resolveTier(user);
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        userRepository.save(user);

        persistEvent(user.getId(), identifier, LoginSecurityEvent.FAILED_LOGIN,
                tier.name(), clientIp, "attempt=" + attempts);

        if (attempts < FAILED_ATTEMPTS_BEFORE_LOCK) {
            throw new BadCredentialsException(GENERIC_CREDENTIALS_MESSAGE);
        }

        applyLockOrDisable(user, identifier, clientIp, tier);
    }

    private void applyLockOrDisable(User user, String identifier, String clientIp, SecurityTier tier) {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        long locksIn24h = loginSecurityEventRepository.countByUserIdAndEventTypeSince(
                user.getId(), LoginSecurityEvent.ACCOUNT_LOCKED, since24h);
        long priorLocksLifetime = loginSecurityEventRepository.countByUserIdAndEventType(
                user.getId(), LoginSecurityEvent.ACCOUNT_LOCKED);

        // Unverified unsubscribed: first lock = 1 day; any subsequent lock → permanent disable.
        // Verified unsubscribed: disable on 3rd lock within 24h.
        // Subscribed: disable on 5th lock within 24h.
        boolean shouldDisable = switch (tier) {
            case UNSUBSCRIBED_UNVERIFIED -> priorLocksLifetime >= 1;
            case UNSUBSCRIBED_VERIFIED -> locksIn24h + 1 >= LOCKS_24H_UNSUB_VERIFIED_DISABLE;
            case SUBSCRIBED -> locksIn24h + 1 >= LOCKS_24H_SUBSCRIBED_DISABLE;
        };

        if (shouldDisable) {
            disableAccount(user, identifier, clientIp, tier, "locksIn24hBefore=" + locksIn24h);
            rejectDisabled(user, identifier, clientIp);
            return;
        }

        LocalDateTime lockedUntil = LocalDateTime.now().plus(lockDuration(tier));
        user.setAccountLockedUntil(lockedUntil);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        persistEvent(user.getId(), identifier, LoginSecurityEvent.ACCOUNT_LOCKED,
                tier.name(), clientIp, "lockedUntil=" + lockedUntil);

        // Server-side audit only — never expose lock details for stealth tiers
        if (tier == SecurityTier.SUBSCRIBED) {
            log.info("Subscribed account locked userId={} until={}", user.getId(), lockedUntil);
            throw new AccountSecurityException(HttpStatus.UNAUTHORIZED, CODE_LOCKED, LOCKED_SUBSCRIBED_MESSAGE);
        }
        log.debug("Account locked (stealth) userId={}", user.getId());
        throw new BadCredentialsException(GENERIC_CREDENTIALS_MESSAGE);
    }

    private void disableAccount(User user, String identifier, String clientIp, SecurityTier tier, String details) {
        user.setAccountDisabled(true);
        user.setDisabledAt(LocalDateTime.now());
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        persistEvent(user.getId(), identifier, LoginSecurityEvent.ACCOUNT_DISABLED,
                tier.name(), clientIp, details);

        try {
            emailService.sendAccountDisabledEmail(user);
        } catch (Exception mailEx) {
            log.warn("Account-disabled email failed for userId={}: {}", user.getId(), mailEx.getMessage());
        }

        if (tier == SecurityTier.SUBSCRIBED) {
            log.info("Subscribed account disabled userId={}", user.getId());
        } else {
            log.debug("Account disabled (stealth) userId={}", user.getId());
        }
    }

    private void clearExpiredLock(User user, String identifier, String clientIp) {
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        persistEvent(user.getId(), identifier, LoginSecurityEvent.LOCK_EXPIRED_CLEARED,
                resolveTier(user).name(), clientIp, null);
    }

    private void rejectDisabled(User user, String identifier, String clientIp) {
        SecurityTier tier = resolveTier(user);
        if (tier == SecurityTier.SUBSCRIBED) {
            throw new AccountSecurityException(HttpStatus.FORBIDDEN, CODE_DISABLED, DISABLED_SUBSCRIBED_MESSAGE);
        }
        throw new BadCredentialsException(GENERIC_CREDENTIALS_MESSAGE);
    }

    private void rejectLocked(User user, String identifier, String clientIp) {
        SecurityTier tier = resolveTier(user);
        if (tier == SecurityTier.SUBSCRIBED) {
            throw new AccountSecurityException(HttpStatus.UNAUTHORIZED, CODE_LOCKED, LOCKED_SUBSCRIBED_MESSAGE);
        }
        throw new BadCredentialsException(GENERIC_CREDENTIALS_MESSAGE);
    }

    private static java.time.Duration lockDuration(SecurityTier tier) {
        return switch (tier) {
            case UNSUBSCRIBED_UNVERIFIED -> java.time.Duration.ofDays(1);
            case UNSUBSCRIBED_VERIFIED -> java.time.Duration.ofHours(2);
            case SUBSCRIBED -> java.time.Duration.ofMinutes(30);
        };
    }

    private void persistEvent(
            Long userId,
            String identifier,
            String eventType,
            String securityTier,
            String clientIp,
            String details
    ) {
        LoginSecurityEvent event = new LoginSecurityEvent();
        event.setUserId(userId);
        event.setIdentifier(identifier == null ? "" : identifier.trim());
        event.setEventType(eventType);
        event.setSecurityTier(securityTier);
        event.setClientIp(clientIp);
        if (details != null && details.length() > 500) {
            event.setDetails(details.substring(0, 500));
        } else {
            event.setDetails(details);
        }
        loginSecurityEventRepository.save(event);
    }
}
