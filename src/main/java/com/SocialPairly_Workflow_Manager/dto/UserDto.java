package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(
    Long id,
    String firstName,
    String lastName,
    String preferredName,
    String displayName,
    String email,
    String phoneNumber,
    String address,
    Role role,
    boolean profileCompleted,
    boolean verified,
    @JsonProperty("phoneVerified") boolean phoneVerified,
    @JsonProperty("emailVerified") boolean emailVerified,
    @JsonProperty("is18OrOlder") boolean is18OrOlder,
    boolean termsAccepted,
    boolean privacyAccepted,
    boolean identityConsent,
    boolean marketingConsent,
    String onboardingStep,
    boolean identityPage1Complete,
    String subscriptionDetails
) {
    public static UserDto from(User u) {
        UserProfile profile = null;
        try {
            profile = u.getProfile();
        } catch (Exception ignored) {
            // lazy profile unavailable outside session
        }
        return from(u, profile);
    }

    public static UserDto from(User u, UserProfile profile) {
        String preferred = u.getPreferredName();
        String displayName = (preferred != null && !preferred.isBlank())
                ? preferred.trim()
                : u.getFirstName();

        String onboardingStep = profile != null ? profile.getOnboardingStep() : null;
        boolean identityComplete = profile != null
                && OnboardingSteps.isIdentityPage1Complete(
                        profile.getOnboardingStep(),
                        profile.getIdentityPage1CompletedAt());

        return new UserDto(
            u.getId(),
            u.getFirstName(),
            u.getLastName(),
            preferred,
            displayName,
            u.getEmail(),
            u.getPhoneNumber(),
            u.getAddress(),
            u.getRole(),
            u.isProfileCompleted(),
            u.isVerified(),
            u.isPhoneVerified(),
            u.isEmailVerified(),
            u.is18OrOlder(),
            u.isTermsAccepted(),
            u.isPrivacyAccepted(),
            u.isIdentityConsent(),
            u.isMarketingConsent(),
            onboardingStep,
            identityComplete,
            ""
        );
    }

    public static UserDto from(User u, boolean subscribed) {
        UserDto base = from(u);
        return new UserDto(
            base.id(),
            base.firstName(),
            base.lastName(),
            base.preferredName(),
            base.displayName(),
            base.email(),
            base.phoneNumber(),
            base.address(),
            base.role(),
            base.profileCompleted(),
            base.verified(),
            base.phoneVerified(),
            base.emailVerified(),
            base.is18OrOlder(),
            base.termsAccepted(),
            base.privacyAccepted(),
            base.identityConsent(),
            base.marketingConsent(),
            base.onboardingStep(),
            base.identityPage1Complete(),
            subscribed ? "Subscribed" : "Unsubscribed"
        );
    }
}
