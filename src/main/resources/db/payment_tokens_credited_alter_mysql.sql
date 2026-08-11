-- Track whether subscription plan tokens were credited for a payment (idempotent fulfillment).
-- Safe to skip if you get Error 1060 Duplicate column name.

ALTER TABLE payments
    ADD COLUMN tokens_credited TINYINT(1) NOT NULL DEFAULT 0;
