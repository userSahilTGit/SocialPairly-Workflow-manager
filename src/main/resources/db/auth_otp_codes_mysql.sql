-- Durable auth OTP codes (email verify + password reset)
-- Target DB: socialpairly_dev (or prod). Safe to re-run (IF NOT EXISTS).

CREATE TABLE IF NOT EXISTS auth_otp_codes (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  purpose       VARCHAR(40)  NOT NULL,
  -- EMAIL_VERIFY | PASSWORD_RESET
  identifier    VARCHAR(120) NOT NULL,
  code_hash     VARCHAR(100) NOT NULL,
  expires_at    DATETIME(6)  NOT NULL,
  verified      BIT(1)       NOT NULL DEFAULT 0,
  consumed_at   DATETIME(6)  NULL,
  attempt_count INT          NOT NULL DEFAULT 0,
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_otp_lookup (purpose, identifier, expires_at),
  KEY idx_otp_active (purpose, identifier, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
