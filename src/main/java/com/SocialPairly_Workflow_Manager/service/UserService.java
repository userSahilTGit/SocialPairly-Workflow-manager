package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.BankDetailsRepository;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserMediaRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.repository.VideoAccessRequestRepository;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserAnswerRepository answerRepository;
    private final VideoAccessRequestRepository videoAccessRequestRepository;
    private final UserMediaRepository userMediaRepository;
    private final BankDetailsRepository bankDetailsRepository;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.sms.default-country-code:+91}")
    private String defaultCountryCode;

    public UserService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       UserAnswerRepository answerRepository,
                       VideoAccessRequestRepository videoAccessRequestRepository,
                       UserMediaRepository userMediaRepository,
                       BankDetailsRepository bankDetailsRepository,
                       RefundRepository refundRepository,
                       PaymentRepository paymentRepository,
                       SubscriptionRepository subscriptionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.answerRepository = answerRepository;
        this.videoAccessRequestRepository = videoAccessRequestRepository;
        this.userMediaRepository = userMediaRepository;
        this.bankDetailsRepository = bankDetailsRepository;
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteAccount(User currentUser, DeleteAccountRequest request) {
        log.info("Deleting account for currentUserId={}", currentUser.getId());

        if (!passwordEncoder.matches(request.password(), currentUser.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }

        if (currentUser.getRole().name().equals("ADMIN")) {
            throw new BadRequestException("Admin accounts cannot be deleted");
        }

        Long userId = currentUser.getId();

        // Delete dependent rows first to avoid FK constraint failures
        videoAccessRequestRepository.deleteByRequesterId(userId);
        videoAccessRequestRepository.deleteByOwnerId(userId);
        userMediaRepository.deleteByUserId(userId);
        bankDetailsRepository.deleteByUserId(userId);
        refundRepository.deleteByUser_Id(userId);
        paymentRepository.deleteByUser_Id(userId);
        subscriptionRepository.deleteByUser_Id(userId);
        answerRepository.deleteAll(answerRepository.findByUserId(userId));
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            currentUser.setProfile(null);
            profileRepository.delete(profile);
        });
        userRepository.delete(currentUser);
    }

    /**
     * Save/change the authenticated user's phone number and mark it unverified.
     * SMS OTP is sent by Firebase on the client.
     */
    @Transactional
    public Map<String, Object> updatePhone(User currentUser, String rawPhone) {
        String phoneStorage = normalizePhoneStorageOrThrow(rawPhone);
        log.info("Updating phone for userId={} to {}", currentUser.getId(), phoneStorage);

        if (phoneNumberExistsForOtherUser(rawPhone, currentUser.getId())) {
            throw new BadRequestException("This phone number is already registered to another account");
        }

        currentUser.setPhoneNumber(phoneStorage);
        currentUser.setPhoneVerified(false);
        userRepository.save(currentUser);

        return Map.of(
                "message", "Phone number saved. Complete Firebase SMS verification to confirm it.",
                "user", UserDto.from(currentUser)
        );
    }

    private String normalizePhoneStorageOrThrow(String raw) {
        try {
            return PhoneNumberNormalizer.toStorageFormat(raw, defaultCountryCode);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid phone number: " + ex.getMessage());
        }
    }

    public User findByIdentifier(String identifier) {
        String trimmed = identifier.trim();
        log.debug("Finding user by identifier={}", trimmed);
        return userRepository.findByEmail(trimmed.toLowerCase())
                .or(() -> findOptionalByPhoneIdentifier(trimmed))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for the given identifier"));
    }

    /** Resolve a phone login / forgot-password identifier to a user (storage + legacy E.164). */
    public java.util.Optional<User> findOptionalByPhoneIdentifier(String identifier) {
        for (String key : PhoneNumberNormalizer.lookupKeys(identifier, defaultCountryCode)) {
            java.util.Optional<User> found = userRepository.findByPhoneNumber(key);
            if (found.isPresent()) {
                return found;
            }
        }
        return java.util.Optional.empty();
    }

    private boolean phoneNumberExistsForOtherUser(String raw, Long userId) {
        return PhoneNumberNormalizer.lookupKeys(raw, defaultCountryCode).stream()
                .anyMatch(key -> userRepository.existsByPhoneNumberAndIdNot(key, userId));
    }
}
