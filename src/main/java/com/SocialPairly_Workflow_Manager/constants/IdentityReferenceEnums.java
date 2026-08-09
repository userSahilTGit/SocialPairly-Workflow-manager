package com.SocialPairly_Workflow_Manager.constants;

import java.util.List;
import java.util.Map;

/** Release-1 allowlists for Identity & Background sections. */
public final class IdentityReferenceEnums {

    private IdentityReferenceEnums() {}

    public static final List<String> RESIDENCE_TYPES = List.of(
            "OWN", "RENT", "FAMILY", "SHARED", "OTHER", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> RELOCATE_PREFS = List.of(
            "YES", "NO", "MAYBE", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> MARITAL_STATUSES = List.of(
            "NEVER_MARRIED", "SINGLE", "DIVORCED", "WIDOWED", "SEPARATED", "MARRIED", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> RESIDENCY_CATEGORIES = List.of(
            "CITIZEN", "PERMANENT_RESIDENT", "WORK_AUTHORIZED", "STUDENT",
            "TEMPORARY_RESIDENT", "OTHER", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> YES_NO_PREFER = List.of(
            "YES", "NO", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> SAFETY_ANSWERS = List.of(
            "NO", "YES", "PREFER_PRIVATE_DISCUSSION"
    );

    public static final List<String> CHILD_AGE_RANGES = List.of(
            "0_2", "3_5", "6_9", "10_12", "13_17", "18_PLUS"
    );

    public static final List<String> EDUCATION_LEVELS = List.of(
            "HIGH_SCHOOL", "ASSOCIATE", "BACHELOR", "MASTER", "DOCTORATE", "PROFESSIONAL", "OTHER"
    );

    public static final List<String> EMPLOYMENT_STATUSES = List.of(
            "EMPLOYEE", "SELF_EMPLOYED", "BUSINESS_OWNER", "CONTRACTOR",
            "STUDENT", "RETIRED", "UNEMPLOYED", "OTHER", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> INCOME_RANGES = List.of(
            "UNDER_25K", "25K_49K", "50K_74K", "75K_99K", "100K_149K", "150K_PLUS", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> CREDIT_SCORE_RANGES = List.of(
            "BELOW_580", "580_669", "670_739", "740_799", "800_PLUS", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> SAVINGS_RANGES = List.of(
            "UNDER_5K", "5K_24K", "25K_49K", "50K_99K", "100K_PLUS", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> HOUSING_STATUSES = List.of(
            "HOMEOWNER", "RENTER", "OTHER", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> DEBT_RANGES = List.of(
            "NONE", "UNDER_10K", "10K_49K", "50K_99K", "100K_PLUS", "PREFER_NOT_TO_SAY"
    );

    public static final List<String> SHARE_PREFERENCES = List.of(
            "PRIVATE", "MATCHES", "PUBLIC"
    );

    public static final List<Map<String, String>> LANGUAGE_OPTIONS = List.of(
            Map.of("code", "en", "name", "English"),
            Map.of("code", "ar", "name", "Arabic"),
            Map.of("code", "hi", "name", "Hindi"),
            Map.of("code", "fr", "name", "French"),
            Map.of("code", "es", "name", "Spanish"),
            Map.of("code", "ur", "name", "Urdu"),
            Map.of("code", "zh", "name", "Chinese"),
            Map.of("code", "other", "name", "Other")
    );

    public static Map<String, Object> asReferencePayload(List<Map<String, String>> countries) {
        return Map.ofEntries(
                Map.entry("countries", countries),
                Map.entry("residenceTypes", RESIDENCE_TYPES),
                Map.entry("relocatePrefs", RELOCATE_PREFS),
                Map.entry("maritalStatuses", MARITAL_STATUSES),
                Map.entry("residencyCategories", RESIDENCY_CATEGORIES),
                Map.entry("yesNoPrefer", YES_NO_PREFER),
                Map.entry("safetyAnswers", SAFETY_ANSWERS),
                Map.entry("childAgeRanges", CHILD_AGE_RANGES),
                Map.entry("educationLevels", EDUCATION_LEVELS),
                Map.entry("employmentStatuses", EMPLOYMENT_STATUSES),
                Map.entry("incomeRanges", INCOME_RANGES),
                Map.entry("creditScoreRanges", CREDIT_SCORE_RANGES),
                Map.entry("savingsRanges", SAVINGS_RANGES),
                Map.entry("housingStatuses", HOUSING_STATUSES),
                Map.entry("debtRanges", DEBT_RANGES),
                Map.entry("sharePreferences", SHARE_PREFERENCES),
                Map.entry("languages", LANGUAGE_OPTIONS),
                Map.entry("backgroundConsentDocumentVersion", OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION)
        );
    }
}
