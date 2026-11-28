-- Event unlock / open-enrollment reference (already applied on socialpairly_test).
-- Safe to skip if columns/tables already exist.

ALTER TABLE event_details
  ADD COLUMN IF NOT EXISTS enrollment_type VARCHAR(20) NOT NULL DEFAULT 'SELECTED'
    COMMENT 'OPEN or SELECTED',
  ADD COLUMN IF NOT EXISTS token_cost INT NOT NULL DEFAULT 0
    COMMENT 'Tokens required to book a slot',
  ADD COLUMN IF NOT EXISTS male_slot_limit INT NULL,
  ADD COLUMN IF NOT EXISTS female_slot_limit INT NULL,
  ADD COLUMN IF NOT EXISTS venue_latitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS venue_longitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS notify_radius_miles INT NOT NULL DEFAULT 100,
  ADD COLUMN IF NOT EXISTS matching_opens_at DATETIME NULL
    COMMENT 'Defaults to event_date+event_time on publish if null';

ALTER TABLE event_participants
  ADD COLUMN IF NOT EXISTS booking_tokens_spent INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS invited_via VARCHAR(20) NULL
    COMMENT 'SELECTED, RADIUS, SELF',
  ADD COLUMN IF NOT EXISTS notification_dismissed TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS booked_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS user_event_notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  kind VARCHAR(40) NOT NULL DEFAULT 'EVENT_INVITE',
  title VARCHAR(255) NOT NULL,
  body TEXT,
  seen TINYINT(1) NOT NULL DEFAULT 0,
  dismissed TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_user_event_notif (user_id, event_id, kind),
  INDEX idx_uen_user_open (user_id, dismissed, seen),
  CONSTRAINT fk_uen_event FOREIGN KEY (event_id) REFERENCES event_details(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
