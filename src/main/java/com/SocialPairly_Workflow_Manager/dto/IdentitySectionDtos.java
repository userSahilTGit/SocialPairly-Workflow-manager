package com.SocialPairly_Workflow_Manager.dto;

import java.util.List;

/** Nested DTOs for Identity & Background sections 7–20. */
public final class IdentitySectionDtos {

    private IdentitySectionDtos() {}

    public record PreferredFutureLocationDto(
            String state,
            List<String> cities
    ) {}

    public record CurrentResidenceDto(
            String line1,
            String line2,
            String unit,
            String city,
            String stateRegion,
            String postalCode,
            String countryCode,
            String residenceType,
            Integer moveInMonth,
            Integer moveInYear,
            Integer residenceDurationMonths,
            String willingToRelocate,
            Integer eventTravelRadiusMiles,
            List<PreferredFutureLocationDto> preferredFutureLocations
    ) {}

    public record PreviousAddressDto(
            Long id,
            String line1,
            String line2,
            String unit,
            String city,
            String stateRegion,
            String countryCode,
            String postalCode,
            String residenceType,
            Integer fromMonth,
            Integer fromYear,
            Integer toMonth,
            Integer toYear,
            String reasonForMoving
    ) {}

    public record LanguageDto(String languageCode, String proficiency) {}

    public record NationalityDto(
            String countryOfBirth,
            String primaryNationality,
            String countryOfCitizenship,
            List<String> additionalNationalities,
            List<LanguageDto> languages
    ) {}

    public record ImmigrationDto(
            String currentCountryOfResidence,
            String residencyCategory,
            String internationalRelocationPref,
            String futureSponsorshipRequired,
            String openToPartnerAbroad,
            List<String> preferredFutureCountries
    ) {}

    public record RelationshipDto(
            String maritalStatus,
            Integer previousMarriagesCount,
            Integer divorcesCount,
            Integer annulmentsCount,
            Boolean currentlySeparated,
            Boolean divorceFinalized,
            Integer mostRecentDivorceYear,
            String coParenting,
            String unresolvedCommitments,
            String relationshipModelPref
    ) {}

    public record FamilyDto(
            String hasChildren,
            Integer childrenCount,
            List<String> childAgeRanges,
            String childrenLiveWithUser,
            String custodyArrangement,
            String futureChildrenPref,
            String openToPartnerWithChildren,
            Integer preferredFutureChildrenCount,
            String adoptionPref,
            String fosterPref,
            String elderCare,
            String otherDependents,
            String petsInfo
    ) {}

    public record EducationDto(
            Long id,
            String educationLevel,
            String degree,
            String fieldOfStudy,
            String institution,
            String city,
            String countryCode,
            Integer startMonth,
            Integer startYear,
            Integer graduationMonth,
            Integer graduationYear,
            Boolean currentlyStudying,
            String honors,
            Boolean showInstitutionPublicly,
            Long verificationDocumentId
    ) {}

    public record CareerDto(
            String employmentStatus,
            String jobFunction,
            String industry,
            String seniority,
            String companySize,
            String workArrangement,
            String workSchedule,
            String selfEmploymentCategory,
            Integer yearsInProfession,
            String careerSatisfaction,
            String travelFrequency,
            String relocationPossibility,
            String careerAmbitions,
            String workLifeBalancePref,
            String employerName,
            Boolean showEmployerPublicly,
            String employmentType
    ) {}

    public record FinancialDto(
            String incomeRange,
            String creditScoreRange,
            String savingsRange,
            String housingStatus,
            String generalDebtRange,
            String studentLoanRange,
            String financialGoals,
            String savingsHabits,
            String spendingStyle,
            String budgetConsciousness,
            String jointFinancePref,
            String separateFinancePref,
            String householdContributionExpectation,
            String extendedFamilySupportPref
    ) {}

    public record SafetyDto(
            String criminalConviction,
            String pendingCriminalCases,
            String protectiveRestrainingOrder,
            String dvStalkingSexualOffense,
            String governmentOffenderRegistry,
            String jurisdiction,
            Integer approxYear,
            Boolean caseResolved,
            String explanation
    ) {}

    public record CivilJudgmentDto(
            String hasJudgment,
            String categories,
            String jurisdiction,
            Integer approxYear,
            Boolean resolved,
            String explanation
    ) {}

    public record VerificationSummaryDto(
            String provider,
            String sessionId,
            String overallStatus,
            String nameStatus,
            String ageStatus,
            String photoStatus,
            Boolean showVerificationBadge,
            String ssn,
            Long dlFrontDocumentId,
            Long dlBackDocumentId,
            String idDocumentType,
            String idDocumentNumber
    ) {}

    public record BackgroundConsentDto(
            boolean accepted,
            String documentVersion,
            String acceptedAt
    ) {}
}
