package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users_details", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "phone_number")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "name_prefix", length = 20)
    private String namePrefix;

    @Column(name = "middle_name", length = 60)
    private String middleName;

    @Column(name = "name_suffix", length = 20)
    private String nameSuffix;

    @Column(name = "preferred_name", length = 60)
    private String preferredName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "secondary_email", length = 120)
    private String secondaryEmail;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "secondary_phone", length = 20)
    private String secondaryPhone;

    @Column(name = "home_phone", length = 20)
    private String homePhone;

    @Column(name = "preferred_contact_method", length = 20)
    private String preferredContactMethod;

    @Column(name = "best_time_to_contact", length = 40)
    private String bestTimeToContact;

    @Column(nullable = false)
    private String password;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted = false;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "is_profile_published", nullable = false)
    private boolean profilePublished = false;

    @Column(name = "is_18_or_older", nullable = false)
    private boolean is18OrOlder = false;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted = false;

    @Column(name = "privacy_accepted", nullable = false)
    private boolean privacyAccepted = false;

    @Column(name = "identity_consent", nullable = false)
    private boolean identityConsent = false;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent = false;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "user_tokens", nullable = false)
    private int userTokens = 50;

    /** Temporary lock expiry after repeated failed password logins; null when not locked. */
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    /** Permanent disable after repeated lock events (admin reactivation required). */
    @Column(name = "account_disabled", nullable = false)
    private boolean accountDisabled = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile profile;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getNamePrefix() { return namePrefix; }
    public void setNamePrefix(String namePrefix) { this.namePrefix = namePrefix; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getNameSuffix() { return nameSuffix; }
    public void setNameSuffix(String nameSuffix) { this.nameSuffix = nameSuffix; }

    public String getPreferredName() { return preferredName; }
    public void setPreferredName(String preferredName) { this.preferredName = preferredName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSecondaryEmail() { return secondaryEmail; }
    public void setSecondaryEmail(String secondaryEmail) { this.secondaryEmail = secondaryEmail; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSecondaryPhone() { return secondaryPhone; }
    public void setSecondaryPhone(String secondaryPhone) { this.secondaryPhone = secondaryPhone; }

    public String getHomePhone() { return homePhone; }
    public void setHomePhone(String homePhone) { this.homePhone = homePhone; }

    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }

    public String getBestTimeToContact() { return bestTimeToContact; }
    public void setBestTimeToContact(String bestTimeToContact) { this.bestTimeToContact = bestTimeToContact; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isProfilePublished() { return profilePublished; }
    public void setProfilePublished(boolean profilePublished) { this.profilePublished = profilePublished; }

    public boolean is18OrOlder() { return is18OrOlder; }
    public void set18OrOlder(boolean is18OrOlder) { this.is18OrOlder = is18OrOlder; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public boolean isPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }

    public boolean isIdentityConsent() { return identityConsent; }
    public void setIdentityConsent(boolean identityConsent) { this.identityConsent = identityConsent; }

    public boolean isMarketingConsent() { return marketingConsent; }
    public void setMarketingConsent(boolean marketingConsent) { this.marketingConsent = marketingConsent; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public int getUserTokens() { return userTokens; }
    public void setUserTokens(int userTokens) { this.userTokens = userTokens; }

    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }

    public boolean isAccountDisabled() { return accountDisabled; }
    public void setAccountDisabled(boolean accountDisabled) { this.accountDisabled = accountDisabled; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getDisabledAt() { return disabledAt; }
    public void setDisabledAt(LocalDateTime disabledAt) { this.disabledAt = disabledAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }
}