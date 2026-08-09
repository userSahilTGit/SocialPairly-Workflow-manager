package com.SocialPairly_Workflow_Manager.dto;

import java.time.LocalDate;
import java.util.List;

import static com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;

public record IdentityBackgroundResponse(
    String namePrefix,
    String firstName,
    String middleName,
    String lastName,
    String nameSuffix,
    String preferredName,
    String displayName,

    LocalDate dateOfBirth,
    Integer age,

    String pronouns,
    String gender,
    String genderShownToMatches,

    String primaryEmail,
    boolean emailVerified,
    String secondaryEmail,
    String primaryPhone,
    boolean phoneVerified,
    String secondaryPhone,
    String homePhone,
    String preferredContactMethod,
    String bestTimeToContact,

    String onboardingStep,
    boolean identityPage1Complete,

    CurrentResidenceDto currentResidence,
    List<PreviousAddressDto> previousAddresses,
    NationalityDto nationality,
    ImmigrationDto immigration,
    RelationshipDto relationship,
    FamilyDto family,
    List<EducationDto> educations,
    CareerDto career,
    FinancialDto financial,
    SafetyDto safety,
    CivilJudgmentDto civilJudgment,
    VerificationSummaryDto verificationSummary,
    BackgroundConsentDto backgroundConsent,
    String incomeRangeSharePreference,
    String locationCity,
    String locationCountry
) {}
