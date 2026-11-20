package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(max = 60, message = "First name is too long (max 60 characters)")
    String firstName,
    @NotBlank(message = "Username is required")
    @Size(max = 60, message = "Last name is too long (max 60 characters)")
    String lastName,
    /** Optional; captured when signup full name has 3+ parts (ADO #1588). */
    @Size(max = 60, message = "Username / full name is too long (middle name max 60 characters)")
    String middleName,
    @NotBlank(message = "Email address is required")
    @Size(max = 120, message = "Email address is too long (max 120 characters)")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Please enter a valid email address (e.g. name@example.com)"
    )
    String email,
    @NotBlank(message = "Phone number is required")
    @Size(min = 7, max = 20, message = "Enter a valid phone number")
    String phoneNumber,
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String password,
    @NotBlank(message = "Confirm password is required")
    @Size(min = 6, max = 100, message = "Confirm password must be between 6 and 100 characters")
    String confirmPassword,
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
