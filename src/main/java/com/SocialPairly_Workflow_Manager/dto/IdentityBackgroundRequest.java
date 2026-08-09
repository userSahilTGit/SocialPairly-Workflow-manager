package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

import static com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;

public record IdentityBackgroundRequest(
    String action,

    @Size(max = 20) String namePrefix,
    @Size(max = 60) String firstName,
    @Size(max = 60) String middleName,
    @Size(max = 60) String lastName,
    @Size(max = 20) String nameSuffix,
    @Size(max = 60) String preferredName,

    @Past LocalDate dateOfBirth,

    @Size(max = 40) String pronouns,
    @Size(max = 30) String gender,
    @Size(max = 20) String genderShownToMatches,

    @Email @Size(max = 120) String secondaryEmail,
    @Size(max = 20) String primaryPhone,
    @Size(max = 20) String secondaryPhone,
    @Size(max = 20) String homePhone,
    @Size(max = 20) String preferredContactMethod,
    @Size(max = 40) String bestTimeToContact,

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
    String incomeRangeSharePreference,
    @Size(max = 11) String ssn
) {}
