-- =============================================================================
-- Identity consolidate v2 — CREATE / MIGRATE / DROP
-- Target DB: socialpairly_dev (or prod)
-- Run order: 1 CREATE, then 2 MIGRATE, then 3 DROP
-- Keep: ref_countries, users_details, user_profiles_details, educations, personality tables
-- Account delete: CASCADE users_details -> background/life/compliance -> blobs
-- =============================================================================

-- =============================================================================
-- Identity consolidate v2 — CREATE
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_identity_background (
  id                              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                         BIGINT       NOT NULL,
  -- current residence
  line1                           VARCHAR(200) NULL,
  line2                           VARCHAR(200) NULL,
  unit                            VARCHAR(60)  NULL,
  city                            VARCHAR(120) NULL,
  state_region                    VARCHAR(120) NULL,
  postal_code                     VARCHAR(20)  NULL,
  country_code                    CHAR(2)      NULL,
  residence_type                  VARCHAR(40)  NULL,
  move_in_month                   TINYINT      NULL,
  move_in_year                    SMALLINT     NULL,
  willing_to_relocate             VARCHAR(40)  NULL,
  event_travel_radius_miles          INT          NULL,
  -- nationality
  country_of_birth                CHAR(2)      NULL,
  primary_nationality             CHAR(2)      NULL,
  country_of_citizenship          CHAR(2)      NULL,
  -- immigration
  current_country_of_residence    CHAR(2)      NULL,
  residency_category              VARCHAR(40)  NULL,
  international_relocation_pref   VARCHAR(40)  NULL,
  future_sponsorship_required     VARCHAR(40)  NULL,
  open_to_partner_abroad          VARCHAR(40)  NULL,
  -- JSON lists (was 1:N child tables)
  preferred_relocate_locations    JSON         NULL,  -- ["Austin TX", ...]
  previous_addresses              JSON         NULL,  -- [{city,stateRegion,countryCode,postalCode,fromMonth,fromYear,toMonth,toYear,reasonForMoving}]
  additional_nationalities        JSON         NULL,  -- ["CA","IN"]
  languages                       JSON         NULL,  -- [{languageCode,proficiency}]
  preferred_future_countries      JSON         NULL,  -- ["US","CA"]
  created_at                      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_uib_user (user_id),
  CONSTRAINT fk_uib_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_uib_country
    FOREIGN KEY (country_code) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_uib_birth
    FOREIGN KEY (country_of_birth) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_uib_nationality
    FOREIGN KEY (primary_nationality) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_uib_citizenship
    FOREIGN KEY (country_of_citizenship) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_uib_imm_country
    FOREIGN KEY (current_country_of_residence) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_life_profile (
  id                                   BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                              BIGINT       NOT NULL,
  -- relationship
  marital_status                       VARCHAR(40)  NULL,
  previous_marriages_count             INT          NULL,
  divorces_count                       INT          NULL,
  annulments_count                     INT          NULL,
  currently_separated                  BIT(1)       NULL,
  divorce_finalized                    BIT(1)       NULL,
  most_recent_divorce_year             SMALLINT     NULL,
  co_parenting                         VARCHAR(40)  NULL,
  unresolved_commitments               VARCHAR(255) NULL,
  relationship_model_pref              VARCHAR(60)  NULL,
  -- family
  has_children                         VARCHAR(40)  NULL,
  children_count                       INT          NULL,
  children_live_with_user              VARCHAR(40)  NULL,
  custody_arrangement                  VARCHAR(80)  NULL,
  future_children_pref                 VARCHAR(40)  NULL,
  open_to_partner_with_children        VARCHAR(40)  NULL,
  preferred_future_children_count      INT          NULL,
  adoption_pref                        VARCHAR(40)  NULL,
  foster_pref                          VARCHAR(40)  NULL,
  elder_care                           VARCHAR(40)  NULL,
  other_dependents                     VARCHAR(255) NULL,
  pets_info                            VARCHAR(255) NULL,
  child_age_ranges                     JSON         NULL,  -- ["0_2","6_9"]
  -- career
  employment_status                    VARCHAR(40)  NULL,
  job_function                         VARCHAR(120) NULL,
  industry                             VARCHAR(120) NULL,
  seniority                            VARCHAR(40)  NULL,
  company_size                         VARCHAR(40)  NULL,
  work_arrangement                     VARCHAR(40)  NULL,
  work_schedule                        VARCHAR(40)  NULL,
  self_employment_category             VARCHAR(60)  NULL,
  years_in_profession                  INT          NULL,
  career_satisfaction                  VARCHAR(40)  NULL,
  travel_frequency                     VARCHAR(40)  NULL,
  relocation_possibility               VARCHAR(40)  NULL,
  career_ambitions                     VARCHAR(500) NULL,
  work_life_balance_pref               VARCHAR(40)  NULL,
  employer_name                        VARCHAR(150) NULL,
  show_employer_publicly               BIT(1)       NOT NULL DEFAULT 0,
  -- financial
  income_range                         VARCHAR(40)  NULL,
  credit_score_range                   VARCHAR(40)  NULL,
  savings_range                        VARCHAR(40)  NULL,
  housing_status                       VARCHAR(40)  NULL,
  general_debt_range                   VARCHAR(40)  NULL,
  student_loan_range                   VARCHAR(40)  NULL,
  financial_goals                      VARCHAR(500) NULL,
  savings_habits                       VARCHAR(80)  NULL,
  spending_style                       VARCHAR(80)  NULL,
  budget_consciousness                 VARCHAR(40)  NULL,
  joint_finance_pref                   VARCHAR(40)  NULL,
  separate_finance_pref                VARCHAR(40)  NULL,
  household_contribution_expectation   VARCHAR(80)  NULL,
  extended_family_support_pref         VARCHAR(80)  NULL,
  -- safety
  criminal_conviction                  VARCHAR(40)  NULL,
  pending_criminal_cases               VARCHAR(40)  NULL,
  protective_restraining_order         VARCHAR(40)  NULL,
  dv_stalking_sexual_offense           VARCHAR(40)  NULL,
  government_offender_registry         VARCHAR(40)  NULL,
  safety_jurisdiction                  VARCHAR(120) NULL,
  safety_approx_year                   SMALLINT     NULL,
  safety_case_resolved                 BIT(1)       NULL,
  safety_explanation                   TEXT         NULL,
  -- civil judgment
  has_judgment                         VARCHAR(40)  NULL,
  civil_categories                     VARCHAR(255) NULL,
  civil_jurisdiction                   VARCHAR(120) NULL,
  civil_approx_year                    SMALLINT     NULL,
  civil_resolved                       BIT(1)       NULL,
  civil_explanation                    TEXT         NULL,
  created_at                           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ulp_user (user_id),
  CONSTRAINT fk_ulp_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_identity_compliance (
  id                     BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                BIGINT       NOT NULL,
  -- verification
  provider               VARCHAR(40)  NOT NULL DEFAULT 'INTERNAL_V1',
  session_id             VARCHAR(100) NULL,
  overall_status         VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  name_status            VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  age_status             VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  photo_status           VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  ssn                    VARCHAR(11)  NULL,
  dl_front_blob_id       BIGINT       NULL,
  dl_back_blob_id        BIGINT       NULL,
  last_error_code        VARCHAR(60)  NULL,
  -- JSON histories
  documents_meta         JSON         NULL,  -- [{id,docPurpose,contentType,fileSizeKb,createdAt}]
  consents               JSON         NULL,  -- [{documentVersion,accepted,acceptedAt,acceptedIp,userAgentHash,createdAt}]
  audit_events           JSON         NULL,  -- [{eventType,entityRef,actorUserId,createdAt}] capped in app
  created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_uic_user (user_id),
  KEY idx_uic_session (session_id),
  CONSTRAINT fk_uic_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_identity_compliance_blobs (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  compliance_id   BIGINT        NOT NULL,
  doc_purpose     VARCHAR(40)   NOT NULL,
  content_type    VARCHAR(100)  NOT NULL,
  file_size_kb    INT           NULL,
  storage_blob    LONGBLOB      NULL,
  storage_path    VARCHAR(500)  NULL,
  created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_uicb_compliance (compliance_id),
  KEY idx_uicb_purpose (compliance_id, doc_purpose),
  CONSTRAINT fk_uicb_compliance
    FOREIGN KEY (compliance_id) REFERENCES user_identity_compliance (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- DL FKs after blobs exist
ALTER TABLE user_identity_compliance
  ADD CONSTRAINT fk_uic_dl_front
    FOREIGN KEY (dl_front_blob_id) REFERENCES user_identity_compliance_blobs (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_uic_dl_back
    FOREIGN KEY (dl_back_blob_id) REFERENCES user_identity_compliance_blobs (id) ON DELETE SET NULL;

- A) Background aggregate (one row per user that has any of residence/nationality/immigration)
INSERT INTO user_identity_background (
  user_id, line1, line2, unit, city, state_region, postal_code, country_code,
  residence_type, move_in_month, move_in_year, willing_to_relocate, event_travel_radius_miles,
  country_of_birth, primary_nationality, country_of_citizenship,
  current_country_of_residence, residency_category, international_relocation_pref,
  future_sponsorship_required, open_to_partner_abroad,
  preferred_relocate_locations, previous_addresses, additional_nationalities,
  languages, preferred_future_countries
)
SELECT
  u.id,
  r.line1, r.line2, r.unit, r.city, r.state_region, r.postal_code, r.country_code,
  r.residence_type, r.move_in_month, r.move_in_year, r.willing_to_relocate, r.event_travel_radius_km,
  n.country_of_birth, n.primary_nationality, n.country_of_citizenship,
  i.current_country_of_residence, i.residency_category, i.international_relocation_pref,
  i.future_sponsorship_required, i.open_to_partner_abroad,
  (
    SELECT JSON_ARRAYAGG(urpl.location_label)
    FROM user_relocate_preferred_locations urpl
    WHERE urpl.residence_id = r.id
  ),
  (
    SELECT JSON_ARRAYAGG(JSON_OBJECT(
      'city', pa.city,
      'stateRegion', pa.state_region,
      'countryCode', pa.country_code,
      'postalCode', pa.postal_code,
      'fromMonth', pa.from_month,
      'fromYear', pa.from_year,
      'toMonth', pa.to_month,
      'toYear', pa.to_year,
      'reasonForMoving', pa.reason_for_moving
    ))
    FROM user_previous_addresses pa
    WHERE pa.user_id = u.id
  ),
  (
    SELECT JSON_ARRAYAGG(uan.nationality_code)
    FROM user_additional_nationalities uan
    WHERE uan.nationality_profile_id = n.id
  ),
  (
    SELECT JSON_ARRAYAGG(JSON_OBJECT(
      'languageCode', ul.language_code,
      'proficiency', ul.proficiency
    ))
    FROM user_languages ul
    WHERE ul.nationality_profile_id = n.id
  ),
  (
    SELECT JSON_ARRAYAGG(upfc.country_code)
    FROM user_preferred_future_countries upfc
    WHERE upfc.immigration_profile_id = i.id
  )
FROM users_details u
LEFT JOIN user_current_residence r ON r.user_id = u.id
LEFT JOIN user_nationality_profile n ON n.user_id = u.id
LEFT JOIN user_immigration_profile i ON i.user_id = u.id
WHERE r.id IS NOT NULL OR n.id IS NOT NULL OR i.id IS NOT NULL
   OR EXISTS (SELECT 1 FROM user_previous_addresses pa2 WHERE pa2.user_id = u.id);

-- B) Life profile
INSERT INTO user_life_profile (
  user_id,
  marital_status, previous_marriages_count, divorces_count, annulments_count,
  currently_separated, divorce_finalized, most_recent_divorce_year, co_parenting,
  unresolved_commitments, relationship_model_pref,
  has_children, children_count, children_live_with_user, custody_arrangement,
  future_children_pref, open_to_partner_with_children, preferred_future_children_count,
  adoption_pref, foster_pref, elder_care, other_dependents, pets_info, child_age_ranges,
  employment_status, job_function, industry, seniority, company_size, work_arrangement,
  work_schedule, self_employment_category, years_in_profession, career_satisfaction,
  travel_frequency, relocation_possibility, career_ambitions, work_life_balance_pref,
  employer_name, show_employer_publicly,
  income_range, credit_score_range, savings_range, housing_status, general_debt_range,
  student_loan_range, financial_goals, savings_habits, spending_style, budget_consciousness,
  joint_finance_pref, separate_finance_pref, household_contribution_expectation,
  extended_family_support_pref,
  criminal_conviction, pending_criminal_cases, protective_restraining_order,
  dv_stalking_sexual_offense, government_offender_registry,
  safety_jurisdiction, safety_approx_year, safety_case_resolved, safety_explanation,
  has_judgment, civil_categories, civil_jurisdiction, civil_approx_year,
  civil_resolved, civil_explanation
)
SELECT
  u.id,
  rp.marital_status, rp.previous_marriages_count, rp.divorces_count, rp.annulments_count,
  rp.currently_separated, rp.divorce_finalized, rp.most_recent_divorce_year, rp.co_parenting,
  rp.unresolved_commitments, rp.relationship_model_pref,
  fp.has_children, fp.children_count, fp.children_live_with_user, fp.custody_arrangement,
  fp.future_children_pref, fp.open_to_partner_with_children, fp.preferred_future_children_count,
  fp.adoption_pref, fp.foster_pref, fp.elder_care, fp.other_dependents, fp.pets_info,
  (
    SELECT JSON_ARRAYAGG(car.age_range_code)
    FROM user_child_age_ranges car
    WHERE car.family_profile_id = fp.id
  ),
  cp.employment_status, cp.job_function, cp.industry, cp.seniority, cp.company_size, cp.work_arrangement,
  cp.work_schedule, cp.self_employment_category, cp.years_in_profession, cp.career_satisfaction,
  cp.travel_frequency, cp.relocation_possibility, cp.career_ambitions, cp.work_life_balance_pref,
  cp.employer_name, COALESCE(cp.show_employer_publicly, 0),
  fin.income_range, fin.credit_score_range, fin.savings_range, fin.housing_status, fin.general_debt_range,
  fin.student_loan_range, fin.financial_goals, fin.savings_habits, fin.spending_style, fin.budget_consciousness,
  fin.joint_finance_pref, fin.separate_finance_pref, fin.household_contribution_expectation,
  fin.extended_family_support_pref,
  sd.criminal_conviction, sd.pending_criminal_cases, sd.protective_restraining_order,
  sd.dv_stalking_sexual_offense, sd.government_offender_registry,
  sd.jurisdiction, sd.approx_year, sd.case_resolved, sd.explanation,
  cj.has_judgment, cj.categories, cj.jurisdiction, cj.approx_year, cj.resolved, cj.explanation
FROM users_details u
LEFT JOIN user_relationship_profile rp ON rp.user_id = u.id
LEFT JOIN user_family_profile fp ON fp.user_id = u.id
LEFT JOIN user_career_profile cp ON cp.user_id = u.id
LEFT JOIN user_financial_profile fin ON fin.user_id = u.id
LEFT JOIN user_safety_disclosure sd ON sd.user_id = u.id
LEFT JOIN user_civil_judgment cj ON cj.user_id = u.id
WHERE rp.id IS NOT NULL OR fp.id IS NOT NULL OR cp.id IS NOT NULL
   OR fin.id IS NOT NULL OR sd.id IS NOT NULL OR cj.id IS NOT NULL;

-- C) Compliance parent rows
INSERT INTO user_identity_compliance (
  user_id, provider, session_id, overall_status, name_status, age_status, photo_status,
  ssn, last_error_code, consents, audit_events, documents_meta
)
SELECT
  u.id,
  COALESCE(v.provider, 'INTERNAL_V1'),
  v.session_id,
  COALESCE(v.overall_status, 'NOT_STARTED'),
  COALESCE(v.name_status, 'NOT_STARTED'),
  COALESCE(v.age_status, 'NOT_STARTED'),
  COALESCE(v.photo_status, 'NOT_STARTED'),
  v.ssn,
  v.last_error_code,
  (
    SELECT JSON_ARRAYAGG(JSON_OBJECT(
      'documentVersion', c.document_version,
      'accepted', c.accepted,
      'acceptedAt', c.accepted_at,
      'acceptedIp', c.accepted_ip,
      'userAgentHash', c.user_agent_hash,
      'createdAt', c.created_at
    ))
    FROM background_screening_consents c
    WHERE c.user_id = u.id
  ),
  (
    SELECT JSON_ARRAYAGG(JSON_OBJECT(
      'eventType', a.event_type,
      'entityRef', a.entity_ref,
      'actorUserId', a.actor_user_id,
      'createdAt', a.created_at
    ))
    FROM onboarding_audit_events a
    WHERE a.user_id = u.id
  ),
  (
    SELECT JSON_ARRAYAGG(JSON_OBJECT(
      'legacyId', d.id,
      'docPurpose', d.doc_purpose,
      'contentType', d.content_type,
      'fileSizeKb', d.file_size_kb,
      'createdAt', d.created_at
    ))
    FROM user_private_documents d
    WHERE d.user_id = u.id
  )
FROM users_details u
LEFT JOIN user_identity_verification v ON v.user_id = u.id
WHERE v.id IS NOT NULL
   OR EXISTS (SELECT 1 FROM background_screening_consents c2 WHERE c2.user_id = u.id)
   OR EXISTS (SELECT 1 FROM onboarding_audit_events a2 WHERE a2.user_id = u.id)
   OR EXISTS (SELECT 1 FROM user_private_documents d2 WHERE d2.user_id = u.id);

-- D) Copy document blobs (legacy id preserved in documents_meta for remapping DL FKs)
INSERT INTO user_identity_compliance_blobs (
  compliance_id, doc_purpose, content_type, file_size_kb, storage_blob, storage_path, created_at
)
SELECT
  c.id,
  d.doc_purpose,
  d.content_type,
  d.file_size_kb,
  d.storage_blob,
  d.storage_path,
  d.created_at
FROM user_private_documents d
JOIN user_identity_compliance c ON c.user_id = d.user_id;

-- E) Remap DL front/back blob ids (match by purpose + user via compliance)
UPDATE user_identity_compliance c
JOIN user_identity_verification v ON v.user_id = c.user_id
LEFT JOIN user_identity_compliance_blobs bf
  ON bf.compliance_id = c.id AND bf.doc_purpose IN ('DL_FRONT', 'ID_FRONT')
LEFT JOIN user_identity_compliance_blobs bb
  ON bb.compliance_id = c.id AND bb.doc_purpose IN ('DL_BACK', 'ID_BACK')
SET
  c.dl_front_blob_id = bf.id,
  c.dl_back_blob_id = bb.id;

- Drop DL FKs on verification first if present
ALTER TABLE user_identity_verification DROP FOREIGN KEY fk_uiv_dl_front;
ALTER TABLE user_identity_verification DROP FOREIGN KEY fk_uiv_dl_back;

-- Drop educations FK to private docs if present
ALTER TABLE educations DROP FOREIGN KEY fk_edu_verification_doc;

DROP TABLE IF EXISTS user_relocate_preferred_locations;
DROP TABLE IF EXISTS user_previous_addresses;
DROP TABLE IF EXISTS user_additional_nationalities;
DROP TABLE IF EXISTS user_languages;
DROP TABLE IF EXISTS user_preferred_future_countries;
DROP TABLE IF EXISTS user_child_age_ranges;

DROP TABLE IF EXISTS user_current_residence;
DROP TABLE IF EXISTS user_nationality_profile;
DROP TABLE IF EXISTS user_immigration_profile;
DROP TABLE IF EXISTS user_relationship_profile;
DROP TABLE IF EXISTS user_family_profile;
DROP TABLE IF EXISTS user_career_profile;
DROP TABLE IF EXISTS user_financial_profile;
DROP TABLE IF EXISTS user_safety_disclosure;
DROP TABLE IF EXISTS user_civil_judgment;

DROP TABLE IF EXISTS onboarding_audit_events;
DROP TABLE IF EXISTS background_screening_consents;
DROP TABLE IF EXISTS user_identity_verification;
DROP TABLE IF EXISTS user_private_documents;

-- Optional: re-point educations.verification_document_id to compliance blobs later in app code
-- ALTER TABLE educations
--   ADD CONSTRAINT fk_edu_verification_blob
--     FOREIGN KEY (verification_document_id) REFERENCES user_identity_compliance_blobs (id)
--     ON DELETE SET NULL;
