-- =============================================================================
-- Add religion + preferred_religion on user_profiles_details (no new table)
-- Target: socialpairly_dev (or prod)
-- Safe to skip a column if you get Error 1060 Duplicate column name
-- =============================================================================

ALTER TABLE user_profiles_details
  ADD COLUMN religion VARCHAR(60) NULL,
  ADD COLUMN preferred_religion VARCHAR(60) NULL;

-- Verify:
-- SHOW COLUMNS FROM user_profiles_details LIKE '%religion%';
