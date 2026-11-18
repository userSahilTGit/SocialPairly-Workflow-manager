-- Employee RBAC tables (reference copy).
-- Safe to re-run: CREATE IF NOT EXISTS + INSERT IGNORE.
-- Environments where tables already exist can skip CREATE and keep seed inserts if needed.

CREATE TABLE IF NOT EXISTS staff_role_assignments (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  email           VARCHAR(120) NOT NULL COMMENT 'Lowercased primary login email',
  user_id         BIGINT       NULL COMMENT 'FK to users_details.id when account exists',
  role_code       VARCHAR(40)  NOT NULL COMMENT 'SUPER_ADMIN, ADMIN, TESTER, FINANCE, ...',
  is_active       BIT(1)       NOT NULL DEFAULT 1,
  assignment_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL'
      COMMENT 'MANUAL | DOMAIN_AUTO | SEED',
  notes           VARCHAR(500) NULL,
  assigned_by_email VARCHAR(120) NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_staff_role_email (email),
  KEY idx_staff_role_code_active (role_code, is_active),
  KEY idx_staff_role_user_id (user_id),
  CONSTRAINT fk_staff_role_user
    FOREIGN KEY (user_id) REFERENCES users_details (id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_dashboard_access (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  role_code       VARCHAR(40)  NOT NULL,
  dashboard_key   VARCHAR(60)  NOT NULL
      COMMENT 'DASHBOARD, QUESTIONS, MODERATION, IDENTITY_DOCS, PLANS, EVENTS, FINANCE, ROLE_ACCESS, CUSTOMER_SUPPORT',
  can_access      BIT(1)       NOT NULL DEFAULT 1,
  updated_by_email VARCHAR(120) NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_dashboard (role_code, dashboard_key),
  KEY idx_role_dashboard_key (dashboard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
