package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "educations")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnore
    private UserProfile profile;

    @Column(nullable = false, length = 150)
    private String institution;

    @Column(length = 150)
    private String degree;

    @Column(name = "field_of_study", length = 150)
    private String fieldOfStudy;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(name = "education_level", length = 40)
    private String educationLevel;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "currently_studying", nullable = false)
    private Boolean currentlyStudying = false;

    @Column(name = "honors", length = 255)
    private String honors;

    @Column(name = "show_institution_publicly", nullable = false)
    private Boolean showInstitutionPublicly = false;

    @Column(name = "verification_document_id")
    private Long verificationDocumentId;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }

    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }

    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public Boolean getCurrentlyStudying() { return currentlyStudying; }
    public void setCurrentlyStudying(Boolean currentlyStudying) { this.currentlyStudying = currentlyStudying; }

    public String getHonors() { return honors; }
    public void setHonors(String honors) { this.honors = honors; }

    public Boolean getShowInstitutionPublicly() { return showInstitutionPublicly; }
    public void setShowInstitutionPublicly(Boolean showInstitutionPublicly) { this.showInstitutionPublicly = showInstitutionPublicly; }

    public Long getVerificationDocumentId() { return verificationDocumentId; }
    public void setVerificationDocumentId(Long verificationDocumentId) { this.verificationDocumentId = verificationDocumentId; }
}
