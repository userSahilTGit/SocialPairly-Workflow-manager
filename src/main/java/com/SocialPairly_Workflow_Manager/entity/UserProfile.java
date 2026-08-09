package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user_profiles_details")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Lob
    @Column(name = "about_me")
    private String aboutMe;

    @Column(length = 120)
    private String occupation;

    @Lob
    private String lifestyle;

    @Column(name = "location_city", length = 120)
    private String locationCity;

    @Column(name = "location_country", length = 120)
    private String locationCountry;

    private Double latitude;
    private Double longitude;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String gender;

    @Column(name = "religion", length = 60)
    private String religion;

    @Column(name = "preferred_religion", length = 60)
    private String preferredReligion;

    @Column(name = "pronouns", length = 40)
    private String pronouns;

    @Column(name = "gender_shown_to_matches", length = 20)
    private String genderShownToMatches = "MATCHES";

    @Column(name = "identity_verification_status", length = 50)
    private String identityVerificationStatus = "PENDING";

    @Column(name = "background_screening_status", length = 50)
    private String backgroundScreeningStatus = "NOT_STARTED";

    @Column(name = "onboarding_step", length = 50)
    private String onboardingStep = "STEP_1_ACCOUNT";

    @Column(name = "identity_page1_completed_at")
    private LocalDateTime identityPage1CompletedAt;

    @Column(name = "identity_name_verification_status", length = 30)
    private String identityNameVerificationStatus = "NOT_STARTED";

    @Column(name = "identity_age_verification_status", length = 30)
    private String identityAgeVerificationStatus = "NOT_STARTED";

    @Column(name = "identity_photo_verification_status", length = 30)
    private String identityPhotoVerificationStatus = "NOT_STARTED";

    @Column(name = "show_verification_badge", nullable = false)
    private Boolean showVerificationBadge = false;

    @Column(name = "employer_name_publicly_allowed", nullable = false)
    private Boolean employerNamePubliclyAllowed = false;

    @Column(name = "income_range_share_preference", length = 20)
    private String incomeRangeSharePreference = "PRIVATE";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interests", columnDefinition = "json")
    private Set<String> interests = new HashSet<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Education> educations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getPreferredReligion() { return preferredReligion; }
    public void setPreferredReligion(String preferredReligion) { this.preferredReligion = preferredReligion; }

    public String getPronouns() { return pronouns; }
    public void setPronouns(String pronouns) { this.pronouns = pronouns; }

    public String getGenderShownToMatches() { return genderShownToMatches; }
    public void setGenderShownToMatches(String genderShownToMatches) { this.genderShownToMatches = genderShownToMatches; }

    public String getIdentityVerificationStatus() { return identityVerificationStatus; }
    public void setIdentityVerificationStatus(String identityVerificationStatus) { this.identityVerificationStatus = identityVerificationStatus; }

    public String getBackgroundScreeningStatus() { return backgroundScreeningStatus; }
    public void setBackgroundScreeningStatus(String backgroundScreeningStatus) { this.backgroundScreeningStatus = backgroundScreeningStatus; }

    public String getOnboardingStep() { return onboardingStep; }
    public void setOnboardingStep(String onboardingStep) { this.onboardingStep = onboardingStep; }

    public LocalDateTime getIdentityPage1CompletedAt() { return identityPage1CompletedAt; }
    public void setIdentityPage1CompletedAt(LocalDateTime identityPage1CompletedAt) { this.identityPage1CompletedAt = identityPage1CompletedAt; }

    public String getIdentityNameVerificationStatus() { return identityNameVerificationStatus; }
    public void setIdentityNameVerificationStatus(String identityNameVerificationStatus) { this.identityNameVerificationStatus = identityNameVerificationStatus; }

    public String getIdentityAgeVerificationStatus() { return identityAgeVerificationStatus; }
    public void setIdentityAgeVerificationStatus(String identityAgeVerificationStatus) { this.identityAgeVerificationStatus = identityAgeVerificationStatus; }

    public String getIdentityPhotoVerificationStatus() { return identityPhotoVerificationStatus; }
    public void setIdentityPhotoVerificationStatus(String identityPhotoVerificationStatus) { this.identityPhotoVerificationStatus = identityPhotoVerificationStatus; }

    public Boolean getShowVerificationBadge() { return showVerificationBadge; }
    public void setShowVerificationBadge(Boolean showVerificationBadge) { this.showVerificationBadge = showVerificationBadge; }

    public Boolean getEmployerNamePubliclyAllowed() { return employerNamePubliclyAllowed; }
    public void setEmployerNamePubliclyAllowed(Boolean employerNamePubliclyAllowed) { this.employerNamePubliclyAllowed = employerNamePubliclyAllowed; }

    public String getIncomeRangeSharePreference() { return incomeRangeSharePreference; }
    public void setIncomeRangeSharePreference(String incomeRangeSharePreference) { this.incomeRangeSharePreference = incomeRangeSharePreference; }

    public Set<String> getInterests() {
        return interests == null ? new HashSet<>() : interests;
    }
    public void setInterests(Set<String> interests) {
        this.interests = interests == null ? new HashSet<>() : interests;
    }

    public List<Education> getEducations() { return educations; }
    public void setEducations(List<Education> educations) { this.educations = educations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}