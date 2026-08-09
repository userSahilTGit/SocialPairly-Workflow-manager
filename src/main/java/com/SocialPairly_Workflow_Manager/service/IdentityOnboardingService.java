package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.IdentityReferenceEnums;
import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundResponse;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.exception.UnprocessableEntityException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IdentityOnboardingService {

    public static final String ACTION_SAVE_LATER = "SAVE_LATER";
    public static final String ACTION_CONTINUE = "CONTINUE";
    public static final int MIN_AGE = 18;
    public static final long MAX_DOCUMENT_BYTES = 5L * 1024 * 1024;
    private static final int MAX_AUDIT_EVENTS = 200;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}][\\p{L} .'-]{0,59}$");
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    private static final Set<String> DOCUMENT_PURPOSES = Set.of(
            "DL_FRONT", "DL_BACK", "SELFIE",
            "SAFETY_SUPPORT", "CIVIL_SUPPORT", "EDU_VERIFY", "EMP_VERIFY",
            "ID_FRONT", "ID_BACK"
    );
    private static final Set<String> DL_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> PREFIXES = Set.of("", "Mr.", "Ms.", "Mrs.", "Dr.", "Other");
    private static final Set<String> SUFFIXES = Set.of("", "None", "Jr.", "Sr.", "II", "III");
    private static final Set<String> PRONOUNS = Set.of(
            "", "She/Her", "He/Him", "They/Them", "She/They", "He/They", "Prefer not to say", "Self-describe"
    );
    private static final Set<String> GENDERS = Set.of(
            "", "Woman", "Man", "Non-binary", "Self-describe", "Prefer not to say",
            "Female", "Male", "Other"
    );
    private static final Set<String> GENDER_VISIBILITY = Set.of("HIDDEN", "MATCHES", "PUBLIC");
    private static final Set<String> CONTACT_METHODS = Set.of("", "EMAIL", "SMS", "PHONE", "ANY");
    private static final Set<String> BEST_TIMES = Set.of("", "MORNING", "AFTERNOON", "EVENING", "ANY");

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RefCountryRepository refCountryRepository;
    private final UserIdentityBackgroundRepository backgroundRepository;
    private final UserLifeProfileRepository lifeProfileRepository;
    private final UserIdentityComplianceRepository complianceRepository;
    private final UserIdentityComplianceBlobRepository blobRepository;
    private final ObjectMapper objectMapper;
    private final String defaultCountryCode;

    public IdentityOnboardingService(
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            RefCountryRepository refCountryRepository,
            UserIdentityBackgroundRepository backgroundRepository,
            UserLifeProfileRepository lifeProfileRepository,
            UserIdentityComplianceRepository complianceRepository,
            UserIdentityComplianceBlobRepository blobRepository,
            ObjectMapper objectMapper,
            @Value("${app.sms.default-country-code:+91}") String defaultCountryCode
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refCountryRepository = refCountryRepository;
        this.backgroundRepository = backgroundRepository;
        this.lifeProfileRepository = lifeProfileRepository;
        this.complianceRepository = complianceRepository;
        this.blobRepository = blobRepository;
        this.objectMapper = objectMapper;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Transactional(readOnly = true)
    public IdentityBackgroundResponse getIdentity(User user) {
        UserProfile profile = ensureProfile(user);
        return toResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReferenceData() {
        List<Map<String, String>> countries = refCountryRepository.findByActiveTrue().stream()
                .map(c -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("code", c.getCode());
                    m.put("name", c.getName());
                    if (c.getPostalRegex() != null) {
                        m.put("postalRegex", c.getPostalRegex());
                    }
                    return m;
                })
                .toList();
        return IdentityReferenceEnums.asReferencePayload(countries);
    }

    @Transactional
    public IdentityBackgroundResponse saveIdentity(User user, IdentityBackgroundRequest request) {
        String action = request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);
        if (!ACTION_SAVE_LATER.equals(action) && !ACTION_CONTINUE.equals(action)) {
            throw new BadRequestException("action must be SAVE_LATER or CONTINUE");
        }

        requireBackgroundConsent(user);

        boolean strict = ACTION_CONTINUE.equals(action);
        validateAndApply(user, request, strict);

        UserProfile profile = ensureProfile(user);
        applyProfileFields(profile, request, strict);
        applySsn(user, request.ssn());

        if (request.currentResidence() != null) {
            applyCurrentResidence(user, profile, request.currentResidence());
        }
        if (request.previousAddresses() != null) {
            applyPreviousAddresses(user, request.previousAddresses());
        }
        if (request.nationality() != null) {
            applyNationality(user, request.nationality());
        }
        if (request.immigration() != null) {
            applyImmigration(user, request.immigration());
        }
        if (request.relationship() != null) {
            applyRelationship(user, request.relationship());
        }
        if (request.family() != null) {
            applyFamily(user, request.family());
        }
        if (request.educations() != null) {
            applyEducations(profile, request.educations());
        }
        if (request.career() != null) {
            applyCareer(user, profile, request.career());
        }
        if (request.financial() != null) {
            applyFinancial(user, request.financial());
        }
        if (request.safety() != null) {
            applySafety(user, request.safety());
        }
        if (request.civilJudgment() != null) {
            applyCivilJudgment(user, request.civilJudgment());
        }
        if (request.incomeRangeSharePreference() != null) {
            String pref = request.incomeRangeSharePreference().trim().toUpperCase(Locale.ROOT);
            validateAllowlistEnum("incomeRangeSharePreference", pref, IdentityReferenceEnums.SHARE_PREFERENCES);
            profile.setIncomeRangeSharePreference(pref);
        }

        if (strict) {
            requireContinueSections(user);
            profile.setOnboardingStep(OnboardingSteps.IDENTITY_COMPLETED);
            profile.setIdentityPage1CompletedAt(LocalDateTime.now());
            profile.setOnboardingStep(OnboardingSteps.PERSONALITY_IN_PROGRESS);
        } else if (!OnboardingSteps.isIdentityPage1Complete(
                profile.getOnboardingStep(), profile.getIdentityPage1CompletedAt())) {
            profile.setOnboardingStep(OnboardingSteps.IDENTITY_IN_PROGRESS);
        }

        userRepository.save(user);
        profileRepository.save(profile);
        writeAudit(user, "IDENTITY_SAVE", action);

        return toResponse(user, profile);
    }

    @Transactional
    public Map<String, Object> uploadDocument(User user, MultipartFile file, String docPurpose) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new BadRequestException("file exceeds maximum size of 5MB");
        }
        String purpose = trimToNull(docPurpose);
        if (purpose == null) {
            throw new BadRequestException("docPurpose is required");
        }
        purpose = purpose.trim().toUpperCase(Locale.ROOT);
        if (!DOCUMENT_PURPOSES.contains(purpose)) {
            throw new BadRequestException("Unsupported docPurpose");
        }
        if ("ID_FRONT".equals(purpose)) {
            purpose = "DL_FRONT";
        } else if ("ID_BACK".equals(purpose)) {
            purpose = "DL_BACK";
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        } else {
            contentType = contentType.trim().toLowerCase(Locale.ROOT);
        }
        if (("DL_FRONT".equals(purpose) || "DL_BACK".equals(purpose) || "SELFIE".equals(purpose))
                && !DL_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("DL and selfie uploads must be image/jpeg, image/png, or image/webp");
        }

        try {
            UserIdentityCompliance compliance = ensureCompliance(user);
            UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
            blob.setCompliance(compliance);
            blob.setDocPurpose(purpose);
            blob.setContentType(contentType);
            blob.setFileSizeKb((int) Math.ceil(file.getSize() / 1024.0));
            blob.setStorageBlob(file.getBytes());
            UserIdentityComplianceBlob saved = blobRepository.save(blob);

            appendDocumentMeta(compliance, saved);
            if ("DL_FRONT".equals(purpose)) {
                compliance.setDlFrontBlobId(saved.getId());
            } else if ("DL_BACK".equals(purpose)) {
                compliance.setDlBackBlobId(saved.getId());
            }
            complianceRepository.save(compliance);

            writeAudit(user, "DOCUMENT_UPLOAD", purpose + ":" + saved.getId());
            return Map.of(
                    "id", saved.getId(),
                    "docPurpose", saved.getDocPurpose(),
                    "contentType", saved.getContentType(),
                    "fileSizeKb", saved.getFileSizeKb()
            );
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded file");
        }
    }

    @Transactional
    public DocumentStreamResult streamDocument(User requester, Long documentId) {
        if (documentId == null) {
            throw new BadRequestException("document id is required");
        }
        UserIdentityComplianceBlob blob = blobRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        User owner = blob.getCompliance().getUser();
        boolean isOwner = owner.getId().equals(requester.getId());
        boolean admin = requester.getRole() == Role.ADMIN;
        if (!isOwner && !admin) {
            throw new ResourceNotFoundException("Document not found");
        }
        if (blob.getStorageBlob() == null || blob.getStorageBlob().length == 0) {
            throw new ResourceNotFoundException("Document content not available");
        }

        String eventType = admin && !isOwner ? "ADMIN_SAFETY_VIEW" : "DOCUMENT_VIEW";
        writeAudit(owner, requester, eventType, "doc:" + documentId);

        String contentType = blob.getContentType() == null || blob.getContentType().isBlank()
                ? "application/octet-stream"
                : blob.getContentType();
        return new DocumentStreamResult(contentType, blob.getStorageBlob());
    }

    public record DocumentStreamResult(String contentType, byte[] bytes) {}

    @Transactional
    public Map<String, Object> startVerification(User user) {
        UserIdentityCompliance compliance = ensureCompliance(user);

        if (compliance.getSessionId() == null || compliance.getSessionId().isBlank()) {
            compliance.setSessionId(UUID.randomUUID().toString());
        }
        if ("NOT_STARTED".equals(compliance.getOverallStatus())) {
            compliance.setOverallStatus("IN_PROGRESS");
        }
        compliance.setProvider("INTERNAL_V1");
        UserIdentityCompliance saved = complianceRepository.save(compliance);
        writeAudit(user, "VERIFY_STATUS_CHANGE", "start:" + saved.getOverallStatus());

        return Map.of(
                "provider", saved.getProvider(),
                "sessionId", saved.getSessionId(),
                "overallStatus", saved.getOverallStatus(),
                "nameStatus", saved.getNameStatus(),
                "ageStatus", saved.getAgeStatus(),
                "photoStatus", saved.getPhotoStatus()
        );
    }

    @Transactional
    public Map<String, Object> submitSelfie(User user, Long documentId) {
        UserIdentityCompliance compliance = complianceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Verification session not started"));

        if (documentId != null) {
            UserIdentityComplianceBlob blob = blobRepository.findById(documentId)
                    .orElseThrow(() -> new BadRequestException("documentId not found"));
            if (!blob.getCompliance().getUser().getId().equals(user.getId())) {
                throw new BadRequestException("documentId does not belong to current user");
            }
            compliance.setPhotoStatus("VERIFIED");
        } else {
            compliance.setPhotoStatus("IN_PROGRESS");
        }
        if ("NOT_STARTED".equals(compliance.getOverallStatus())) {
            compliance.setOverallStatus("IN_PROGRESS");
        }
        UserIdentityCompliance saved = complianceRepository.save(compliance);

        UserProfile profile = ensureProfile(user);
        profile.setIdentityPhotoVerificationStatus(saved.getPhotoStatus());
        if ("VERIFIED".equals(saved.getPhotoStatus())) {
            profile.setShowVerificationBadge(true);
        }
        profileRepository.save(profile);
        writeAudit(user, "VERIFY_STATUS_CHANGE", "selfie:" + saved.getPhotoStatus());

        return Map.of(
                "sessionId", saved.getSessionId() == null ? "" : saved.getSessionId(),
                "photoStatus", saved.getPhotoStatus(),
                "overallStatus", saved.getOverallStatus()
        );
    }

    @Transactional
    public Map<String, Object> handleVerificationWebhook(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BadRequestException("webhook body is required");
        }
        Object sessionRaw = body.get("sessionId");
        if (sessionRaw == null) {
            sessionRaw = body.get("session_id");
        }
        if (sessionRaw == null || sessionRaw.toString().isBlank()) {
            throw new BadRequestException("sessionId is required");
        }
        String sessionId = sessionRaw.toString().trim();
        UserIdentityCompliance compliance = complianceRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BadRequestException("Unknown verification session"));

        Object statusRaw = body.get("status");
        if (statusRaw == null) {
            statusRaw = body.get("overallStatus");
        }
        if (statusRaw != null && !statusRaw.toString().isBlank()) {
            compliance.setOverallStatus(statusRaw.toString().trim().toUpperCase(Locale.ROOT));
        }
        if (body.get("photoStatus") != null) {
            compliance.setPhotoStatus(body.get("photoStatus").toString().trim().toUpperCase(Locale.ROOT));
        }
        if (body.get("nameStatus") != null) {
            compliance.setNameStatus(body.get("nameStatus").toString().trim().toUpperCase(Locale.ROOT));
        }
        if (body.get("ageStatus") != null) {
            compliance.setAgeStatus(body.get("ageStatus").toString().trim().toUpperCase(Locale.ROOT));
        }
        UserIdentityCompliance saved = complianceRepository.save(compliance);
        writeAudit(compliance.getUser(), "VERIFY_STATUS_CHANGE", "webhook:" + saved.getOverallStatus());

        return Map.of(
                "sessionId", saved.getSessionId(),
                "overallStatus", saved.getOverallStatus(),
                "photoStatus", saved.getPhotoStatus()
        );
    }

    @Transactional
    public Map<String, Object> acceptBackgroundConsent(User user, Boolean accepted, String documentVersion) {
        if (!Boolean.TRUE.equals(accepted)) {
            throw new BadRequestException("accepted must be true");
        }
        String version = trimToNull(documentVersion);
        if (version == null || !OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION.equals(version)) {
            throw new BadRequestException("Unsupported or missing documentVersion");
        }

        UserIdentityCompliance compliance = ensureCompliance(user);
        LocalDateTime acceptedAt = LocalDateTime.now();
        Map<String, Object> consent = new LinkedHashMap<>();
        consent.put("documentVersion", version);
        consent.put("accepted", true);
        consent.put("acceptedAt", acceptedAt.toString());
        consent.put("acceptedIp", null);
        consent.put("userAgentHash", null);
        consent.put("createdAt", acceptedAt.toString());
        appendJsonArray(compliance, "consents", compliance.getConsents(), consent, Integer.MAX_VALUE);
        complianceRepository.save(compliance);
        writeAudit(user, "CONSENT_ACCEPTED", version);

        return Map.of(
                "accepted", true,
                "documentVersion", version,
                "acceptedAt", acceptedAt.toString()
        );
    }

    private void requireContinueSections(User user) {
        UserIdentityBackground background = backgroundRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("currentResidence is required"));
        if (trimToNull(background.getLine1()) == null
                || trimToNull(background.getCity()) == null
                || trimToNull(background.getStateRegion()) == null
                || trimToNull(background.getPostalCode()) == null
                || trimToNull(background.getCountryCode()) == null) {
            throw new BadRequestException(
                    "currentResidence requires line1, city, stateRegion, postalCode, and countryCode");
        }

        UserLifeProfile life = lifeProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("relationship.maritalStatus is required"));
        if (trimToNull(life.getMaritalStatus()) == null) {
            throw new BadRequestException("relationship.maritalStatus is required");
        }
    }

    private void requireBackgroundConsent(User user) {
        if (!hasAcceptedConsent(user)) {
            throw new BadRequestException("Background screening consent is required before saving");
        }
    }

    private boolean hasAcceptedConsent(User user) {
        return complianceRepository.findByUserId(user.getId())
                .map(c -> readConsentList(c.getConsents()).stream()
                        .anyMatch(entry -> Boolean.TRUE.equals(asBoolean(entry.get("accepted")))
                                && OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION
                                .equals(asString(entry.get("documentVersion")))))
                .orElse(false);
    }

    private void applySsn(User user, String rawSsn) {
        String trimmed = trimToNull(rawSsn);
        if (trimmed == null) {
            return;
        }
        if (trimmed.contains("*")) {
            return;
        }
        String normalized = normalizeSsn(trimmed);
        UserIdentityCompliance compliance = ensureCompliance(user);
        compliance.setSsn(normalized);
        complianceRepository.save(compliance);
    }

    static String normalizeSsn(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 9) {
            throw new BadRequestException("SSN must be in XXX-XX-XXXX format");
        }
        String formatted = digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
        if (!SSN_PATTERN.matcher(formatted).matches()) {
            throw new BadRequestException("SSN must be in XXX-XX-XXXX format");
        }
        return formatted;
    }

    static String maskSsn(String ssn) {
        if (ssn == null || ssn.length() < 4) {
            return null;
        }
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }

    private UserIdentityBackground ensureBackground(User user) {
        return backgroundRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserIdentityBackground bg = new UserIdentityBackground();
                    bg.setUser(user);
                    return backgroundRepository.save(bg);
                });
    }

    private UserLifeProfile ensureLife(User user) {
        return lifeProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserLifeProfile life = new UserLifeProfile();
                    life.setUser(user);
                    return lifeProfileRepository.save(life);
                });
    }

    private UserIdentityCompliance ensureCompliance(User user) {
        return complianceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserIdentityCompliance c = new UserIdentityCompliance();
                    c.setUser(user);
                    c.setProvider("INTERNAL_V1");
                    c.setOverallStatus("NOT_STARTED");
                    c.setNameStatus("NOT_STARTED");
                    c.setAgeStatus("NOT_STARTED");
                    c.setPhotoStatus("NOT_STARTED");
                    return complianceRepository.save(c);
                });
    }

    private void applyCurrentResidence(User user, UserProfile profile, CurrentResidenceDto dto) {
        validateMonth("moveInMonth", dto.moveInMonth());
        if (dto.residenceType() != null && !dto.residenceType().isBlank()) {
            validateAllowlistEnum("residenceType", dto.residenceType().trim().toUpperCase(Locale.ROOT),
                    IdentityReferenceEnums.RESIDENCE_TYPES);
        }
        if (dto.willingToRelocate() != null && !dto.willingToRelocate().isBlank()) {
            validateAllowlistEnum("willingToRelocate", dto.willingToRelocate().trim().toUpperCase(Locale.ROOT),
                    IdentityReferenceEnums.RELOCATE_PREFS);
        }

        String countryCode = normalizeCountryCode(dto.countryCode());
        String postalCode = trimToNull(dto.postalCode());
        validatePostalCode(countryCode, postalCode);

        UserIdentityBackground background = ensureBackground(user);
        background.setLine1(trimToNull(dto.line1()));
        background.setLine2(trimToNull(dto.line2()));
        background.setUnit(trimToNull(dto.unit()));
        background.setCity(trimToNull(dto.city()));
        background.setStateRegion(trimToNull(dto.stateRegion()));
        background.setPostalCode(postalCode);
        background.setCountryCode(countryCode);
        if (dto.residenceType() != null) {
            background.setResidenceType(blankToNull(dto.residenceType().trim().toUpperCase(Locale.ROOT)));
        }
        background.setMoveInMonth(dto.moveInMonth());
        background.setMoveInYear(dto.moveInYear());
        if (dto.willingToRelocate() != null) {
            background.setWillingToRelocate(blankToNull(dto.willingToRelocate().trim().toUpperCase(Locale.ROOT)));
        }
        background.setEventTravelRadiusKm(dto.eventTravelRadiusKm());

        List<String> preferred = new ArrayList<>();
        if (dto.preferredFutureLocations() != null) {
            for (String label : dto.preferredFutureLocations()) {
                String trimmed = trimToNull(label);
                if (trimmed != null) {
                    preferred.add(trimmed);
                }
            }
        }
        background.setPreferredRelocateLocations(writeJson(preferred));
        backgroundRepository.save(background);

        profile.setLocationCity(background.getCity());
        profile.setLocationCountry(resolveCountryName(background.getCountryCode()));
        user.setAddress(buildPrivateAddressSummary(background));
    }

    private void applyPreviousAddresses(User user, List<PreviousAddressDto> addresses) {
        UserIdentityBackground background = ensureBackground(user);
        if (addresses.isEmpty()) {
            background.setPreviousAddresses(writeJson(List.of()));
            backgroundRepository.save(background);
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PreviousAddressDto dto : addresses) {
            validateMonth("fromMonth", dto.fromMonth());
            validateMonth("toMonth", dto.toMonth());
            validateYearRange("previous address", dto.fromMonth(), dto.fromYear(), dto.toMonth(), dto.toYear());
            String countryCode = normalizeCountryCode(dto.countryCode());
            validatePostalCode(countryCode, trimToNull(dto.postalCode()));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("city", trimToNull(dto.city()));
            row.put("stateRegion", trimToNull(dto.stateRegion()));
            row.put("countryCode", countryCode);
            row.put("postalCode", trimToNull(dto.postalCode()));
            row.put("fromMonth", dto.fromMonth());
            row.put("fromYear", dto.fromYear());
            row.put("toMonth", dto.toMonth());
            row.put("toYear", dto.toYear());
            row.put("reasonForMoving", trimToNull(dto.reasonForMoving()));
            rows.add(row);
        }
        background.setPreviousAddresses(writeJson(rows));
        backgroundRepository.save(background);
    }

    private void applyNationality(User user, NationalityDto dto) {
        String primary = normalizeCountryCode(dto.primaryNationality());
        Set<String> additional = new LinkedHashSet<>();
        if (dto.additionalNationalities() != null) {
            for (String code : dto.additionalNationalities()) {
                String normalized = normalizeCountryCode(code);
                if (normalized == null) continue;
                if (!additional.add(normalized)) {
                    throw new BadRequestException("Duplicate nationality codes are not allowed");
                }
            }
        }
        if (primary != null && additional.contains(primary)) {
            throw new BadRequestException("Duplicate nationality codes are not allowed");
        }

        UserIdentityBackground background = ensureBackground(user);
        background.setCountryOfBirth(normalizeCountryCode(dto.countryOfBirth()));
        background.setPrimaryNationality(primary);
        background.setCountryOfCitizenship(normalizeCountryCode(dto.countryOfCitizenship()));
        background.setAdditionalNationalities(writeJson(new ArrayList<>(additional)));

        List<Map<String, Object>> languages = new ArrayList<>();
        if (dto.languages() != null) {
            for (LanguageDto lang : dto.languages()) {
                String code = trimToNull(lang.languageCode());
                if (code == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("languageCode", code.toLowerCase(Locale.ROOT));
                row.put("proficiency", trimToNull(lang.proficiency()));
                languages.add(row);
            }
        }
        background.setLanguages(writeJson(languages));
        backgroundRepository.save(background);
    }

    private void applyImmigration(User user, ImmigrationDto dto) {
        if (dto.residencyCategory() != null && !dto.residencyCategory().isBlank()) {
            validateAllowlistEnum("residencyCategory", dto.residencyCategory().trim().toUpperCase(Locale.ROOT),
                    IdentityReferenceEnums.RESIDENCY_CATEGORIES);
        }
        validateOptionalYesNoPrefer("internationalRelocationPref", dto.internationalRelocationPref());
        validateOptionalYesNoPrefer("futureSponsorshipRequired", dto.futureSponsorshipRequired());
        validateOptionalYesNoPrefer("openToPartnerAbroad", dto.openToPartnerAbroad());

        UserIdentityBackground background = ensureBackground(user);
        background.setCurrentCountryOfResidence(normalizeCountryCode(dto.currentCountryOfResidence()));
        if (dto.residencyCategory() != null) {
            background.setResidencyCategory(blankToNull(dto.residencyCategory().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.internationalRelocationPref() != null) {
            background.setInternationalRelocationPref(
                    blankToNull(dto.internationalRelocationPref().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.futureSponsorshipRequired() != null) {
            background.setFutureSponsorshipRequired(
                    blankToNull(dto.futureSponsorshipRequired().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.openToPartnerAbroad() != null) {
            background.setOpenToPartnerAbroad(blankToNull(dto.openToPartnerAbroad().trim().toUpperCase(Locale.ROOT)));
        }
        List<String> preferred = new ArrayList<>();
        if (dto.preferredFutureCountries() != null) {
            for (String code : dto.preferredFutureCountries()) {
                String normalized = normalizeCountryCode(code);
                if (normalized != null) {
                    preferred.add(normalized);
                }
            }
        }
        background.setPreferredFutureCountries(writeJson(preferred));
        backgroundRepository.save(background);
    }

    private void applyRelationship(User user, RelationshipDto dto) {
        if (dto.maritalStatus() != null && !dto.maritalStatus().isBlank()) {
            validateAllowlistEnum("maritalStatus", dto.maritalStatus().trim().toUpperCase(Locale.ROOT),
                    IdentityReferenceEnums.MARITAL_STATUSES);
        }
        if (dto.previousMarriagesCount() != null && dto.divorcesCount() != null
                && dto.divorcesCount() > dto.previousMarriagesCount()) {
            throw new BadRequestException("divorcesCount cannot exceed previousMarriagesCount");
        }

        UserLifeProfile life = ensureLife(user);
        if (dto.maritalStatus() != null) {
            life.setMaritalStatus(blankToNull(dto.maritalStatus().trim().toUpperCase(Locale.ROOT)));
        }
        life.setPreviousMarriagesCount(dto.previousMarriagesCount());
        life.setDivorcesCount(dto.divorcesCount());
        life.setAnnulmentsCount(dto.annulmentsCount());
        life.setCurrentlySeparated(dto.currentlySeparated());
        life.setDivorceFinalized(dto.divorceFinalized());
        life.setMostRecentDivorceYear(dto.mostRecentDivorceYear());
        life.setCoParenting(trimToNull(dto.coParenting()));
        life.setUnresolvedCommitments(trimToNull(dto.unresolvedCommitments()));
        life.setRelationshipModelPref(trimToNull(dto.relationshipModelPref()));
        lifeProfileRepository.save(life);
    }

    private void applyFamily(User user, FamilyDto dto) {
        validateOptionalYesNoPrefer("hasChildren", dto.hasChildren());
        validateOptionalYesNoPrefer("futureChildrenPref", dto.futureChildrenPref());
        validateOptionalYesNoPrefer("openToPartnerWithChildren", dto.openToPartnerWithChildren());
        validateOptionalYesNoPrefer("adoptionPref", dto.adoptionPref());
        validateOptionalYesNoPrefer("fosterPref", dto.fosterPref());

        if (dto.childAgeRanges() != null) {
            for (String range : dto.childAgeRanges()) {
                if (range == null || range.isBlank()) continue;
                validateAllowlistEnum("childAgeRanges", range.trim().toUpperCase(Locale.ROOT),
                        IdentityReferenceEnums.CHILD_AGE_RANGES);
            }
        }

        UserLifeProfile life = ensureLife(user);
        if (dto.hasChildren() != null) {
            life.setHasChildren(blankToNull(dto.hasChildren().trim().toUpperCase(Locale.ROOT)));
        }
        life.setChildrenCount(dto.childrenCount());
        life.setChildrenLiveWithUser(trimToNull(dto.childrenLiveWithUser()));
        life.setCustodyArrangement(trimToNull(dto.custodyArrangement()));
        if (dto.futureChildrenPref() != null) {
            life.setFutureChildrenPref(blankToNull(dto.futureChildrenPref().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.openToPartnerWithChildren() != null) {
            life.setOpenToPartnerWithChildren(
                    blankToNull(dto.openToPartnerWithChildren().trim().toUpperCase(Locale.ROOT)));
        }
        life.setPreferredFutureChildrenCount(dto.preferredFutureChildrenCount());
        if (dto.adoptionPref() != null) {
            life.setAdoptionPref(blankToNull(dto.adoptionPref().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.fosterPref() != null) {
            life.setFosterPref(blankToNull(dto.fosterPref().trim().toUpperCase(Locale.ROOT)));
        }
        life.setElderCare(trimToNull(dto.elderCare()));
        life.setOtherDependents(trimToNull(dto.otherDependents()));
        life.setPetsInfo(trimToNull(dto.petsInfo()));

        List<String> ranges = new ArrayList<>();
        if (dto.childAgeRanges() != null) {
            for (String range : dto.childAgeRanges()) {
                String normalized = trimToNull(range);
                if (normalized != null) {
                    ranges.add(normalized.toUpperCase(Locale.ROOT));
                }
            }
        }
        life.setChildAgeRanges(writeJson(ranges));
        lifeProfileRepository.save(life);
    }

    private void applyEducations(UserProfile profile, List<EducationDto> educations) {
        profile.getEducations().clear();
        for (EducationDto dto : educations) {
            String institution = trimToNull(dto.institution());
            if (institution == null) {
                continue;
            }
            if (dto.educationLevel() != null && !dto.educationLevel().isBlank()) {
                validateAllowlistEnum("educationLevel", dto.educationLevel().trim().toUpperCase(Locale.ROOT),
                        IdentityReferenceEnums.EDUCATION_LEVELS);
            }
            if (dto.startYear() != null && dto.graduationYear() != null
                    && dto.graduationYear() < dto.startYear()) {
                throw new BadRequestException("graduationYear cannot be before startYear");
            }

            Education edu = new Education();
            edu.setProfile(profile);
            edu.setInstitution(institution);
            edu.setDegree(trimToNull(dto.degree()));
            edu.setFieldOfStudy(trimToNull(dto.fieldOfStudy()));
            edu.setCity(trimToNull(dto.city()));
            edu.setCountryCode(normalizeCountryCode(dto.countryCode()));
            edu.setStartYear(dto.startYear());
            edu.setEndYear(dto.graduationYear());
            if (dto.educationLevel() != null) {
                edu.setEducationLevel(blankToNull(dto.educationLevel().trim().toUpperCase(Locale.ROOT)));
            }
            edu.setCurrentlyStudying(Boolean.TRUE.equals(dto.currentlyStudying()));
            edu.setHonors(trimToNull(dto.honors()));
            edu.setShowInstitutionPublicly(Boolean.TRUE.equals(dto.showInstitutionPublicly()));
            edu.setVerificationDocumentId(dto.verificationDocumentId());
            profile.getEducations().add(edu);
        }
    }

    private void applyCareer(User user, UserProfile profile, CareerDto dto) {
        if (dto.employmentStatus() != null && !dto.employmentStatus().isBlank()) {
            validateAllowlistEnum("employmentStatus", dto.employmentStatus().trim().toUpperCase(Locale.ROOT),
                    IdentityReferenceEnums.EMPLOYMENT_STATUSES);
        }

        UserLifeProfile life = ensureLife(user);
        if (dto.employmentStatus() != null) {
            life.setEmploymentStatus(blankToNull(dto.employmentStatus().trim().toUpperCase(Locale.ROOT)));
        }
        life.setJobFunction(trimToNull(dto.jobFunction()));
        life.setIndustry(trimToNull(dto.industry()));
        life.setSeniority(trimToNull(dto.seniority()));
        life.setCompanySize(trimToNull(dto.companySize()));
        life.setWorkArrangement(trimToNull(dto.workArrangement()));
        life.setWorkSchedule(trimToNull(dto.workSchedule()));
        life.setSelfEmploymentCategory(trimToNull(dto.selfEmploymentCategory()));
        life.setYearsInProfession(dto.yearsInProfession());
        life.setCareerSatisfaction(trimToNull(dto.careerSatisfaction()));
        life.setTravelFrequency(trimToNull(dto.travelFrequency()));
        life.setRelocationPossibility(trimToNull(dto.relocationPossibility()));
        life.setCareerAmbitions(trimToNull(dto.careerAmbitions()));
        life.setWorkLifeBalancePref(trimToNull(dto.workLifeBalancePref()));
        life.setEmployerName(trimToNull(dto.employerName()));
        life.setShowEmployerPublicly(Boolean.TRUE.equals(dto.showEmployerPublicly()));
        lifeProfileRepository.save(life);

        if (life.getJobFunction() != null) {
            profile.setOccupation(life.getJobFunction());
        }
        profile.setEmployerNamePubliclyAllowed(Boolean.TRUE.equals(life.getShowEmployerPublicly()));
    }

    private void applyFinancial(User user, FinancialDto dto) {
        validateOptionalEnum("incomeRange", dto.incomeRange(), IdentityReferenceEnums.INCOME_RANGES);
        validateOptionalEnum("creditScoreRange", dto.creditScoreRange(), IdentityReferenceEnums.CREDIT_SCORE_RANGES);
        validateOptionalEnum("savingsRange", dto.savingsRange(), IdentityReferenceEnums.SAVINGS_RANGES);
        validateOptionalEnum("housingStatus", dto.housingStatus(), IdentityReferenceEnums.HOUSING_STATUSES);
        validateOptionalEnum("generalDebtRange", dto.generalDebtRange(), IdentityReferenceEnums.DEBT_RANGES);
        validateOptionalEnum("studentLoanRange", dto.studentLoanRange(), IdentityReferenceEnums.DEBT_RANGES);

        UserLifeProfile life = ensureLife(user);
        if (dto.incomeRange() != null) {
            life.setIncomeRange(blankToNull(dto.incomeRange().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.creditScoreRange() != null) {
            life.setCreditScoreRange(blankToNull(dto.creditScoreRange().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.savingsRange() != null) {
            life.setSavingsRange(blankToNull(dto.savingsRange().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.housingStatus() != null) {
            life.setHousingStatus(blankToNull(dto.housingStatus().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.generalDebtRange() != null) {
            life.setGeneralDebtRange(blankToNull(dto.generalDebtRange().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.studentLoanRange() != null) {
            life.setStudentLoanRange(blankToNull(dto.studentLoanRange().trim().toUpperCase(Locale.ROOT)));
        }
        life.setFinancialGoals(trimToNull(dto.financialGoals()));
        life.setSavingsHabits(trimToNull(dto.savingsHabits()));
        life.setSpendingStyle(trimToNull(dto.spendingStyle()));
        life.setBudgetConsciousness(trimToNull(dto.budgetConsciousness()));
        life.setJointFinancePref(trimToNull(dto.jointFinancePref()));
        life.setSeparateFinancePref(trimToNull(dto.separateFinancePref()));
        life.setHouseholdContributionExpectation(trimToNull(dto.householdContributionExpectation()));
        life.setExtendedFamilySupportPref(trimToNull(dto.extendedFamilySupportPref()));
        lifeProfileRepository.save(life);
    }

    private void applySafety(User user, SafetyDto dto) {
        validateOptionalSafety("criminalConviction", dto.criminalConviction());
        validateOptionalSafety("pendingCriminalCases", dto.pendingCriminalCases());
        validateOptionalSafety("protectiveRestrainingOrder", dto.protectiveRestrainingOrder());
        validateOptionalSafety("dvStalkingSexualOffense", dto.dvStalkingSexualOffense());
        validateOptionalSafety("governmentOffenderRegistry", dto.governmentOffenderRegistry());

        UserLifeProfile life = ensureLife(user);
        if (dto.criminalConviction() != null) {
            life.setCriminalConviction(blankToNull(dto.criminalConviction().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.pendingCriminalCases() != null) {
            life.setPendingCriminalCases(blankToNull(dto.pendingCriminalCases().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.protectiveRestrainingOrder() != null) {
            life.setProtectiveRestrainingOrder(
                    blankToNull(dto.protectiveRestrainingOrder().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.dvStalkingSexualOffense() != null) {
            life.setDvStalkingSexualOffense(
                    blankToNull(dto.dvStalkingSexualOffense().trim().toUpperCase(Locale.ROOT)));
        }
        if (dto.governmentOffenderRegistry() != null) {
            life.setGovernmentOffenderRegistry(
                    blankToNull(dto.governmentOffenderRegistry().trim().toUpperCase(Locale.ROOT)));
        }
        life.setSafetyJurisdiction(trimToNull(dto.jurisdiction()));
        life.setSafetyApproxYear(dto.approxYear());
        life.setSafetyCaseResolved(dto.caseResolved());
        life.setSafetyExplanation(trimToNull(dto.explanation()));
        lifeProfileRepository.save(life);
    }

    private void applyCivilJudgment(User user, CivilJudgmentDto dto) {
        validateOptionalYesNoPrefer("hasJudgment", dto.hasJudgment());

        UserLifeProfile life = ensureLife(user);
        if (dto.hasJudgment() != null) {
            life.setHasJudgment(blankToNull(dto.hasJudgment().trim().toUpperCase(Locale.ROOT)));
        }
        life.setCivilCategories(trimToNull(dto.categories()));
        life.setCivilJurisdiction(trimToNull(dto.jurisdiction()));
        life.setCivilApproxYear(dto.approxYear());
        life.setCivilResolved(dto.resolved());
        life.setCivilExplanation(trimToNull(dto.explanation()));
        lifeProfileRepository.save(life);
    }

    private void validateAndApply(User user, IdentityBackgroundRequest request, boolean strict) {
        String prefix = normalizeOptional(request.namePrefix());
        String first = trimToNull(request.firstName());
        String middle = trimToNull(request.middleName());
        String last = trimToNull(request.lastName());
        String suffix = normalizeOptional(request.nameSuffix());
        if ("None".equalsIgnoreCase(suffix)) {
            suffix = null;
        }
        String preferred = trimToNull(request.preferredName());

        validateAllowlist("namePrefix", prefix == null ? "" : prefix, PREFIXES);
        validateAllowlist("nameSuffix", suffix == null ? "" : suffix, SUFFIXES);
        validateNameField("firstName", first, strict);
        validateNameField("lastName", last, strict);
        if (middle != null) {
            validateNameField("middleName", middle, false);
        }
        if (preferred != null) {
            validateNameField("preferredName", preferred, false);
        }

        if (strict) {
            if (first == null || last == null) {
                throw new BadRequestException("First name and last name are required");
            }
            if (request.dateOfBirth() == null) {
                throw new BadRequestException("Date of birth is required");
            }
        }

        if (request.dateOfBirth() != null) {
            LocalDate dob = request.dateOfBirth();
            if (!dob.isBefore(LocalDate.now())) {
                throw new BadRequestException("Date of birth must be in the past");
            }
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age < MIN_AGE) {
                throw new UnprocessableEntityException(
                        "You must be at least " + MIN_AGE + " years old to continue");
            }
            user.set18OrOlder(true);
        }

        String pronouns = normalizeOptional(request.pronouns());
        String gender = normalizeOptional(request.gender());
        String visibility = normalizeOptional(request.genderShownToMatches());
        if (visibility == null || visibility.isBlank()) {
            visibility = "MATCHES";
        } else {
            visibility = visibility.toUpperCase(Locale.ROOT);
        }
        validateAllowlist("pronouns", pronouns == null ? "" : pronouns, PRONOUNS);
        validateAllowlist("gender", gender == null ? "" : gender, GENDERS);
        validateAllowlist("genderShownToMatches", visibility, GENDER_VISIBILITY);

        String secondaryEmail = trimToNull(request.secondaryEmail());
        if (secondaryEmail != null) {
            secondaryEmail = secondaryEmail.toLowerCase(Locale.ROOT);
        }

        String primaryPhone = optionalPhone(request.primaryPhone());
        String secondaryPhone = optionalPhone(request.secondaryPhone());
        String homePhone = optionalPhone(request.homePhone());

        String contactMethod = normalizeOptional(request.preferredContactMethod());
        if (contactMethod != null) {
            contactMethod = contactMethod.toUpperCase(Locale.ROOT);
        }
        String bestTime = normalizeOptional(request.bestTimeToContact());
        if (bestTime != null) {
            bestTime = bestTime.toUpperCase(Locale.ROOT);
        }
        validateAllowlist("preferredContactMethod", contactMethod == null ? "" : contactMethod, CONTACT_METHODS);
        validateAllowlist("bestTimeToContact", bestTime == null ? "" : bestTime, BEST_TIMES);

        user.setNamePrefix(blankToNull(prefix));
        if (first != null) {
            user.setFirstName(first);
        }
        user.setMiddleName(middle);
        if (last != null) {
            user.setLastName(last);
        }
        user.setNameSuffix(suffix);
        user.setPreferredName(preferred);
        user.setSecondaryEmail(secondaryEmail);
        if (primaryPhone != null) {
            if (!primaryPhone.equals(user.getPhoneNumber())) {
                user.setPhoneNumber(primaryPhone);
                user.setPhoneVerified(false);
            }
        }
        user.setSecondaryPhone(secondaryPhone);
        user.setHomePhone(homePhone);
        user.setPreferredContactMethod(blankToNull(contactMethod));
        user.setBestTimeToContact(blankToNull(bestTime));
    }

    private void applyProfileFields(UserProfile profile, IdentityBackgroundRequest request, boolean strict) {
        if (request.dateOfBirth() != null || strict) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        String pronouns = trimToNull(request.pronouns());
        String gender = trimToNull(request.gender());
        String visibility = trimToNull(request.genderShownToMatches());
        if (visibility != null) {
            visibility = visibility.toUpperCase(Locale.ROOT);
        } else if (profile.getGenderShownToMatches() == null) {
            visibility = "MATCHES";
        }
        profile.setPronouns(pronouns);
        if (gender != null || request.gender() != null) {
            profile.setGender(gender);
        }
        if (visibility != null) {
            profile.setGenderShownToMatches(visibility);
        }
    }

    private String optionalPhone(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            return PhoneNumberNormalizer.toE164(trimmed, defaultCountryCode);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private void validateNameField(String field, String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BadRequestException(field + " is required");
            }
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 60) {
            throw new BadRequestException(field + " exceeds maximum length");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException(field + " contains invalid characters");
        }
    }

    private void validateAllowlist(String field, String value, Set<String> allowed) {
        if (!allowed.contains(value) && !allowed.contains(value.trim())) {
            boolean ok = allowed.stream().anyMatch(a -> a.equalsIgnoreCase(value));
            if (!ok) {
                throw new BadRequestException("Unsupported value for " + field);
            }
        }
    }

    private void validateAllowlistEnum(String field, String value, List<String> allowed) {
        boolean ok = allowed.stream().anyMatch(a -> a.equalsIgnoreCase(value));
        if (!ok) {
            throw new BadRequestException("Unsupported value for " + field);
        }
    }

    private void validateOptionalEnum(String field, String value, List<String> allowed) {
        if (value == null || value.isBlank()) {
            return;
        }
        validateAllowlistEnum(field, value.trim().toUpperCase(Locale.ROOT), allowed);
    }

    private void validateOptionalYesNoPrefer(String field, String value) {
        validateOptionalEnum(field, value, IdentityReferenceEnums.YES_NO_PREFER);
    }

    private void validateOptionalSafety(String field, String value) {
        validateOptionalEnum(field, value, IdentityReferenceEnums.SAFETY_ANSWERS);
    }

    private void validateMonth(String field, Integer month) {
        if (month == null) {
            return;
        }
        if (month < 1 || month > 12) {
            throw new BadRequestException(field + " must be between 1 and 12");
        }
    }

    private void validateYearRange(String label, Integer fromMonth, Integer fromYear, Integer toMonth, Integer toYear) {
        if (fromYear == null || toYear == null) {
            return;
        }
        int from = fromYear * 12 + (fromMonth == null ? 1 : fromMonth);
        int to = toYear * 12 + (toMonth == null ? 12 : toMonth);
        if (to < from) {
            throw new BadRequestException(label + " to date must be on or after from date");
        }
    }

    private void validatePostalCode(String countryCode, String postalCode) {
        if (countryCode == null || postalCode == null) {
            return;
        }
        RefCountry country = refCountryRepository.findById(countryCode)
                .orElseThrow(() -> new BadRequestException("Unsupported countryCode: " + countryCode));
        String regex = country.getPostalRegex();
        if (regex != null && !regex.isBlank() && !Pattern.compile(regex).matcher(postalCode).matches()) {
            throw new BadRequestException("Invalid postalCode for country " + countryCode);
        }
    }

    private String normalizeCountryCode(String code) {
        String trimmed = trimToNull(code);
        if (trimmed == null) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String resolveCountryName(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        return refCountryRepository.findById(countryCode)
                .map(RefCountry::getName)
                .orElse(countryCode);
    }

    private String buildPrivateAddressSummary(UserIdentityBackground background) {
        return Stream.of(
                        background.getLine1(),
                        background.getCity(),
                        background.getStateRegion(),
                        background.getPostalCode(),
                        background.getCountryCode()
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private Integer calculateResidenceDurationMonths(Integer month, Integer year) {
        if (month == null || year == null) {
            return null;
        }
        if (month < 1 || month > 12) {
            return null;
        }
        LocalDate moveIn = LocalDate.of(year, month, 1);
        LocalDate now = LocalDate.now().withDayOfMonth(1);
        if (moveIn.isAfter(now)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(moveIn, now);
    }

    private void writeAudit(User user, String eventType, String entityRef) {
        writeAudit(user, user, eventType, entityRef);
    }

    private void writeAudit(User subject, User actor, String eventType, String entityRef) {
        UserIdentityCompliance compliance = ensureCompliance(subject);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", eventType);
        event.put("entityRef", entityRef);
        event.put("actorUserId", actor == null ? null : actor.getId());
        event.put("createdAt", LocalDateTime.now().toString());
        appendJsonArray(compliance, "auditEvents", compliance.getAuditEvents(), event, MAX_AUDIT_EVENTS);
        complianceRepository.save(compliance);
    }

    private void appendDocumentMeta(UserIdentityCompliance compliance, UserIdentityComplianceBlob saved) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", saved.getId());
        meta.put("docPurpose", saved.getDocPurpose());
        meta.put("contentType", saved.getContentType());
        meta.put("fileSizeKb", saved.getFileSizeKb());
        meta.put("createdAt", saved.getCreatedAt() == null
                ? LocalDateTime.now().toString()
                : saved.getCreatedAt().toString());
        appendJsonArray(compliance, "documentsMeta", compliance.getDocumentsMeta(), meta, Integer.MAX_VALUE);
    }

    private void appendJsonArray(
            UserIdentityCompliance compliance,
            String field,
            String existingJson,
            Map<String, Object> entry,
            int maxSize
    ) {
        List<Map<String, Object>> list = readMapList(existingJson);
        list.add(entry);
        if (list.size() > maxSize) {
            list = new ArrayList<>(list.subList(list.size() - maxSize, list.size()));
        }
        String json = writeJson(list);
        if ("consents".equals(field)) {
            compliance.setConsents(json);
        } else if ("auditEvents".equals(field)) {
            compliance.setAuditEvents(json);
        } else if ("documentsMeta".equals(field)) {
            compliance.setDocumentsMeta(json);
        }
    }

    private UserProfile ensureProfile(User user) {
        return profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUser(user);
            p.setGenderShownToMatches("MATCHES");
            return profileRepository.save(p);
        });
    }

    private IdentityBackgroundResponse toResponse(User user, UserProfile profile) {
        Integer age = null;
        if (profile.getDateOfBirth() != null) {
            age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
        }
        String preferred = user.getPreferredName();
        String displayName = (preferred != null && !preferred.isBlank())
                ? preferred.trim()
                : user.getFirstName();
        boolean complete = OnboardingSteps.isIdentityPage1Complete(
                profile.getOnboardingStep(),
                profile.getIdentityPage1CompletedAt());

        Optional<UserIdentityBackground> backgroundOpt = backgroundRepository.findByUserId(user.getId());
        CurrentResidenceDto residenceDto = backgroundOpt
                .filter(this::hasResidenceData)
                .map(this::toResidenceDto)
                .orElse(null);
        List<PreviousAddressDto> previous = backgroundOpt
                .map(bg -> toPreviousAddressDtos(bg.getPreviousAddresses()))
                .orElse(List.of());
        NationalityDto nationality = backgroundOpt
                .filter(this::hasNationalityData)
                .map(this::toNationalityDto)
                .orElse(null);
        ImmigrationDto immigration = backgroundOpt
                .filter(this::hasImmigrationData)
                .map(this::toImmigrationDto)
                .orElse(null);

        Optional<UserLifeProfile> lifeOpt = lifeProfileRepository.findByUserId(user.getId());
        RelationshipDto relationship = lifeOpt
                .filter(this::hasRelationshipData)
                .map(this::toRelationshipDto)
                .orElse(null);
        FamilyDto family = lifeOpt
                .filter(this::hasFamilyData)
                .map(this::toFamilyDto)
                .orElse(null);
        CareerDto career = lifeOpt
                .filter(this::hasCareerData)
                .map(this::toCareerDto)
                .orElse(null);
        FinancialDto financial = lifeOpt
                .filter(this::hasFinancialData)
                .map(this::toFinancialDto)
                .orElse(null);
        SafetyDto safety = lifeOpt
                .filter(this::hasSafetyData)
                .map(this::toSafetyDto)
                .orElse(null);
        CivilJudgmentDto civilJudgment = lifeOpt
                .filter(this::hasCivilData)
                .map(this::toCivilJudgmentDto)
                .orElse(null);

        List<EducationDto> educations = profile.getEducations() == null ? List.of() : profile.getEducations().stream()
                .map(this::toEducationDto)
                .toList();

        Optional<UserIdentityCompliance> complianceOpt = complianceRepository.findByUserId(user.getId());
        VerificationSummaryDto verificationSummary = complianceOpt
                .map(v -> new VerificationSummaryDto(
                        v.getProvider(),
                        v.getSessionId(),
                        v.getOverallStatus(),
                        v.getNameStatus(),
                        v.getAgeStatus(),
                        v.getPhotoStatus(),
                        Boolean.TRUE.equals(profile.getShowVerificationBadge()),
                        maskSsn(v.getSsn()),
                        v.getDlFrontBlobId(),
                        v.getDlBackBlobId()
                ))
                .orElse(null);
        BackgroundConsentDto backgroundConsent = complianceOpt
                .flatMap(c -> latestAcceptedConsent(c.getConsents()))
                .orElse(null);

        return new IdentityBackgroundResponse(
                user.getNamePrefix(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getNameSuffix(),
                user.getPreferredName(),
                displayName,
                profile.getDateOfBirth(),
                age,
                profile.getPronouns(),
                profile.getGender(),
                profile.getGenderShownToMatches() == null ? "MATCHES" : profile.getGenderShownToMatches(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getSecondaryEmail(),
                user.getPhoneNumber(),
                user.isPhoneVerified(),
                user.getSecondaryPhone(),
                user.getHomePhone(),
                user.getPreferredContactMethod(),
                user.getBestTimeToContact(),
                profile.getOnboardingStep(),
                complete,
                residenceDto,
                previous,
                nationality,
                immigration,
                relationship,
                family,
                educations,
                career,
                financial,
                safety,
                civilJudgment,
                verificationSummary,
                backgroundConsent,
                profile.getIncomeRangeSharePreference(),
                profile.getLocationCity(),
                profile.getLocationCountry()
        );
    }

    private boolean hasResidenceData(UserIdentityBackground bg) {
        return trimToNull(bg.getLine1()) != null
                || trimToNull(bg.getCity()) != null
                || trimToNull(bg.getStateRegion()) != null
                || trimToNull(bg.getPostalCode()) != null
                || trimToNull(bg.getCountryCode()) != null
                || trimToNull(bg.getResidenceType()) != null
                || bg.getMoveInMonth() != null
                || bg.getMoveInYear() != null
                || trimToNull(bg.getWillingToRelocate()) != null
                || bg.getEventTravelRadiusKm() != null
                || !readStringList(bg.getPreferredRelocateLocations()).isEmpty();
    }

    private boolean hasNationalityData(UserIdentityBackground bg) {
        return trimToNull(bg.getCountryOfBirth()) != null
                || trimToNull(bg.getPrimaryNationality()) != null
                || trimToNull(bg.getCountryOfCitizenship()) != null
                || !readStringList(bg.getAdditionalNationalities()).isEmpty()
                || !readMapList(bg.getLanguages()).isEmpty();
    }

    private boolean hasImmigrationData(UserIdentityBackground bg) {
        return trimToNull(bg.getCurrentCountryOfResidence()) != null
                || trimToNull(bg.getResidencyCategory()) != null
                || trimToNull(bg.getInternationalRelocationPref()) != null
                || trimToNull(bg.getFutureSponsorshipRequired()) != null
                || trimToNull(bg.getOpenToPartnerAbroad()) != null
                || !readStringList(bg.getPreferredFutureCountries()).isEmpty();
    }

    private boolean hasRelationshipData(UserLifeProfile life) {
        return trimToNull(life.getMaritalStatus()) != null
                || life.getPreviousMarriagesCount() != null
                || life.getDivorcesCount() != null
                || life.getAnnulmentsCount() != null
                || life.getCurrentlySeparated() != null
                || life.getDivorceFinalized() != null
                || life.getMostRecentDivorceYear() != null
                || trimToNull(life.getCoParenting()) != null
                || trimToNull(life.getUnresolvedCommitments()) != null
                || trimToNull(life.getRelationshipModelPref()) != null;
    }

    private boolean hasFamilyData(UserLifeProfile life) {
        return trimToNull(life.getHasChildren()) != null
                || life.getChildrenCount() != null
                || trimToNull(life.getChildrenLiveWithUser()) != null
                || trimToNull(life.getCustodyArrangement()) != null
                || trimToNull(life.getFutureChildrenPref()) != null
                || trimToNull(life.getOpenToPartnerWithChildren()) != null
                || life.getPreferredFutureChildrenCount() != null
                || trimToNull(life.getAdoptionPref()) != null
                || trimToNull(life.getFosterPref()) != null
                || trimToNull(life.getElderCare()) != null
                || trimToNull(life.getOtherDependents()) != null
                || trimToNull(life.getPetsInfo()) != null
                || !readStringList(life.getChildAgeRanges()).isEmpty();
    }

    private boolean hasCareerData(UserLifeProfile life) {
        return trimToNull(life.getEmploymentStatus()) != null
                || trimToNull(life.getJobFunction()) != null
                || trimToNull(life.getIndustry()) != null
                || trimToNull(life.getSeniority()) != null
                || trimToNull(life.getCompanySize()) != null
                || trimToNull(life.getWorkArrangement()) != null
                || trimToNull(life.getWorkSchedule()) != null
                || trimToNull(life.getSelfEmploymentCategory()) != null
                || life.getYearsInProfession() != null
                || trimToNull(life.getCareerSatisfaction()) != null
                || trimToNull(life.getTravelFrequency()) != null
                || trimToNull(life.getRelocationPossibility()) != null
                || trimToNull(life.getCareerAmbitions()) != null
                || trimToNull(life.getWorkLifeBalancePref()) != null
                || trimToNull(life.getEmployerName()) != null
                || Boolean.TRUE.equals(life.getShowEmployerPublicly());
    }

    private boolean hasFinancialData(UserLifeProfile life) {
        return trimToNull(life.getIncomeRange()) != null
                || trimToNull(life.getCreditScoreRange()) != null
                || trimToNull(life.getSavingsRange()) != null
                || trimToNull(life.getHousingStatus()) != null
                || trimToNull(life.getGeneralDebtRange()) != null
                || trimToNull(life.getStudentLoanRange()) != null
                || trimToNull(life.getFinancialGoals()) != null
                || trimToNull(life.getSavingsHabits()) != null
                || trimToNull(life.getSpendingStyle()) != null
                || trimToNull(life.getBudgetConsciousness()) != null
                || trimToNull(life.getJointFinancePref()) != null
                || trimToNull(life.getSeparateFinancePref()) != null
                || trimToNull(life.getHouseholdContributionExpectation()) != null
                || trimToNull(life.getExtendedFamilySupportPref()) != null;
    }

    private boolean hasSafetyData(UserLifeProfile life) {
        return trimToNull(life.getCriminalConviction()) != null
                || trimToNull(life.getPendingCriminalCases()) != null
                || trimToNull(life.getProtectiveRestrainingOrder()) != null
                || trimToNull(life.getDvStalkingSexualOffense()) != null
                || trimToNull(life.getGovernmentOffenderRegistry()) != null
                || trimToNull(life.getSafetyJurisdiction()) != null
                || life.getSafetyApproxYear() != null
                || life.getSafetyCaseResolved() != null
                || trimToNull(life.getSafetyExplanation()) != null;
    }

    private boolean hasCivilData(UserLifeProfile life) {
        return trimToNull(life.getHasJudgment()) != null
                || trimToNull(life.getCivilCategories()) != null
                || trimToNull(life.getCivilJurisdiction()) != null
                || life.getCivilApproxYear() != null
                || life.getCivilResolved() != null
                || trimToNull(life.getCivilExplanation()) != null;
    }

    private CurrentResidenceDto toResidenceDto(UserIdentityBackground r) {
        return new CurrentResidenceDto(
                r.getLine1(),
                r.getLine2(),
                r.getUnit(),
                r.getCity(),
                r.getStateRegion(),
                r.getPostalCode(),
                r.getCountryCode(),
                r.getResidenceType(),
                r.getMoveInMonth(),
                r.getMoveInYear(),
                calculateResidenceDurationMonths(r.getMoveInMonth(), r.getMoveInYear()),
                r.getWillingToRelocate(),
                r.getEventTravelRadiusKm(),
                readStringList(r.getPreferredRelocateLocations())
        );
    }

    private List<PreviousAddressDto> toPreviousAddressDtos(String json) {
        List<Map<String, Object>> rows = readMapList(json);
        List<PreviousAddressDto> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new PreviousAddressDto(
                    asLong(row.get("id")),
                    asString(row.get("city")),
                    asString(row.get("stateRegion")),
                    asString(row.get("countryCode")),
                    asString(row.get("postalCode")),
                    asInteger(row.get("fromMonth")),
                    asInteger(row.get("fromYear")),
                    asInteger(row.get("toMonth")),
                    asInteger(row.get("toYear")),
                    asString(row.get("reasonForMoving"))
            ));
        }
        return result;
    }

    private NationalityDto toNationalityDto(UserIdentityBackground n) {
        List<LanguageDto> languages = readMapList(n.getLanguages()).stream()
                .map(l -> new LanguageDto(asString(l.get("languageCode")), asString(l.get("proficiency"))))
                .toList();
        return new NationalityDto(
                n.getCountryOfBirth(),
                n.getPrimaryNationality(),
                n.getCountryOfCitizenship(),
                readStringList(n.getAdditionalNationalities()),
                languages
        );
    }

    private ImmigrationDto toImmigrationDto(UserIdentityBackground i) {
        return new ImmigrationDto(
                i.getCurrentCountryOfResidence(),
                i.getResidencyCategory(),
                i.getInternationalRelocationPref(),
                i.getFutureSponsorshipRequired(),
                i.getOpenToPartnerAbroad(),
                readStringList(i.getPreferredFutureCountries())
        );
    }

    private RelationshipDto toRelationshipDto(UserLifeProfile r) {
        return new RelationshipDto(
                r.getMaritalStatus(),
                r.getPreviousMarriagesCount(),
                r.getDivorcesCount(),
                r.getAnnulmentsCount(),
                r.getCurrentlySeparated(),
                r.getDivorceFinalized(),
                r.getMostRecentDivorceYear(),
                r.getCoParenting(),
                r.getUnresolvedCommitments(),
                r.getRelationshipModelPref()
        );
    }

    private FamilyDto toFamilyDto(UserLifeProfile f) {
        return new FamilyDto(
                f.getHasChildren(),
                f.getChildrenCount(),
                readStringList(f.getChildAgeRanges()),
                f.getChildrenLiveWithUser(),
                f.getCustodyArrangement(),
                f.getFutureChildrenPref(),
                f.getOpenToPartnerWithChildren(),
                f.getPreferredFutureChildrenCount(),
                f.getAdoptionPref(),
                f.getFosterPref(),
                f.getElderCare(),
                f.getOtherDependents(),
                f.getPetsInfo()
        );
    }

    private EducationDto toEducationDto(Education e) {
        return new EducationDto(
                e.getId(),
                e.getEducationLevel(),
                e.getDegree(),
                e.getFieldOfStudy(),
                e.getInstitution(),
                e.getCity(),
                e.getCountryCode(),
                e.getStartYear(),
                e.getEndYear(),
                e.getCurrentlyStudying(),
                e.getHonors(),
                e.getShowInstitutionPublicly(),
                e.getVerificationDocumentId()
        );
    }

    private CareerDto toCareerDto(UserLifeProfile c) {
        return new CareerDto(
                c.getEmploymentStatus(),
                c.getJobFunction(),
                c.getIndustry(),
                c.getSeniority(),
                c.getCompanySize(),
                c.getWorkArrangement(),
                c.getWorkSchedule(),
                c.getSelfEmploymentCategory(),
                c.getYearsInProfession(),
                c.getCareerSatisfaction(),
                c.getTravelFrequency(),
                c.getRelocationPossibility(),
                c.getCareerAmbitions(),
                c.getWorkLifeBalancePref(),
                c.getEmployerName(),
                c.getShowEmployerPublicly()
        );
    }

    private FinancialDto toFinancialDto(UserLifeProfile f) {
        return new FinancialDto(
                f.getIncomeRange(),
                f.getCreditScoreRange(),
                f.getSavingsRange(),
                f.getHousingStatus(),
                f.getGeneralDebtRange(),
                f.getStudentLoanRange(),
                f.getFinancialGoals(),
                f.getSavingsHabits(),
                f.getSpendingStyle(),
                f.getBudgetConsciousness(),
                f.getJointFinancePref(),
                f.getSeparateFinancePref(),
                f.getHouseholdContributionExpectation(),
                f.getExtendedFamilySupportPref()
        );
    }

    private SafetyDto toSafetyDto(UserLifeProfile s) {
        return new SafetyDto(
                s.getCriminalConviction(),
                s.getPendingCriminalCases(),
                s.getProtectiveRestrainingOrder(),
                s.getDvStalkingSexualOffense(),
                s.getGovernmentOffenderRegistry(),
                s.getSafetyJurisdiction(),
                s.getSafetyApproxYear(),
                s.getSafetyCaseResolved(),
                s.getSafetyExplanation()
        );
    }

    private CivilJudgmentDto toCivilJudgmentDto(UserLifeProfile c) {
        return new CivilJudgmentDto(
                c.getHasJudgment(),
                c.getCivilCategories(),
                c.getCivilJurisdiction(),
                c.getCivilApproxYear(),
                c.getCivilResolved(),
                c.getCivilExplanation()
        );
    }

    private Optional<BackgroundConsentDto> latestAcceptedConsent(String consentsJson) {
        return readConsentList(consentsJson).stream()
                .filter(c -> Boolean.TRUE.equals(asBoolean(c.get("accepted")))
                        && OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION
                        .equals(asString(c.get("documentVersion"))))
                .max(Comparator.comparing(
                        c -> asString(c.get("acceptedAt")),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> new BackgroundConsentDto(
                        true,
                        asString(c.get("documentVersion")),
                        asString(c.get("acceptedAt"))
                ));
    }

    private List<Map<String, Object>> readConsentList(String json) {
        return readMapList(json);
    }

    private List<Map<String, Object>> readMapList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list == null ? new ArrayList<>() : new ArrayList<>(list);
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list == null ? List.of() : list;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to serialize identity JSON");
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Boolean asBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private static Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
