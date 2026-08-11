-- Event tables reference migration (FK corrected to event_details)
-- Run manually if tables need to be recreated.

CREATE TABLE IF NOT EXISTS event_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    venue_name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    member_count INT DEFAULT 30,
    event_perks JSON,
    status ENUM('draft', 'published', 'completed', 'cancelled') DEFAULT 'draft',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rsvp_status ENUM('pending', 'accepted', 'declined', 'checked_in') DEFAULT 'pending',
    entry_code VARCHAR(50),
    verification_status ENUM('Pending', 'Done') DEFAULT 'Pending',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_user UNIQUE (event_id, user_id),
    CONSTRAINT fk_event_participants_events
        FOREIGN KEY (event_id) REFERENCES event_details(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    reaction_type ENUM('Wave', 'Spark', 'Heart', 'Coffee', 'Priority') NOT NULL,
    tokens_spent INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_no_self_reaction CHECK (from_user_id <> to_user_id),
    CONSTRAINT uq_event_user_reaction UNIQUE (event_id, from_user_id, to_user_id, reaction_type),
    CONSTRAINT fk_event_reactions_event_details
        FOREIGN KEY (event_id) REFERENCES event_details(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_events_status ON event_details(status);
CREATE INDEX idx_events_event_date ON event_details(event_date);
CREATE INDEX idx_event_participants_event ON event_participants(event_id);
CREATE INDEX idx_event_participants_user ON event_participants(user_id);
CREATE INDEX idx_reactions_event ON event_reactions(event_id);
CREATE INDEX idx_reactions_from_user ON event_reactions(event_id, from_user_id);
CREATE INDEX idx_reactions_to_user ON event_reactions(event_id, to_user_id);
CREATE INDEX idx_reactions_mutual_match ON event_reactions(event_id, to_user_id, from_user_id);
