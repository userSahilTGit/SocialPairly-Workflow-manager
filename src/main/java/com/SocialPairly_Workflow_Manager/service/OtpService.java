package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.AuthOtpCode;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.AuthOtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final AuthOtpCodeRepository authOtpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public OtpService(AuthOtpCodeRepository authOtpCodeRepository, PasswordEncoder passwordEncoder) {
        this.authOtpCodeRepository = authOtpCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Email verification OTP. */
    @Transactional
    public String generateAndStore(String identifier) {
        return generateAndStore(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier);
    }

    @Transactional
    public String generateAndStore(String purpose, String identifier) {
        String key = normalize(identifier);
        LocalDateTime now = LocalDateTime.now();
        authOtpCodeRepository.consumeAllActive(purpose, key, now);

        String otp = String.format("%04d", random.nextInt(10000));
        AuthOtpCode row = new AuthOtpCode();
        row.setPurpose(purpose);
        row.setIdentifier(key);
        row.setCodeHash(passwordEncoder.encode(otp));
        row.setExpiresAt(now.plusMinutes(OTP_TTL_MINUTES));
        row.setVerified(false);
        row.setAttemptCount(0);
        authOtpCodeRepository.save(row);
        log.info("OTP generated purpose={} identifier={} (valid {} minutes)", purpose, key, OTP_TTL_MINUTES);
        return otp;
    }

    @Transactional
    public void verify(String identifier, String otp) {
        verify(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier, otp);
    }

    @Transactional
    public void verify(String purpose, String identifier, String otp) {
        AuthOtpCode entry = loadActiveOrThrow(purpose, identifier);
        if (entry.getAttemptCount() != null && entry.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BadRequestException("Too many invalid OTP attempts. Please request a new one.");
        }
        if (LocalDateTime.now().isAfter(entry.getExpiresAt())) {
            entry.setConsumedAt(LocalDateTime.now());
            authOtpCodeRepository.save(entry);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        boolean matches = passwordEncoder.matches(otp == null ? "" : otp.trim(), entry.getCodeHash());
        entry.setAttemptCount(entry.getAttemptCount() == null ? 1 : entry.getAttemptCount() + 1);
        if (!matches) {
            authOtpCodeRepository.save(entry);
            if (entry.getAttemptCount() >= MAX_ATTEMPTS) {
                entry.setConsumedAt(LocalDateTime.now());
                authOtpCodeRepository.save(entry);
                throw new BadRequestException("Too many invalid OTP attempts. Please request a new one.");
            }
            throw new BadRequestException("Invalid OTP");
        }
        entry.setVerified(true);
        authOtpCodeRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public void assertVerified(String identifier, String otp) {
        assertVerified(AuthOtpCode.PURPOSE_PASSWORD_RESET, identifier, otp);
    }

    @Transactional(readOnly = true)
    public void assertVerified(String purpose, String identifier, String otp) {
        AuthOtpCode entry = loadActiveOrThrow(purpose, identifier);
        if (!Boolean.TRUE.equals(entry.getVerified())) {
            throw new BadRequestException("OTP verification required before resetting password");
        }
        if (LocalDateTime.now().isAfter(entry.getExpiresAt())) {
            throw new BadRequestException("OTP has expired. Please start over.");
        }
        if (!passwordEncoder.matches(otp == null ? "" : otp.trim(), entry.getCodeHash())) {
            throw new BadRequestException("OTP verification required before resetting password");
        }
    }

    @Transactional
    public void clear(String identifier) {
        clear(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier);
    }

    @Transactional
    public void clear(String purpose, String identifier) {
        authOtpCodeRepository.consumeAllActive(purpose, normalize(identifier), LocalDateTime.now());
    }

    private AuthOtpCode loadActiveOrThrow(String purpose, String identifier) {
        return authOtpCodeRepository
                .findLatestActive(purpose, normalize(identifier), LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("No OTP found. Please request a new one."));
    }

    private String normalize(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase() : trimmed;
    }
}
