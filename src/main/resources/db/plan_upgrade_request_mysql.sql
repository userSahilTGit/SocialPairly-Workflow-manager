-- plan_upgrade_request already created in DB.
-- This file documents the expected schema for local/reference use.
-- Safe to skip if the table already exists.

CREATE TABLE IF NOT EXISTS `plan_upgrade_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `current_plan` VARCHAR(50) NOT NULL,
    `upgrade_plan` VARCHAR(50) NOT NULL,
    `reason` TEXT NOT NULL,
    `status` ENUM('Started', 'InProgress', 'Completed', 'Rejected') NOT NULL DEFAULT 'Started',
    `action` ENUM('Requested', 'Approved', 'Closed') NOT NULL DEFAULT 'Requested',
    `extra_token` INT DEFAULT 0,
    `extra_amount` DECIMAL(10, 2) DEFAULT 0.00,
    `payment_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_action` (`action`),
    INDEX `idx_payment_id` (`payment_id`),

    CONSTRAINT `fk_upgrade_request_user`
        FOREIGN KEY (`user_id`) REFERENCES `users_details` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT `fk_upgrade_request_payment`
        FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
