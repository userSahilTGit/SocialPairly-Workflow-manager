package com.SocialPairly_Workflow_Manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_life_profile")
public class UserLifeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "marital_status", length = 40)
    private String maritalStatus;

    @Column(name = "previous_marriages_count")
    private Integer previousMarriagesCount;

    @Column(name = "divorces_count")
    private Integer divorcesCount;

    @Column(name = "annulments_count")
    private Integer annulmentsCount;

    @Column(name = "currently_separated")
    private Boolean currentlySeparated;

    @Column(name = "divorce_finalized")
    private Boolean divorceFinalized;

    @Column(name = "most_recent_divorce_year")
    private Integer mostRecentDivorceYear;

    @Column(name = "co_parenting", length = 40)
    private String coParenting;

    @Column(name = "unresolved_commitments", length = 255)
    private String unresolvedCommitments;

    @Column(name = "relationship_model_pref", length = 60)
    private String relationshipModelPref;

    @Column(name = "has_children", length = 40)
    private String hasChildren;

    @Column(name = "children_count")
    private Integer childrenCount;

    @Column(name = "children_live_with_user", length = 40)
    private String childrenLiveWithUser;

    @Column(name = "custody_arrangement", length = 80)
    private String custodyArrangement;

    @Column(name = "future_children_pref", length = 40)
    private String futureChildrenPref;

    @Column(name = "open_to_partner_with_children", length = 40)
    private String openToPartnerWithChildren;

    @Column(name = "preferred_future_children_count")
    private Integer preferredFutureChildrenCount;

    @Column(name = "adoption_pref", length = 40)
    private String adoptionPref;

    @Column(name = "foster_pref", length = 40)
    private String fosterPref;

    @Column(name = "elder_care", length = 40)
    private String elderCare;

    @Column(name = "other_dependents", length = 255)
    private String otherDependents;

    @Column(name = "pets_info", length = 255)
    private String petsInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "child_age_ranges", columnDefinition = "json")
    private String childAgeRanges;

    @Column(name = "employment_status", length = 40)
    private String employmentStatus;

    @Column(name = "job_function", length = 120)
    private String jobFunction;

    @Column(name = "industry", length = 120)
    private String industry;

    @Column(name = "seniority", length = 40)
    private String seniority;

    @Column(name = "company_size", length = 40)
    private String companySize;

    @Column(name = "work_arrangement", length = 40)
    private String workArrangement;

    @Column(name = "work_schedule", length = 40)
    private String workSchedule;

    @Column(name = "self_employment_category", length = 60)
    private String selfEmploymentCategory;

    @Column(name = "years_in_profession")
    private Integer yearsInProfession;

    @Column(name = "career_satisfaction", length = 40)
    private String careerSatisfaction;

    @Column(name = "travel_frequency", length = 40)
    private String travelFrequency;

    @Column(name = "relocation_possibility", length = 40)
    private String relocationPossibility;

    @Column(name = "career_ambitions", length = 500)
    private String careerAmbitions;

    @Column(name = "work_life_balance_pref", length = 40)
    private String workLifeBalancePref;

    @Column(name = "employer_name", length = 150)
    private String employerName;

    @Column(name = "employment_type", length = 80)
    private String employmentType;

    @Column(name = "show_employer_publicly", nullable = false)
    private Boolean showEmployerPublicly = false;

    @Column(name = "income_range", length = 40)
    private String incomeRange;

    @Column(name = "credit_score_range", length = 40)
    private String creditScoreRange;

    @Column(name = "savings_range", length = 40)
    private String savingsRange;

    @Column(name = "housing_status", length = 40)
    private String housingStatus;

    @Column(name = "general_debt_range", length = 40)
    private String generalDebtRange;

    @Column(name = "student_loan_range", length = 40)
    private String studentLoanRange;

    @Column(name = "financial_goals", length = 500)
    private String financialGoals;

    @Column(name = "savings_habits", length = 80)
    private String savingsHabits;

    @Column(name = "spending_style", length = 80)
    private String spendingStyle;

    @Column(name = "budget_consciousness", length = 40)
    private String budgetConsciousness;

    @Column(name = "joint_finance_pref", length = 40)
    private String jointFinancePref;

    @Column(name = "separate_finance_pref", length = 40)
    private String separateFinancePref;

    @Column(name = "household_contribution_expectation", length = 80)
    private String householdContributionExpectation;

    @Column(name = "extended_family_support_pref", length = 80)
    private String extendedFamilySupportPref;

    @Column(name = "criminal_conviction", length = 40)
    private String criminalConviction;

    @Column(name = "pending_criminal_cases", length = 40)
    private String pendingCriminalCases;

    @Column(name = "protective_restraining_order", length = 40)
    private String protectiveRestrainingOrder;

    @Column(name = "dv_stalking_sexual_offense", length = 40)
    private String dvStalkingSexualOffense;

    @Column(name = "government_offender_registry", length = 40)
    private String governmentOffenderRegistry;

    @Column(name = "safety_jurisdiction", length = 120)
    private String safetyJurisdiction;

    @Column(name = "safety_approx_year")
    private Integer safetyApproxYear;

    @Column(name = "safety_case_resolved")
    private Boolean safetyCaseResolved;

    @Lob
    @Column(name = "safety_explanation")
    private String safetyExplanation;

    @Column(name = "has_judgment", length = 40)
    private String hasJudgment;

    @Column(name = "civil_categories", length = 255)
    private String civilCategories;

    @Column(name = "civil_jurisdiction", length = 120)
    private String civilJurisdiction;

    @Column(name = "civil_approx_year")
    private Integer civilApproxYear;

    @Column(name = "civil_resolved")
    private Boolean civilResolved;

    @Lob
    @Column(name = "civil_explanation")
    private String civilExplanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.showEmployerPublicly == null) {
            this.showEmployerPublicly = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public Integer getPreviousMarriagesCount() { return previousMarriagesCount; }
    public void setPreviousMarriagesCount(Integer previousMarriagesCount) {
        this.previousMarriagesCount = previousMarriagesCount;
    }

    public Integer getDivorcesCount() { return divorcesCount; }
    public void setDivorcesCount(Integer divorcesCount) { this.divorcesCount = divorcesCount; }

    public Integer getAnnulmentsCount() { return annulmentsCount; }
    public void setAnnulmentsCount(Integer annulmentsCount) { this.annulmentsCount = annulmentsCount; }

    public Boolean getCurrentlySeparated() { return currentlySeparated; }
    public void setCurrentlySeparated(Boolean currentlySeparated) { this.currentlySeparated = currentlySeparated; }

    public Boolean getDivorceFinalized() { return divorceFinalized; }
    public void setDivorceFinalized(Boolean divorceFinalized) { this.divorceFinalized = divorceFinalized; }

    public Integer getMostRecentDivorceYear() { return mostRecentDivorceYear; }
    public void setMostRecentDivorceYear(Integer mostRecentDivorceYear) {
        this.mostRecentDivorceYear = mostRecentDivorceYear;
    }

    public String getCoParenting() { return coParenting; }
    public void setCoParenting(String coParenting) { this.coParenting = coParenting; }

    public String getUnresolvedCommitments() { return unresolvedCommitments; }
    public void setUnresolvedCommitments(String unresolvedCommitments) {
        this.unresolvedCommitments = unresolvedCommitments;
    }

    public String getRelationshipModelPref() { return relationshipModelPref; }
    public void setRelationshipModelPref(String relationshipModelPref) {
        this.relationshipModelPref = relationshipModelPref;
    }

    public String getHasChildren() { return hasChildren; }
    public void setHasChildren(String hasChildren) { this.hasChildren = hasChildren; }

    public Integer getChildrenCount() { return childrenCount; }
    public void setChildrenCount(Integer childrenCount) { this.childrenCount = childrenCount; }

    public String getChildrenLiveWithUser() { return childrenLiveWithUser; }
    public void setChildrenLiveWithUser(String childrenLiveWithUser) {
        this.childrenLiveWithUser = childrenLiveWithUser;
    }

    public String getCustodyArrangement() { return custodyArrangement; }
    public void setCustodyArrangement(String custodyArrangement) { this.custodyArrangement = custodyArrangement; }

    public String getFutureChildrenPref() { return futureChildrenPref; }
    public void setFutureChildrenPref(String futureChildrenPref) { this.futureChildrenPref = futureChildrenPref; }

    public String getOpenToPartnerWithChildren() { return openToPartnerWithChildren; }
    public void setOpenToPartnerWithChildren(String openToPartnerWithChildren) {
        this.openToPartnerWithChildren = openToPartnerWithChildren;
    }

    public Integer getPreferredFutureChildrenCount() { return preferredFutureChildrenCount; }
    public void setPreferredFutureChildrenCount(Integer preferredFutureChildrenCount) {
        this.preferredFutureChildrenCount = preferredFutureChildrenCount;
    }

    public String getAdoptionPref() { return adoptionPref; }
    public void setAdoptionPref(String adoptionPref) { this.adoptionPref = adoptionPref; }

    public String getFosterPref() { return fosterPref; }
    public void setFosterPref(String fosterPref) { this.fosterPref = fosterPref; }

    public String getElderCare() { return elderCare; }
    public void setElderCare(String elderCare) { this.elderCare = elderCare; }

    public String getOtherDependents() { return otherDependents; }
    public void setOtherDependents(String otherDependents) { this.otherDependents = otherDependents; }

    public String getPetsInfo() { return petsInfo; }
    public void setPetsInfo(String petsInfo) { this.petsInfo = petsInfo; }

    public String getChildAgeRanges() { return childAgeRanges; }
    public void setChildAgeRanges(String childAgeRanges) { this.childAgeRanges = childAgeRanges; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public String getJobFunction() { return jobFunction; }
    public void setJobFunction(String jobFunction) { this.jobFunction = jobFunction; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }

    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }

    public String getWorkArrangement() { return workArrangement; }
    public void setWorkArrangement(String workArrangement) { this.workArrangement = workArrangement; }

    public String getWorkSchedule() { return workSchedule; }
    public void setWorkSchedule(String workSchedule) { this.workSchedule = workSchedule; }

    public String getSelfEmploymentCategory() { return selfEmploymentCategory; }
    public void setSelfEmploymentCategory(String selfEmploymentCategory) {
        this.selfEmploymentCategory = selfEmploymentCategory;
    }

    public Integer getYearsInProfession() { return yearsInProfession; }
    public void setYearsInProfession(Integer yearsInProfession) { this.yearsInProfession = yearsInProfession; }

    public String getCareerSatisfaction() { return careerSatisfaction; }
    public void setCareerSatisfaction(String careerSatisfaction) { this.careerSatisfaction = careerSatisfaction; }

    public String getTravelFrequency() { return travelFrequency; }
    public void setTravelFrequency(String travelFrequency) { this.travelFrequency = travelFrequency; }

    public String getRelocationPossibility() { return relocationPossibility; }
    public void setRelocationPossibility(String relocationPossibility) {
        this.relocationPossibility = relocationPossibility;
    }

    public String getCareerAmbitions() { return careerAmbitions; }
    public void setCareerAmbitions(String careerAmbitions) { this.careerAmbitions = careerAmbitions; }

    public String getWorkLifeBalancePref() { return workLifeBalancePref; }
    public void setWorkLifeBalancePref(String workLifeBalancePref) {
        this.workLifeBalancePref = workLifeBalancePref;
    }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Boolean getShowEmployerPublicly() { return showEmployerPublicly; }
    public void setShowEmployerPublicly(Boolean showEmployerPublicly) {
        this.showEmployerPublicly = showEmployerPublicly;
    }

    public String getIncomeRange() { return incomeRange; }
    public void setIncomeRange(String incomeRange) { this.incomeRange = incomeRange; }

    public String getCreditScoreRange() { return creditScoreRange; }
    public void setCreditScoreRange(String creditScoreRange) { this.creditScoreRange = creditScoreRange; }

    public String getSavingsRange() { return savingsRange; }
    public void setSavingsRange(String savingsRange) { this.savingsRange = savingsRange; }

    public String getHousingStatus() { return housingStatus; }
    public void setHousingStatus(String housingStatus) { this.housingStatus = housingStatus; }

    public String getGeneralDebtRange() { return generalDebtRange; }
    public void setGeneralDebtRange(String generalDebtRange) { this.generalDebtRange = generalDebtRange; }

    public String getStudentLoanRange() { return studentLoanRange; }
    public void setStudentLoanRange(String studentLoanRange) { this.studentLoanRange = studentLoanRange; }

    public String getFinancialGoals() { return financialGoals; }
    public void setFinancialGoals(String financialGoals) { this.financialGoals = financialGoals; }

    public String getSavingsHabits() { return savingsHabits; }
    public void setSavingsHabits(String savingsHabits) { this.savingsHabits = savingsHabits; }

    public String getSpendingStyle() { return spendingStyle; }
    public void setSpendingStyle(String spendingStyle) { this.spendingStyle = spendingStyle; }

    public String getBudgetConsciousness() { return budgetConsciousness; }
    public void setBudgetConsciousness(String budgetConsciousness) {
        this.budgetConsciousness = budgetConsciousness;
    }

    public String getJointFinancePref() { return jointFinancePref; }
    public void setJointFinancePref(String jointFinancePref) { this.jointFinancePref = jointFinancePref; }

    public String getSeparateFinancePref() { return separateFinancePref; }
    public void setSeparateFinancePref(String separateFinancePref) {
        this.separateFinancePref = separateFinancePref;
    }

    public String getHouseholdContributionExpectation() { return householdContributionExpectation; }
    public void setHouseholdContributionExpectation(String householdContributionExpectation) {
        this.householdContributionExpectation = householdContributionExpectation;
    }

    public String getExtendedFamilySupportPref() { return extendedFamilySupportPref; }
    public void setExtendedFamilySupportPref(String extendedFamilySupportPref) {
        this.extendedFamilySupportPref = extendedFamilySupportPref;
    }

    public String getCriminalConviction() { return criminalConviction; }
    public void setCriminalConviction(String criminalConviction) { this.criminalConviction = criminalConviction; }

    public String getPendingCriminalCases() { return pendingCriminalCases; }
    public void setPendingCriminalCases(String pendingCriminalCases) {
        this.pendingCriminalCases = pendingCriminalCases;
    }

    public String getProtectiveRestrainingOrder() { return protectiveRestrainingOrder; }
    public void setProtectiveRestrainingOrder(String protectiveRestrainingOrder) {
        this.protectiveRestrainingOrder = protectiveRestrainingOrder;
    }

    public String getDvStalkingSexualOffense() { return dvStalkingSexualOffense; }
    public void setDvStalkingSexualOffense(String dvStalkingSexualOffense) {
        this.dvStalkingSexualOffense = dvStalkingSexualOffense;
    }

    public String getGovernmentOffenderRegistry() { return governmentOffenderRegistry; }
    public void setGovernmentOffenderRegistry(String governmentOffenderRegistry) {
        this.governmentOffenderRegistry = governmentOffenderRegistry;
    }

    public String getSafetyJurisdiction() { return safetyJurisdiction; }
    public void setSafetyJurisdiction(String safetyJurisdiction) { this.safetyJurisdiction = safetyJurisdiction; }

    public Integer getSafetyApproxYear() { return safetyApproxYear; }
    public void setSafetyApproxYear(Integer safetyApproxYear) { this.safetyApproxYear = safetyApproxYear; }

    public Boolean getSafetyCaseResolved() { return safetyCaseResolved; }
    public void setSafetyCaseResolved(Boolean safetyCaseResolved) { this.safetyCaseResolved = safetyCaseResolved; }

    public String getSafetyExplanation() { return safetyExplanation; }
    public void setSafetyExplanation(String safetyExplanation) { this.safetyExplanation = safetyExplanation; }

    public String getHasJudgment() { return hasJudgment; }
    public void setHasJudgment(String hasJudgment) { this.hasJudgment = hasJudgment; }

    public String getCivilCategories() { return civilCategories; }
    public void setCivilCategories(String civilCategories) { this.civilCategories = civilCategories; }

    public String getCivilJurisdiction() { return civilJurisdiction; }
    public void setCivilJurisdiction(String civilJurisdiction) { this.civilJurisdiction = civilJurisdiction; }

    public Integer getCivilApproxYear() { return civilApproxYear; }
    public void setCivilApproxYear(Integer civilApproxYear) { this.civilApproxYear = civilApproxYear; }

    public Boolean getCivilResolved() { return civilResolved; }
    public void setCivilResolved(Boolean civilResolved) { this.civilResolved = civilResolved; }

    public String getCivilExplanation() { return civilExplanation; }
    public void setCivilExplanation(String civilExplanation) { this.civilExplanation = civilExplanation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
