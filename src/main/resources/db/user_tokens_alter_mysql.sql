-- Adds token balance column for reaction/wave spending.
-- Safe to skip if you get Error 1060 Duplicate column name.

ALTER TABLE users_details
    ADD COLUMN user_tokens INT NOT NULL DEFAULT 50;
