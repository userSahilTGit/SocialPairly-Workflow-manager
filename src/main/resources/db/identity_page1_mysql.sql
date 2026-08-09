-- =============================================================================
-- Identity & Background (Page 1) — MySQL 8
-- Stories 1–6 (ALTER) + stories 7–26 (CREATE / seed / ALTER)
-- Target DB: socialpairly_dev (or prod DB). Parent FK: users_details(id) BIGINT.
--
-- Run order matters. ALTER is NOT idempotent — skip steps that already exist
-- (Error 1060 duplicate column / 1826 duplicate FK name).
-- Pre-flight: SHOW CREATE TABLE users_details\G  (confirm id BIGINT, InnoDB)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- A) Stories 1–6 — ALTER existing tables
-- ---------------------------------------------------------------------------
ALTER TABLE users_details
  ADD COLUMN name_prefix VARCHAR(20) NULL AFTER last_name,
  ADD COLUMN middle_name VARCHAR(60) NULL AFTER name_prefix,
  ADD COLUMN name_suffix VARCHAR(20) NULL AFTER middle_name,
  ADD COLUMN preferred_name VARCHAR(60) NULL AFTER name_suffix,
  ADD COLUMN secondary_email VARCHAR(120) NULL AFTER email,
  ADD COLUMN secondary_phone VARCHAR(20) NULL AFTER phone_number,
  ADD COLUMN home_phone VARCHAR(20) NULL AFTER secondary_phone,
  ADD COLUMN preferred_contact_method VARCHAR(20) NULL,
  ADD COLUMN best_time_to_contact VARCHAR(40) NULL;

ALTER TABLE user_profiles_details
  ADD COLUMN pronouns VARCHAR(40) NULL AFTER gender,
  ADD COLUMN gender_shown_to_matches VARCHAR(20) NULL AFTER pronouns,
  ADD COLUMN identity_page1_completed_at DATETIME NULL AFTER onboarding_step;

UPDATE user_profiles_details
SET gender_shown_to_matches = 'MATCHES'
WHERE gender_shown_to_matches IS NULL;

-- ---------------------------------------------------------------------------
-- B) Stories 7–26 — CREATE TABLES
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ref_countries (
  code          CHAR(2)      NOT NULL,
  name          VARCHAR(120) NOT NULL,
  postal_regex  VARCHAR(120) NULL,
  active        BIT(1)       NOT NULL DEFAULT 1,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_current_residence (
  id                      BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                 BIGINT       NOT NULL,
  line1                   VARCHAR(200) NULL,
  line2                   VARCHAR(200) NULL,
  unit                    VARCHAR(60)  NULL,
  city                    VARCHAR(120) NULL,
  state_region            VARCHAR(120) NULL,
  postal_code             VARCHAR(20)  NULL,
  country_code            CHAR(2)      NULL,
  residence_type          VARCHAR(40)  NULL,
  move_in_month           TINYINT      NULL,
  move_in_year            SMALLINT     NULL,
  willing_to_relocate     VARCHAR(40)  NULL,
  event_travel_radius_km  INT          NULL,
  created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_current_residence_user (user_id),
  CONSTRAINT fk_ucr_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_ucr_country
    FOREIGN KEY (country_code) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT chk_ucr_move_in_month
    CHECK (move_in_month IS NULL OR (move_in_month BETWEEN 1 AND 12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_relocate_preferred_locations (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  residence_id    BIGINT       NOT NULL,
  location_label  VARCHAR(120) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_urpl_residence (residence_id),
  CONSTRAINT fk_urpl_residence
    FOREIGN KEY (residence_id) REFERENCES user_current_residence (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_previous_addresses (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  user_id            BIGINT       NOT NULL,
  city               VARCHAR(120) NULL,
  state_region       VARCHAR(120) NULL,
  country_code       CHAR(2)      NULL,
  postal_code        VARCHAR(20)  NULL,
  from_month         TINYINT      NULL,
  from_year          SMALLINT     NULL,
  to_month           TINYINT      NULL,
  to_year            SMALLINT     NULL,
  reason_for_moving  VARCHAR(255) NULL,
  created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_upa_user (user_id),
  CONSTRAINT fk_upa_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_upa_country
    FOREIGN KEY (country_code) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT chk_upa_from_month
    CHECK (from_month IS NULL OR (from_month BETWEEN 1 AND 12)),
  CONSTRAINT chk_upa_to_month
    CHECK (to_month IS NULL OR (to_month BETWEEN 1 AND 12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_nationality_profile (
  id                      BIGINT      NOT NULL AUTO_INCREMENT,
  user_id                 BIGINT      NOT NULL,
  country_of_birth        CHAR(2)     NULL,
  primary_nationality     CHAR(2)     NULL,
  country_of_citizenship  CHAR(2)     NULL,
  created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_unp_user (user_id),
  CONSTRAINT fk_unp_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_unp_birth
    FOREIGN KEY (country_of_birth) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_unp_nationality
    FOREIGN KEY (primary_nationality) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_unp_citizenship
    FOREIGN KEY (country_of_citizenship) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_additional_nationalities (
  nationality_profile_id  BIGINT  NOT NULL,
  nationality_code        CHAR(2) NOT NULL,
  PRIMARY KEY (nationality_profile_id, nationality_code),
  CONSTRAINT fk_uan_profile
    FOREIGN KEY (nationality_profile_id) REFERENCES user_nationality_profile (id) ON DELETE CASCADE,
  CONSTRAINT fk_uan_code
    FOREIGN KEY (nationality_code) REFERENCES ref_countries (code)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_languages (
  id                      BIGINT      NOT NULL AUTO_INCREMENT,
  nationality_profile_id  BIGINT      NOT NULL,
  language_code           VARCHAR(16) NOT NULL,
  proficiency             VARCHAR(40) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ul_profile_lang (nationality_profile_id, language_code),
  CONSTRAINT fk_ul_profile
    FOREIGN KEY (nationality_profile_id) REFERENCES user_nationality_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_immigration_profile (
  id                              BIGINT      NOT NULL AUTO_INCREMENT,
  user_id                         BIGINT      NOT NULL,
  current_country_of_residence    CHAR(2)     NULL,
  residency_category              VARCHAR(40) NULL,
  -- CITIZEN | PERMANENT_RESIDENT | WORK_AUTHORIZED | STUDENT |
  -- TEMPORARY_RESIDENT | OTHER | PREFER_NOT_TO_SAY
  international_relocation_pref   VARCHAR(40) NULL,
  future_sponsorship_required     VARCHAR(40) NULL,
  open_to_partner_abroad          VARCHAR(40) NULL,
  created_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_uip_user (user_id),
  CONSTRAINT fk_uip_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_uip_country
    FOREIGN KEY (current_country_of_residence) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_preferred_future_countries (
  immigration_profile_id  BIGINT  NOT NULL,
  country_code            CHAR(2) NOT NULL,
  PRIMARY KEY (immigration_profile_id, country_code),
  CONSTRAINT fk_upfc_profile
    FOREIGN KEY (immigration_profile_id) REFERENCES user_immigration_profile (id) ON DELETE CASCADE,
  CONSTRAINT fk_upfc_country
    FOREIGN KEY (country_code) REFERENCES ref_countries (code)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_relationship_profile (
  id                         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                    BIGINT       NOT NULL,
  marital_status             VARCHAR(40)  NULL,
  previous_marriages_count   INT          NULL,
  divorces_count             INT          NULL,
  annulments_count           INT          NULL,
  currently_separated        BIT(1)       NULL,
  divorce_finalized          BIT(1)       NULL,
  most_recent_divorce_year   SMALLINT     NULL,
  co_parenting               VARCHAR(40)  NULL,
  unresolved_commitments     VARCHAR(255) NULL,
  relationship_model_pref    VARCHAR(60)  NULL,
  created_at                 DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                 DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_urp_user (user_id),
  CONSTRAINT fk_urp_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_family_profile (
  id                              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                         BIGINT       NOT NULL,
  has_children                    VARCHAR(40)  NULL,
  children_count                  INT          NULL,
  children_live_with_user         VARCHAR(40)  NULL,
  custody_arrangement             VARCHAR(80)  NULL,
  future_children_pref            VARCHAR(40)  NULL,
  open_to_partner_with_children   VARCHAR(40)  NULL,
  preferred_future_children_count INT          NULL,
  adoption_pref                   VARCHAR(40)  NULL,
  foster_pref                     VARCHAR(40)  NULL,
  elder_care                      VARCHAR(40)  NULL,
  other_dependents                VARCHAR(255) NULL,
  pets_info                       VARCHAR(255) NULL,
  created_at                      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ufp_user (user_id),
  CONSTRAINT fk_ufp_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_child_age_ranges (
  family_profile_id  BIGINT      NOT NULL,
  age_range_code     VARCHAR(20) NOT NULL,
  -- 0_2 | 3_5 | 6_9 | 10_12 | 13_17 | 18_PLUS
  PRIMARY KEY (family_profile_id, age_range_code),
  CONSTRAINT fk_ucar_family
    FOREIGN KEY (family_profile_id) REFERENCES user_family_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_career_profile (
  id                        BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                   BIGINT       NOT NULL,
  employment_status         VARCHAR(40)  NULL,
  job_function              VARCHAR(120) NULL,
  industry                  VARCHAR(120) NULL,
  seniority                 VARCHAR(40)  NULL,
  company_size              VARCHAR(40)  NULL,
  work_arrangement          VARCHAR(40)  NULL,
  work_schedule             VARCHAR(40)  NULL,
  self_employment_category  VARCHAR(60)  NULL,
  years_in_profession       INT          NULL,
  career_satisfaction       VARCHAR(40)  NULL,
  travel_frequency          VARCHAR(40)  NULL,
  relocation_possibility    VARCHAR(40)  NULL,
  career_ambitions          VARCHAR(500) NULL,
  work_life_balance_pref    VARCHAR(40)  NULL,
  employer_name             VARCHAR(150) NULL,
  show_employer_publicly    BIT(1)       NOT NULL DEFAULT 0,
  created_at                DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ucp_user (user_id),
  CONSTRAINT fk_ucp_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_financial_profile (
  id                                   BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                              BIGINT       NOT NULL,
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
  created_at                           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ufin_user (user_id),
  CONSTRAINT fk_ufin_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_safety_disclosure (
  id                              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                         BIGINT       NOT NULL,
  criminal_conviction             VARCHAR(40)  NULL,
  pending_criminal_cases          VARCHAR(40)  NULL,
  protective_restraining_order    VARCHAR(40)  NULL,
  dv_stalking_sexual_offense      VARCHAR(40)  NULL,
  government_offender_registry    VARCHAR(40)  NULL,
  jurisdiction                    VARCHAR(120) NULL,
  approx_year                     SMALLINT     NULL,
  case_resolved                   BIT(1)       NULL,
  explanation                     TEXT         NULL,
  created_at                      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_usd_user (user_id),
  CONSTRAINT fk_usd_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_civil_judgment (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL,
  has_judgment    VARCHAR(40)  NULL,
  categories      VARCHAR(255) NULL,
  jurisdiction    VARCHAR(120) NULL,
  approx_year     SMALLINT     NULL,
  resolved        BIT(1)       NULL,
  explanation     TEXT         NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_ucj_user (user_id),
  CONSTRAINT fk_ucj_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_private_documents (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  user_id        BIGINT        NOT NULL,
  doc_purpose    VARCHAR(40)   NOT NULL,
  -- DL_FRONT | DL_BACK | SELFIE | SAFETY_SUPPORT | CIVIL_SUPPORT | EDU_VERIFY | EMP_VERIFY
  -- (legacy ID_FRONT / ID_BACK still readable)
  content_type   VARCHAR(100)  NOT NULL,
  file_size_kb   INT           NULL,
  storage_blob   LONGBLOB      NULL,
  storage_path   VARCHAR(500)  NULL,
  created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_upd_user (user_id),
  KEY idx_upd_purpose (user_id, doc_purpose),
  CONSTRAINT fk_upd_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_identity_verification (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  user_id               BIGINT       NOT NULL,
  provider              VARCHAR(40)  NOT NULL DEFAULT 'INTERNAL_V1',
  session_id            VARCHAR(100) NULL,
  overall_status        VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  name_status           VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  age_status            VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  photo_status          VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  ssn                   VARCHAR(11)  NULL,
  dl_front_document_id  BIGINT       NULL,
  dl_back_document_id   BIGINT       NULL,
  last_error_code       VARCHAR(60)  NULL,
  created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_uiv_user (user_id),
  KEY idx_uiv_session (session_id),
  CONSTRAINT fk_uiv_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_uiv_dl_front
    FOREIGN KEY (dl_front_document_id) REFERENCES user_private_documents (id) ON DELETE SET NULL,
  CONSTRAINT fk_uiv_dl_back
    FOREIGN KEY (dl_back_document_id) REFERENCES user_private_documents (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS background_screening_consents (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  user_id            BIGINT       NOT NULL,
  document_version   VARCHAR(40)  NOT NULL,
  accepted           BIT(1)       NOT NULL DEFAULT 0,
  accepted_at        DATETIME(6)  NULL,
  accepted_ip        VARCHAR(45)  NULL,
  user_agent_hash    VARCHAR(128) NULL,
  created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_bsc_user (user_id),
  KEY idx_bsc_user_version (user_id, document_version),
  CONSTRAINT fk_bsc_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS onboarding_audit_events (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL,
  actor_user_id   BIGINT       NULL,
  event_type      VARCHAR(60)  NOT NULL,
  -- IDENTITY_SAVE | CONSENT_ACCEPTED | VERIFY_STATUS_CHANGE | ADMIN_SAFETY_VIEW
  entity_ref      VARCHAR(120) NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_oae_user (user_id),
  KEY idx_oae_type_time (event_type, created_at),
  CONSTRAINT fk_oae_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE,
  CONSTRAINT fk_oae_actor
    FOREIGN KEY (actor_user_id) REFERENCES users_details (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- C) Seed DML — ref_countries (ZIP/postal checks)
-- ---------------------------------------------------------------------------
INSERT INTO ref_countries (code, name, postal_regex, active) VALUES
  ('US', 'United States', '^[0-9]{5}(-[0-9]{4})?$', 1),
  ('CA', 'Canada', '^[A-Za-z][0-9][A-Za-z][ ]?[0-9][A-Za-z][0-9]$', 1),
  ('GB', 'United Kingdom', '^(GIR 0AA|[A-Za-z]{1,2}[0-9][A-Za-z0-9]? [0-9][A-Za-z]{2})$', 1),
  ('AE', 'United Arab Emirates', '^[0-9]{5}$', 1),
  ('IN', 'India', '^[1-9][0-9]{5}$', 1) AS new_rows
ON DUPLICATE KEY UPDATE
  name = new_rows.name,
  postal_regex = new_rows.postal_regex,
  active = new_rows.active;

-- ---------------------------------------------------------------------------
-- D) ALTER educations — columns then FKs
--    country_code collation must match ref_countries (utf8mb4_unicode_ci)
--    If Error 3780: MODIFY country_code to that collation, then re-add FK.
-- ---------------------------------------------------------------------------
ALTER TABLE educations
  ADD COLUMN education_level VARCHAR(40) NULL,
  ADD COLUMN city VARCHAR(120) NULL,
  ADD COLUMN country_code CHAR(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  ADD COLUMN currently_studying BIT(1) NOT NULL DEFAULT 0,
  ADD COLUMN honors VARCHAR(255) NULL,
  ADD COLUMN show_institution_publicly BIT(1) NOT NULL DEFAULT 0,
  ADD COLUMN verification_document_id BIGINT NULL;

-- Repair collation if country_code was added earlier without matching collate:
-- ALTER TABLE educations
--   MODIFY COLUMN country_code CHAR(2)
--     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;

ALTER TABLE educations
  ADD CONSTRAINT fk_edu_country
    FOREIGN KEY (country_code) REFERENCES ref_countries (code)
    ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT fk_edu_verification_doc
    FOREIGN KEY (verification_document_id) REFERENCES user_private_documents (id)
    ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- E) ALTER user_profiles_details — verification / privacy rollups
-- ---------------------------------------------------------------------------
ALTER TABLE user_profiles_details
  ADD COLUMN identity_name_verification_status VARCHAR(30) NULL DEFAULT 'NOT_STARTED',
  ADD COLUMN identity_age_verification_status VARCHAR(30) NULL DEFAULT 'NOT_STARTED',
  ADD COLUMN identity_photo_verification_status VARCHAR(30) NULL DEFAULT 'NOT_STARTED',
  ADD COLUMN show_verification_badge BIT(1) NOT NULL DEFAULT 0,
  ADD COLUMN employer_name_publicly_allowed BIT(1) NOT NULL DEFAULT 0,
  ADD COLUMN income_range_share_preference VARCHAR(20) NULL DEFAULT 'PRIVATE';

-- ---------------------------------------------------------------------------
-- F) Personality & Lifestyle (Page 2)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_personality_profile (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  user_id              BIGINT       NOT NULL,
  headline             VARCHAR(200) NULL,
  about_story          TEXT         NULL,
  friend_descriptors   JSON         NULL,  -- ["loyal","funny"]
  proud_of             VARCHAR(255) NULL,
  life_philosophy      VARCHAR(255) NULL,
  personality_traits   JSON         NULL,  -- ["Adventurous","Loyal"]
  interests_hobbies    TEXT         NULL,
  lifestyle_notes      TEXT         NULL,
  relationship_goals   TEXT         NULL,
  first_date_prefs     TEXT         NULL,
  ideal_partner        TEXT         NULL,
  extended_family      TEXT         NULL,
  created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_upp_user (user_id),
  CONSTRAINT fk_upp_user
    FOREIGN KEY (user_id) REFERENCES users_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NOTE: user_friend_descriptors / user_personality_traits folded into JSON above.
-- Profile chip interests live on user_profiles_details.interests (JSON), not user_interests.

-- ---------------------------------------------------------------------------
-- G) SSN / DL verification columns for existing DBs
-- Prefer: identity_ssn_dl_alter_mysql.sql (run once; skip if columns exist).
-- ---------------------------------------------------------------------------
