package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.OnboardingSteps;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundRequest;
import com.SocialPairly_Workflow_Manager.dto.IdentityBackgroundResponse;
import com.SocialPairly_Workflow_Manager.dto.IdentitySectionDtos.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Targets remaining BRANCH gaps in IdentityOnboardingService — especially has*Data
 * short-circuit chains and partial apply* null branches.
 */
@ExtendWith(MockitoExtension.class)
class IdentityOnboardingServiceBranchCoverageTest {

    @Mock UserRepository userRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock RefCountryRepository refCountryRepository;
    @Mock UserIdentityBackgroundRepository backgroundRepository;
    @Mock UserLifeProfileRepository lifeProfileRepository;
    @Mock UserIdentityComplianceRepository complianceRepository;
    @Mock UserIdentityComplianceBlobRepository blobRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    IdentityOnboardingService service;
    User user;
    UserProfile profile;

    @BeforeEach
    void setUp() {
        service = new IdentityOnboardingService(
                userRepository, profileRepository, refCountryRepository,
                backgroundRepository, lifeProfileRepository, complianceRepository,
                blobRepository, objectMapper, "+91");

        user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setPhoneNumber("+919876543210");
        user.setPassword("x");
        user.setRole(Role.USER);

        profile = new UserProfile();
        profile.setUser(user);
        profile.setOnboardingStep(OnboardingSteps.STEP_1_ACCOUNT);
        profile.setEducations(new ArrayList<>());

        lenient().when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(backgroundRepository.save(any(UserIdentityBackground.class))).thenAnswer(inv -> {
            UserIdentityBackground bg = inv.getArgument(0);
            if (bg.getId() == null) bg.setId(20L);
            lenient().when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
            return bg;
        });
        lenient().when(lifeProfileRepository.save(any(UserLifeProfile.class))).thenAnswer(inv -> {
            UserLifeProfile life = inv.getArgument(0);
            if (life.getId() == null) life.setId(30L);
            lenient().when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
            return life;
        });
        stubAcceptedConsent();

        RefCountry us = new RefCountry();
        us.setCode("US");
        us.setName("United States");
        us.setPostalRegex("^[0-9]{5}(-[0-9]{4})?$");
        us.setActive(true);
        lenient().when(refCountryRepository.findById("US")).thenReturn(Optional.of(us));
        lenient().when(refCountryRepository.findByActiveTrue()).thenReturn(List.of(us));

        RefCountry noRegex = new RefCountry();
        noRegex.setCode("XX");
        noRegex.setName("No Regex Land");
        noRegex.setActive(true);
        lenient().when(refCountryRepository.findById("XX")).thenReturn(Optional.of(noRegex));
    }

    private void stubAcceptedConsent() {
        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.setId(10L);
        compliance.setUser(user);
        compliance.setConsents("""
                [{"documentVersion":"%s","accepted":true,"acceptedAt":"%s","createdAt":"%s"}]
                """.formatted(
                OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION,
                LocalDateTime.now(), LocalDateTime.now()).trim());
        lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));
        lenient().when(complianceRepository.save(any(UserIdentityCompliance.class))).thenAnswer(inv -> {
            UserIdentityCompliance c = inv.getArgument(0);
            if (c.getId() == null) c.setId(10L);
            lenient().when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(c));
            return c;
        });
    }

    private UserIdentityBackground emptyBg() {
        UserIdentityBackground bg = new UserIdentityBackground();
        bg.setId(20L);
        bg.setUser(user);
        return bg;
    }

    private UserLifeProfile emptyLife() {
        UserLifeProfile life = new UserLifeProfile();
        life.setId(30L);
        life.setUser(user);
        return life;
    }

    private void stubBg(Consumer<UserIdentityBackground> mutator) {
        UserIdentityBackground bg = emptyBg();
        mutator.accept(bg);
        when(backgroundRepository.findByUserId(1L)).thenReturn(Optional.of(bg));
    }

    private void stubLife(Consumer<UserLifeProfile> mutator) {
        UserLifeProfile life = emptyLife();
        mutator.accept(life);
        when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(life));
    }

    private IdentityBackgroundRequest saveLaterWith(
            CurrentResidenceDto residence,
            NationalityDto nationality,
            ImmigrationDto immigration,
            RelationshipDto relationship,
            FamilyDto family,
            List<EducationDto> educations,
            CareerDto career,
            FinancialDto financial,
            SafetyDto safety,
            CivilJudgmentDto civil
    ) {
        return new IdentityBackgroundRequest(
                "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                residence, null, nationality, immigration, relationship,
                family, educations, career, financial, safety, civil,
                null, null
        );
    }

    @Nested
    class HasResidenceDataBranches {
        @Test void line1Only() {
            stubBg(bg -> bg.setLine1("1 Main"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void cityOnly() {
            stubBg(bg -> bg.setCity("Austin"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void stateOnly() {
            stubBg(bg -> bg.setStateRegion("TX"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void postalOnly() {
            stubBg(bg -> bg.setPostalCode("78701"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void countryOnly() {
            stubBg(bg -> bg.setCountryCode("US"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void residenceTypeOnly() {
            stubBg(bg -> bg.setResidenceType("RENT"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void moveInMonthOnly() {
            stubBg(bg -> bg.setMoveInMonth(3));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void moveInYearOnly() {
            stubBg(bg -> bg.setMoveInYear(2020));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void relocatePrefOnly() {
            stubBg(bg -> bg.setWillingToRelocate("YES"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void travelRadiusOnly() {
            stubBg(bg -> bg.setEventTravelRadiusMiles(25));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void preferredLocationsOnly() {
            stubBg(bg -> bg.setPreferredRelocateLocations("[\"Dallas\"]"));
            assertNotNull(service.getIdentity(user).currentResidence());
        }
        @Test void blankStringsDoNotCountAsResidence() {
            stubBg(bg -> {
                bg.setLine1("  ");
                bg.setCity("");
                bg.setPreferredRelocateLocations("[]");
            });
            assertNull(service.getIdentity(user).currentResidence());
        }
    }

    @Nested
    class HasNationalityDataBranches {
        @Test void countryOfBirthOnly() {
            stubBg(bg -> bg.setCountryOfBirth("US"));
            assertNotNull(service.getIdentity(user).nationality());
        }
        @Test void primaryNationalityOnly() {
            stubBg(bg -> bg.setPrimaryNationality("US"));
            assertNotNull(service.getIdentity(user).nationality());
        }
        @Test void citizenshipOnly() {
            stubBg(bg -> bg.setCountryOfCitizenship("US"));
            assertNotNull(service.getIdentity(user).nationality());
        }
        @Test void additionalNationalitiesOnly() {
            stubBg(bg -> bg.setAdditionalNationalities("[\"CA\"]"));
            assertNotNull(service.getIdentity(user).nationality());
        }
        @Test void languagesOnly() {
            stubBg(bg -> bg.setLanguages("[{\"languageCode\":\"en\",\"proficiency\":\"NATIVE\"}]"));
            assertNotNull(service.getIdentity(user).nationality());
        }
    }

    @Nested
    class HasImmigrationDataBranches {
        @Test void currentCountryOnly() {
            stubBg(bg -> bg.setCurrentCountryOfResidence("US"));
            assertNotNull(service.getIdentity(user).immigration());
        }
        @Test void residencyCategoryOnly() {
            stubBg(bg -> bg.setResidencyCategory("CITIZEN"));
            assertNotNull(service.getIdentity(user).immigration());
        }
        @Test void internationalRelocOnly() {
            stubBg(bg -> bg.setInternationalRelocationPref("YES"));
            assertNotNull(service.getIdentity(user).immigration());
        }
        @Test void sponsorshipOnly() {
            stubBg(bg -> bg.setFutureSponsorshipRequired("NO"));
            assertNotNull(service.getIdentity(user).immigration());
        }
        @Test void partnerAbroadOnly() {
            stubBg(bg -> bg.setOpenToPartnerAbroad("YES"));
            assertNotNull(service.getIdentity(user).immigration());
        }
        @Test void preferredCountriesOnly() {
            stubBg(bg -> bg.setPreferredFutureCountries("[\"CA\"]"));
            assertNotNull(service.getIdentity(user).immigration());
        }
    }

    @Nested
    class HasRelationshipDataBranches {
        @Test void maritalOnly() {
            stubLife(l -> l.setMaritalStatus("NEVER_MARRIED"));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void previousMarriagesOnly() {
            stubLife(l -> l.setPreviousMarriagesCount(1));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void divorcesOnly() {
            stubLife(l -> l.setDivorcesCount(1));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void annulmentsOnly() {
            stubLife(l -> l.setAnnulmentsCount(1));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void separatedOnly() {
            stubLife(l -> l.setCurrentlySeparated(true));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void divorceFinalizedOnly() {
            stubLife(l -> l.setDivorceFinalized(false));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void divorceYearOnly() {
            stubLife(l -> l.setMostRecentDivorceYear(2020));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void coParentingOnly() {
            stubLife(l -> l.setCoParenting("YES"));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void unresolvedOnly() {
            stubLife(l -> l.setUnresolvedCommitments("NONE"));
            assertNotNull(service.getIdentity(user).relationship());
        }
        @Test void modelPrefOnly() {
            stubLife(l -> l.setRelationshipModelPref("MONOGAMY"));
            assertNotNull(service.getIdentity(user).relationship());
        }
    }

    @Nested
    class HasFamilyDataBranches {
        @Test void hasChildrenOnly() { stubLife(l -> l.setHasChildren("YES")); assertNotNull(service.getIdentity(user).family()); }
        @Test void childrenCountOnly() { stubLife(l -> l.setChildrenCount(2)); assertNotNull(service.getIdentity(user).family()); }
        @Test void liveWithOnly() { stubLife(l -> l.setChildrenLiveWithUser("YES")); assertNotNull(service.getIdentity(user).family()); }
        @Test void custodyOnly() { stubLife(l -> l.setCustodyArrangement("FULL")); assertNotNull(service.getIdentity(user).family()); }
        @Test void futureChildrenOnly() { stubLife(l -> l.setFutureChildrenPref("YES")); assertNotNull(service.getIdentity(user).family()); }
        @Test void openToPartnerKidsOnly() { stubLife(l -> l.setOpenToPartnerWithChildren("YES")); assertNotNull(service.getIdentity(user).family()); }
        @Test void preferredCountOnly() { stubLife(l -> l.setPreferredFutureChildrenCount(1)); assertNotNull(service.getIdentity(user).family()); }
        @Test void adoptionOnly() { stubLife(l -> l.setAdoptionPref("NO")); assertNotNull(service.getIdentity(user).family()); }
        @Test void fosterOnly() { stubLife(l -> l.setFosterPref("NO")); assertNotNull(service.getIdentity(user).family()); }
        @Test void elderCareOnly() { stubLife(l -> l.setElderCare("YES")); assertNotNull(service.getIdentity(user).family()); }
        @Test void otherDependentsOnly() { stubLife(l -> l.setOtherDependents("Parent")); assertNotNull(service.getIdentity(user).family()); }
        @Test void petsOnly() { stubLife(l -> l.setPetsInfo("cat")); assertNotNull(service.getIdentity(user).family()); }
        @Test void childAgeRangesOnly() { stubLife(l -> l.setChildAgeRanges("[\"0_2\"]")); assertNotNull(service.getIdentity(user).family()); }
    }

    @Nested
    class HasCareerDataBranches {
        @Test void employmentOnly() { stubLife(l -> l.setEmploymentStatus("EMPLOYEE")); assertNotNull(service.getIdentity(user).career()); }
        @Test void jobFunctionOnly() { stubLife(l -> l.setJobFunction("Eng")); assertNotNull(service.getIdentity(user).career()); }
        @Test void industryOnly() { stubLife(l -> l.setIndustry("Tech")); assertNotNull(service.getIdentity(user).career()); }
        @Test void seniorityOnly() { stubLife(l -> l.setSeniority("Senior")); assertNotNull(service.getIdentity(user).career()); }
        @Test void companySizeOnly() { stubLife(l -> l.setCompanySize("500+")); assertNotNull(service.getIdentity(user).career()); }
        @Test void workArrangementOnly() { stubLife(l -> l.setWorkArrangement("REMOTE")); assertNotNull(service.getIdentity(user).career()); }
        @Test void workScheduleOnly() { stubLife(l -> l.setWorkSchedule("FULL_TIME")); assertNotNull(service.getIdentity(user).career()); }
        @Test void selfEmployedOnly() { stubLife(l -> l.setSelfEmploymentCategory("FREELANCE")); assertNotNull(service.getIdentity(user).career()); }
        @Test void yearsOnly() { stubLife(l -> l.setYearsInProfession(5)); assertNotNull(service.getIdentity(user).career()); }
        @Test void satisfactionOnly() { stubLife(l -> l.setCareerSatisfaction("HIGH")); assertNotNull(service.getIdentity(user).career()); }
        @Test void travelOnly() { stubLife(l -> l.setTravelFrequency("RARELY")); assertNotNull(service.getIdentity(user).career()); }
        @Test void relocationOnly() { stubLife(l -> l.setRelocationPossibility("OPEN")); assertNotNull(service.getIdentity(user).career()); }
        @Test void ambitionsOnly() { stubLife(l -> l.setCareerAmbitions("Lead")); assertNotNull(service.getIdentity(user).career()); }
        @Test void workLifeOnly() { stubLife(l -> l.setWorkLifeBalancePref("BALANCED")); assertNotNull(service.getIdentity(user).career()); }
        @Test void employerOnly() { stubLife(l -> l.setEmployerName("Acme")); assertNotNull(service.getIdentity(user).career()); }
        @Test void showEmployerPubliclyOnly() { stubLife(l -> l.setShowEmployerPublicly(true)); assertNotNull(service.getIdentity(user).career()); }
        @Test void showEmployerFalseDoesNotCountAlone() {
            stubLife(l -> l.setShowEmployerPublicly(false));
            assertNull(service.getIdentity(user).career());
        }
    }

    @Nested
    class HasFinancialDataBranches {
        @Test void incomeOnly() { stubLife(l -> l.setIncomeRange("100K_149K")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void creditOnly() { stubLife(l -> l.setCreditScoreRange("740_799")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void savingsOnly() { stubLife(l -> l.setSavingsRange("50K_99K")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void housingOnly() { stubLife(l -> l.setHousingStatus("RENTER")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void debtOnly() { stubLife(l -> l.setGeneralDebtRange("NONE")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void studentLoanOnly() { stubLife(l -> l.setStudentLoanRange("NONE")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void goalsOnly() { stubLife(l -> l.setFinancialGoals("Retire")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void habitsOnly() { stubLife(l -> l.setSavingsHabits("Regular")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void spendingOnly() { stubLife(l -> l.setSpendingStyle("Moderate")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void budgetOnly() { stubLife(l -> l.setBudgetConsciousness("HIGH")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void jointOnly() { stubLife(l -> l.setJointFinancePref("OPEN")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void separateOnly() { stubLife(l -> l.setSeparateFinancePref("OPEN")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void householdOnly() { stubLife(l -> l.setHouseholdContributionExpectation("50/50")); assertNotNull(service.getIdentity(user).financial()); }
        @Test void extendedFamilyOnly() { stubLife(l -> l.setExtendedFamilySupportPref("YES")); assertNotNull(service.getIdentity(user).financial()); }
    }

    @Nested
    class HasSafetyAndCivilBranches {
        @Test void criminalOnly() { stubLife(l -> l.setCriminalConviction("NO")); assertNotNull(service.getIdentity(user).safety()); }
        @Test void pendingOnly() { stubLife(l -> l.setPendingCriminalCases("NO")); assertNotNull(service.getIdentity(user).safety()); }
        @Test void protectiveOnly() { stubLife(l -> l.setProtectiveRestrainingOrder("NO")); assertNotNull(service.getIdentity(user).safety()); }
        @Test void dvOnly() { stubLife(l -> l.setDvStalkingSexualOffense("NO")); assertNotNull(service.getIdentity(user).safety()); }
        @Test void registryOnly() { stubLife(l -> l.setGovernmentOffenderRegistry("NO")); assertNotNull(service.getIdentity(user).safety()); }
        @Test void jurisdictionOnly() { stubLife(l -> l.setSafetyJurisdiction("TX")); assertNotNull(service.getIdentity(user).safety()); }
        @Test void yearOnly() { stubLife(l -> l.setSafetyApproxYear(2020)); assertNotNull(service.getIdentity(user).safety()); }
        @Test void resolvedOnly() { stubLife(l -> l.setSafetyCaseResolved(true)); assertNotNull(service.getIdentity(user).safety()); }
        @Test void explanationOnly() { stubLife(l -> l.setSafetyExplanation("n/a")); assertNotNull(service.getIdentity(user).safety()); }

        @Test void hasJudgmentOnly() { stubLife(l -> l.setHasJudgment("NO")); assertNotNull(service.getIdentity(user).civilJudgment()); }
        @Test void categoriesOnly() { stubLife(l -> l.setCivilCategories("Contract")); assertNotNull(service.getIdentity(user).civilJudgment()); }
        @Test void civilJurisdictionOnly() { stubLife(l -> l.setCivilJurisdiction("NY")); assertNotNull(service.getIdentity(user).civilJudgment()); }
        @Test void civilYearOnly() { stubLife(l -> l.setCivilApproxYear(2019)); assertNotNull(service.getIdentity(user).civilJudgment()); }
        @Test void civilResolvedOnly() { stubLife(l -> l.setCivilResolved(true)); assertNotNull(service.getIdentity(user).civilJudgment()); }
        @Test void civilExplanationOnly() { stubLife(l -> l.setCivilExplanation("Settled")); assertNotNull(service.getIdentity(user).civilJudgment()); }
    }

    @Nested
    class PartialApplyAndHelpers {
        @Test
        void savePartialCareerFinancialSafetyWithNullEnumFields() {
            CareerDto career = new CareerDto(
                    null, "Engineer", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );
            FinancialDto financial = new FinancialDto(
                    null, null, null, null, null, null,
                    "Goals", null, null, null, null, null, null, null
            );
            SafetyDto safety = new SafetyDto(
                    null, null, null, null, null, "TX", null, null, "note"
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, null, null, null, null, career, financial, safety, null));
            assertNotNull(res.career());
            assertEquals("Engineer", res.career().jobFunction());
            assertNotNull(res.financial());
            assertNotNull(res.safety());
        }

        @Test
        void savePartialImmigrationAndFamilyNullOptionalEnums() {
            ImmigrationDto immigration = new ImmigrationDto(
                    "US", null, null, null, null, java.util.Arrays.asList("  ", "CA", null)
            );
            FamilyDto family = new FamilyDto(
                    null, 1, java.util.Arrays.asList("  ", "0_2", null), "YES", null,
                    null, null, null, null, null, "care", "deps", null
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, immigration, null, family, null, null, null, null, null));
            assertNotNull(res.immigration());
            assertNotNull(res.family());
            assertEquals(1, res.family().childrenCount());
        }

        @Test
        void saveEducationSkipsBlankInstitutionAndBlankLevel() {
            List<EducationDto> educations = List.of(
                    new EducationDto(null, null, null, null, "  ", null, null, null, null, null, null, null, null),
                    new EducationDto(null, "  ", "BS", "CS", "MIT", null, "US", 2010, 2014, false, null, false, null)
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, null, null, null, educations, null, null, null, null));
            assertEquals(1, res.educations().size());
            assertEquals("MIT", res.educations().get(0).institution());
        }

        @Test
        void saveNationalitySkipsNullAdditionalAndBlankLanguage() {
            NationalityDto nationality = new NationalityDto(
                    "US", "US", "US",
                    java.util.Arrays.asList("  ", "CA", null),
                    java.util.Arrays.asList(new LanguageDto("  ", null), new LanguageDto("en", "NATIVE"))
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, nationality, null, null, null, null, null, null, null, null));
            assertNotNull(res.nationality());
            assertTrue(res.nationality().additionalNationalities().contains("CA"));
            assertEquals(1, res.nationality().languages().size());
        }

        @Test
        void saveResidenceWithBlankTypeAndRelocateAndPreferredLocations() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "Austin", "TX", "78701", "US",
                    "  ", 1, 2020, null, "  ", null, java.util.Arrays.asList("  ", "Dallas", null)
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(residence, null, null, null, null, null, null, null, null, null));
            assertNotNull(res.currentResidence());
            assertEquals(1, res.currentResidence().preferredFutureLocations().size());
        }

        @Test
        void saveCivilWithNullHasJudgment() {
            CivilJudgmentDto civil = new CivilJudgmentDto(null, "Contract", "NY", 2018, true, "ok");
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, null, null, null, null, null, null, null, civil));
            assertNotNull(res.civilJudgment());
            assertEquals("Contract", res.civilJudgment().categories());
        }

        @Test
        void previousAddressJsonCoversAsLongAsIntegerAndAsBooleanString() {
            stubBg(bg -> bg.setPreviousAddresses("""
                    [{"id":5,"city":"NYC","stateRegion":"NY","countryCode":"US","postalCode":"10001",
                    "fromMonth":1,"fromYear":"2018","toMonth":"5","toYear":2020,"reasonForMoving":"Job"},
                    {"id":"not-a-number","city":"LA","fromMonth":"x"}]
                    """.trim().replace('\n', ' ')));
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertEquals(2, res.previousAddresses().size());
            assertEquals(5L, res.previousAddresses().get(0).id());
            assertNull(res.previousAddresses().get(1).id());
            assertEquals(2018, res.previousAddresses().get(0).fromYear());
        }

        @Test
        void consentAcceptedAsStringBooleanAndWrongVersionIgnored() {
            UserIdentityCompliance compliance = new UserIdentityCompliance();
            compliance.setId(10L);
            compliance.setUser(user);
            compliance.setConsents("""
                    [{"documentVersion":"old","accepted":true,"acceptedAt":"2020-01-01T00:00:00","createdAt":"2020-01-01T00:00:00"},
                     {"documentVersion":"%s","accepted":"true","acceptedAt":"2024-01-01T00:00:00","createdAt":"2024-01-01T00:00:00"}]
                    """.formatted(OnboardingSteps.BACKGROUND_CONSENT_DOCUMENT_VERSION));
            when(complianceRepository.findByUserId(1L)).thenReturn(Optional.of(compliance));

            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.backgroundConsent());
            assertTrue(res.backgroundConsent().accepted());
        }

        @Test
        void getReferenceDataUsesCacheAndHandlesCountryWithoutPostalRegex() {
            RefCountry xx = new RefCountry();
            xx.setCode("XX");
            xx.setName("No Regex Land");
            xx.setActive(true);
            when(refCountryRepository.findByActiveTrue()).thenReturn(List.of(xx));

            Map<String, Object> first = service.getReferenceData();
            Map<String, Object> second = service.getReferenceData();
            assertSame(first, second);
            verify(refCountryRepository, times(1)).findByActiveTrue();
        }

        @Test
        void preferredNameBlankFallsBackToFirstName() {
            user.setPreferredName("   ");
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertEquals("Ada", res.displayName());
        }

        @Test
        void readStringListNullJsonAndInvalidJson() {
            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setPreferredRelocateLocations("not-json");
                bg.setAdditionalNationalities(null);
            });
            IdentityBackgroundResponse res = service.getIdentity(user);
            assertNotNull(res.currentResidence());
            assertTrue(res.currentResidence().preferredFutureLocations().isEmpty());
        }

        @Test
        void validatePostalCodeCountryWithoutRegexPasses() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "City", "ST", "ANY", "XX",
                    "RENT", 1, 2020, null, "NO", null, List.of()
            );
            assertDoesNotThrow(() -> service.saveIdentity(user,
                    saveLaterWith(residence, null, null, null, null, null, null, null, null, null)));
        }

        @Test
        void previousAddressYearRangeWithNullMonths() {
            List<PreviousAddressDto> previous = List.of(
                    new PreviousAddressDto(null, "NYC", "NY", "US", "10001",
                            null, 2018, null, 2019, "Move")
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, previous, null, null, null,
                    null, null, null, null, null, null,
                    null, null
            );
            assertDoesNotThrow(() -> service.saveIdentity(user, req));
        }

        @Test
        void requireContinueMissingLifeProfile() {
            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setCity("Austin");
                bg.setStateRegion("TX");
                bg.setPostalCode("78701");
                bg.setCountryCode("US");
            });
            when(lifeProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void relationshipDivorcesExceedingMarriagesRejected() {
            RelationshipDto relationship = new RelationshipDto(
                    "DIVORCED", 1, 2, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user,
                    saveLaterWith(null, null, null, relationship, null, null, null, null, null, null)));
        }

        @Test
        void educationGraduationBeforeStartRejected() {
            List<EducationDto> educations = List.of(
                    new EducationDto(null, "BACHELOR", "BS", "CS", "MIT", null, "US",
                            2018, 2014, false, null, false, null)
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user,
                    saveLaterWith(null, null, null, null, null, educations, null, null, null, null)));
        }

        @Test
        void careerBlankEmploymentStatusSkipped() {
            CareerDto career = new CareerDto(
                    "  ", "Engineer", null, null, null, null, null, null,
                    null, null, null, null, null, null, "Acme", false
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, null, null, null, null, career, null, null, null));
            assertNotNull(res.career());
            assertEquals("Engineer", res.career().jobFunction());
        }

        @Test
        void getReferenceDataRefreshesAfterTtlExpiry() {
            Map<String, Object> first = service.getReferenceData();
            ReflectionTestUtils.setField(service, "referenceDataCachedAtMs", System.currentTimeMillis() - 6 * 60 * 1000L);
            Map<String, Object> second = service.getReferenceData();
            assertNotSame(first, second);
            verify(refCountryRepository, times(2)).findByActiveTrue();
        }

        @Test
        void validateAndApplyCoversVisibilityContactAndPhoneChange() {
            user.setPhoneNumber("+919999999999");
            user.setPhoneVerified(true);
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", "Marie", "Lovelace", "None", "Addie",
                    LocalDate.of(1990, 1, 1), "She/Her", "Woman", "public",
                    "ADA@Example.COM", "+919876543210", null, null,
                    "email", "evening",
                    null, null, null, null, null, null, null, null, null, null, null,
                    null, null
            );
            IdentityBackgroundResponse res = service.saveIdentity(user, req);
            assertEquals("Addie", res.displayName());
            assertFalse(user.isPhoneVerified());
            assertEquals("ada@example.com", user.getSecondaryEmail());
            assertEquals("EMAIL", user.getPreferredContactMethod());
            assertEquals("EVENING", user.getBestTimeToContact());
        }

        @Test
        void continueRejectsUnderageAndFutureDobAndMissingNames() {
            IdentityBackgroundRequest underage = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.now().minusYears(10), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(com.SocialPairly_Workflow_Manager.exception.UnprocessableEntityException.class,
                    () -> service.saveIdentity(user, underage));

            IdentityBackgroundRequest future = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.now().plusDays(1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, future));

            IdentityBackgroundRequest missingName = new IdentityBackgroundRequest(
                    "CONTINUE", null, null, null, null, null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, missingName));
        }

        @Test
        void validateAllowlistCaseInsensitiveAndNameTooLong() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", "mr.", "Ada", null, "Lovelace", "jr.", null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertDoesNotThrow(() -> service.saveIdentity(user, req));

            String longName = "A".repeat(61);
            IdentityBackgroundRequest tooLong = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, longName, null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, tooLong));
        }

        @Test
        void relationshipBlankMaritalStatusSkippedAndEducationBlankLevelSetNull() {
            RelationshipDto relationship = new RelationshipDto(
                    "  ", 0, 0, null, null, null, null, null, null, null
            );
            List<EducationDto> educations = List.of(
                    new EducationDto(null, "bachelor", "BS", "CS", "MIT", null, "US",
                            2010, 2014, true, null, true, 9L)
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, null, relationship, null, educations, null, null, null, null));
            assertEquals(1, res.educations().size());
            assertEquals("BACHELOR", res.educations().get(0).educationLevel());
        }

        @Test
        void requireContinueRejectsEachMissingResidenceField() {
            IdentityBackgroundRequest cont = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            stubBg(bg -> {
                bg.setLine1(null);
                bg.setCity("Austin");
                bg.setStateRegion("TX");
                bg.setPostalCode("78701");
                bg.setCountryCode("US");
            });
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, cont));

            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setCity(null);
                bg.setStateRegion("TX");
                bg.setPostalCode("78701");
                bg.setCountryCode("US");
            });
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, cont));

            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setCity("Austin");
                bg.setStateRegion(null);
                bg.setPostalCode("78701");
                bg.setCountryCode("US");
            });
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, cont));

            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setCity("Austin");
                bg.setStateRegion("TX");
                bg.setPostalCode(null);
                bg.setCountryCode("US");
            });
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, cont));
        }

        @Test
        void postalCodeMismatchAndUnsupportedCountry() {
            CurrentResidenceDto badPostal = new CurrentResidenceDto(
                    "1 Main", null, null, "Austin", "TX", "BAD", "US",
                    "RENT", 1, 2020, null, "NO", null, List.of()
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user,
                    saveLaterWith(badPostal, null, null, null, null, null, null, null, null, null)));

            CurrentResidenceDto unknownCountry = new CurrentResidenceDto(
                    "1 Main", null, null, "City", "ST", "12345", "ZZ",
                    "RENT", 1, 2020, null, "NO", null, List.of()
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user,
                    saveLaterWith(unknownCountry, null, null, null, null, null, null, null, null, null)));
        }

        @Test
        void samePrimaryPhoneDoesNotClearVerification() {
            user.setPhoneNumber("+91-9876543210");
            user.setPhoneVerified(true);
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, "",
                    null, "+919876543210", null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            service.saveIdentity(user, req);
            assertTrue(user.isPhoneVerified());
        }

        @Test
        void nationalityDuplicatePrimaryInAdditionalRejected() {
            NationalityDto nationality = new NationalityDto(
                    "US", "US", "US", List.of("US"), List.of()
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user,
                    saveLaterWith(null, nationality, null, null, null, null, null, null, null, null)));
        }

        @Test
        void previousAddressInvalidYearRangeRejected() {
            List<PreviousAddressDto> previous = List.of(
                    new PreviousAddressDto(null, "NYC", "NY", "US", "10001",
                            6, 2020, 1, 2019, "Move")
            );
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, "Ada", null, "Lovelace", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null,
                    null, previous, null, null, null,
                    null, null, null, null, null, null,
                    null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, req));
        }

        @Test
        void continueRequiresBothNamesIndividually() {
            IdentityBackgroundRequest missingFirst = new IdentityBackgroundRequest(
                    "CONTINUE", null, null, null, "Lovelace", null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, missingFirst));

            IdentityBackgroundRequest missingLast = new IdentityBackgroundRequest(
                    "CONTINUE", null, "Ada", null, null, null, null,
                    LocalDate.of(1990, 1, 1), null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user, missingLast));
        }

        @Test
        void saveLaterAllowsNullNamesAndClearsGenderWhenBlankProvided() {
            IdentityBackgroundRequest req = new IdentityBackgroundRequest(
                    "SAVE_LATER", null, null, null, null, null, null,
                    null, null, "  ", null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertDoesNotThrow(() -> service.saveIdentity(user, req));
            assertNull(profile.getGender());
        }

        @Test
        void residenceDurationInvalidMonthAndBlankPostalRegex() {
            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setMoveInMonth(0);
                bg.setMoveInYear(2020);
            });
            assertNull(service.getIdentity(user).currentResidence().residenceDurationMonths());

            stubBg(bg -> {
                bg.setLine1("1 Main");
                bg.setMoveInMonth(13);
                bg.setMoveInYear(2020);
            });
            assertNull(service.getIdentity(user).currentResidence().residenceDurationMonths());

            RefCountry blankRegex = new RefCountry();
            blankRegex.setCode("BR");
            blankRegex.setName("Blank Regex");
            blankRegex.setPostalRegex("  ");
            blankRegex.setActive(true);
            when(refCountryRepository.findById("BR")).thenReturn(Optional.of(blankRegex));

            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "City", "ST", "ANYTHING", "BR",
                    "RENT", 1, 2020, null, "NO", null, List.of()
            );
            assertDoesNotThrow(() -> service.saveIdentity(user,
                    saveLaterWith(residence, null, null, null, null, null, null, null, null, null)));
        }

        @Test
        void relationshipDivorcesWithNullMarriagesAllowedAndDuplicateNationalityCodes() {
            RelationshipDto relationship = new RelationshipDto(
                    "DIVORCED", null, 2, null, null, null, null, null, null, null
            );
            assertDoesNotThrow(() -> service.saveIdentity(user,
                    saveLaterWith(null, null, null, relationship, null, null, null, null, null, null)));

            NationalityDto nationality = new NationalityDto(
                    "US", "US", "US", List.of("CA", "CA"), List.of()
            );
            assertThrows(BadRequestException.class, () -> service.saveIdentity(user,
                    saveLaterWith(null, nationality, null, null, null, null, null, null, null, null)));
        }

        @Test
        void educationLevelOnlyStartYearAndBlankLevel() {
            List<EducationDto> educations = List.of(
                    new EducationDto(null, null, "BS", "CS", "MIT", null, null,
                            2010, null, null, null, null, null)
            );
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(null, null, null, null, null, educations, null, null, null, null));
            assertEquals(1, res.educations().size());
            assertNull(res.educations().get(0).educationLevel());
            assertEquals(2010, res.educations().get(0).startYear());
        }

        @Test
        void resolveCountryNameFallsBackToCode() {
            CurrentResidenceDto residence = new CurrentResidenceDto(
                    "1 Main", null, null, "City", "ST", null, "ZZ",
                    null, null, null, null, null, null, null
            );
            when(refCountryRepository.findById("ZZ")).thenReturn(Optional.empty());
            // countryCode alone without postal skips postal validation
            IdentityBackgroundResponse res = service.saveIdentity(user,
                    saveLaterWith(residence, null, null, null, null, null, null, null, null, null));
            assertEquals("ZZ", profile.getLocationCountry());
            assertNotNull(res.currentResidence());
        }
    }
}
