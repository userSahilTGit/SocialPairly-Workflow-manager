package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_identity_background")
public class UserIdentityBackground {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "line1", length = 200)
    private String line1;

    @Column(name = "line2", length = 200)
    private String line2;

    @Column(name = "unit", length = 60)
    private String unit;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state_region", length = 120)
    private String stateRegion;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", columnDefinition = "char(2)")
    private String countryCode;

    @Column(name = "residence_type", length = 40)
    private String residenceType;

    @Column(name = "move_in_month")
    private Integer moveInMonth;

    @Column(name = "move_in_year")
    private Integer moveInYear;

    @Column(name = "willing_to_relocate", length = 40)
    private String willingToRelocate;

    @Column(name = "event_travel_radius_km")
    private Integer eventTravelRadiusKm;

    @Column(name = "country_of_birth", columnDefinition = "char(2)")
    private String countryOfBirth;

    @Column(name = "primary_nationality", columnDefinition = "char(2)")
    private String primaryNationality;

    @Column(name = "country_of_citizenship", columnDefinition = "char(2)")
    private String countryOfCitizenship;

    @Column(name = "current_country_of_residence", columnDefinition = "char(2)")
    private String currentCountryOfResidence;

    @Column(name = "residency_category", length = 40)
    private String residencyCategory;

    @Column(name = "international_relocation_pref", length = 40)
    private String internationalRelocationPref;

    @Column(name = "future_sponsorship_required", length = 40)
    private String futureSponsorshipRequired;

    @Column(name = "open_to_partner_abroad", length = 40)
    private String openToPartnerAbroad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_relocate_locations", columnDefinition = "json")
    private String preferredRelocateLocations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_addresses", columnDefinition = "json")
    private String previousAddresses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_nationalities", columnDefinition = "json")
    private String additionalNationalities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "json")
    private String languages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_future_countries", columnDefinition = "json")
    private String preferredFutureCountries;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }

    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateRegion() { return stateRegion; }
    public void setStateRegion(String stateRegion) { this.stateRegion = stateRegion; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getResidenceType() { return residenceType; }
    public void setResidenceType(String residenceType) { this.residenceType = residenceType; }

    public Integer getMoveInMonth() { return moveInMonth; }
    public void setMoveInMonth(Integer moveInMonth) { this.moveInMonth = moveInMonth; }

    public Integer getMoveInYear() { return moveInYear; }
    public void setMoveInYear(Integer moveInYear) { this.moveInYear = moveInYear; }

    public String getWillingToRelocate() { return willingToRelocate; }
    public void setWillingToRelocate(String willingToRelocate) { this.willingToRelocate = willingToRelocate; }

    public Integer getEventTravelRadiusKm() { return eventTravelRadiusKm; }
    public void setEventTravelRadiusKm(Integer eventTravelRadiusKm) { this.eventTravelRadiusKm = eventTravelRadiusKm; }

    public String getCountryOfBirth() { return countryOfBirth; }
    public void setCountryOfBirth(String countryOfBirth) { this.countryOfBirth = countryOfBirth; }

    public String getPrimaryNationality() { return primaryNationality; }
    public void setPrimaryNationality(String primaryNationality) { this.primaryNationality = primaryNationality; }

    public String getCountryOfCitizenship() { return countryOfCitizenship; }
    public void setCountryOfCitizenship(String countryOfCitizenship) { this.countryOfCitizenship = countryOfCitizenship; }

    public String getCurrentCountryOfResidence() { return currentCountryOfResidence; }
    public void setCurrentCountryOfResidence(String currentCountryOfResidence) {
        this.currentCountryOfResidence = currentCountryOfResidence;
    }

    public String getResidencyCategory() { return residencyCategory; }
    public void setResidencyCategory(String residencyCategory) { this.residencyCategory = residencyCategory; }

    public String getInternationalRelocationPref() { return internationalRelocationPref; }
    public void setInternationalRelocationPref(String internationalRelocationPref) {
        this.internationalRelocationPref = internationalRelocationPref;
    }

    public String getFutureSponsorshipRequired() { return futureSponsorshipRequired; }
    public void setFutureSponsorshipRequired(String futureSponsorshipRequired) {
        this.futureSponsorshipRequired = futureSponsorshipRequired;
    }

    public String getOpenToPartnerAbroad() { return openToPartnerAbroad; }
    public void setOpenToPartnerAbroad(String openToPartnerAbroad) { this.openToPartnerAbroad = openToPartnerAbroad; }

    public String getPreferredRelocateLocations() { return preferredRelocateLocations; }
    public void setPreferredRelocateLocations(String preferredRelocateLocations) {
        this.preferredRelocateLocations = preferredRelocateLocations;
    }

    public String getPreviousAddresses() { return previousAddresses; }
    public void setPreviousAddresses(String previousAddresses) { this.previousAddresses = previousAddresses; }

    public String getAdditionalNationalities() { return additionalNationalities; }
    public void setAdditionalNationalities(String additionalNationalities) {
        this.additionalNationalities = additionalNationalities;
    }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public String getPreferredFutureCountries() { return preferredFutureCountries; }
    public void setPreferredFutureCountries(String preferredFutureCountries) {
        this.preferredFutureCountries = preferredFutureCountries;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
