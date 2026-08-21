-- Account lock / disable fields on users_details.
-- Safe to skip if you get Error 1060 Duplicate column name.

ALTER TABLE users_details
    ADD COLUMN account_locked_until DATETIME(6) NULL,
    ADD COLUMN account_disabled BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN disabled_at DATETIME(6) NULL;

-- Login security audit trail (fraud detection / lock-disable history)
CREATE TABLE IF NOT EXISTS login_security_events (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NULL,
  identifier    VARCHAR(120) NOT NULL,
  event_type    VARCHAR(40)  NOT NULL,
  security_tier VARCHAR(40)  NULL,
  client_ip     VARCHAR(64)  NULL,
  details       VARCHAR(500) NULL,
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_login_sec_user_created (user_id, created_at),
  KEY idx_login_sec_type_created (event_type, created_at),
  KEY idx_login_sec_identifier (identifier, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
