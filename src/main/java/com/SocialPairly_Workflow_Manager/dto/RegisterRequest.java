package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 60) String firstName,
    @NotBlank @Size(max = 60) String lastName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 7, max = 20) String phoneNumber,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(min = 6, max = 100) String confirmPassword,
    @Size(max = 255) String address,
    boolean is18OrOlder,
    boolean termsAccepted,
    boolean privacyAccepted,
    boolean identityConsent,
    boolean marketingConsent
) {
    @AssertTrue(message = "Passwords must match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    @AssertTrue(message = "You must confirm you are 18 or older")
    public boolean isAgeConfirmed() {
        return is18OrOlder;
    }

    @AssertTrue(message = "You must accept the Terms of Service")
    public boolean isTermsConfirmed() {
        return termsAccepted;
    }

    @AssertTrue(message = "You must accept the Privacy Policy")
    public boolean isPrivacyConfirmed() {
        return privacyAccepted;
    }

    @AssertTrue(message = "You must consent to identity verification")
    public boolean isIdentityConfirmed() {
        return identityConsent;
    }
}
