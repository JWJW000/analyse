-- V8: AI Mentor Tables
-- Supporting tables for AI Mentor functionality

-- AI Mentor Conversation History
CREATE TABLE IF NOT EXISTS mentor_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL COMMENT 'user or assistant',
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_project_user (project_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI Mentor Suggestion Log
CREATE TABLE IF NOT EXISTS mentor_suggestion_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    page VARCHAR(32) NOT NULL COMMENT 'current page when suggestion shown',
    suggestion_type VARCHAR(16) NOT NULL COMMENT 'action, warning, tip',
    suggestion_content TEXT NOT NULL,
    shown_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    clicked BOOLEAN DEFAULT FALSE COMMENT 'whether user clicked the suggestion',
    clicked_at TIMESTAMP NULL,
    adopted BOOLEAN DEFAULT FALSE COMMENT 'whether user adopted the suggestion',
    INDEX idx_project (project_id),
    INDEX idx_shown_at (shown_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add project context for AI.
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE projects ADD COLUMN ai_context JSON NULL COMMENT ''AI mentor context cache''',
        'SELECT ''projects.ai_context already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'projects'
      AND COLUMN_NAME = 'ai_context'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add fields to track student learning progress.
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE submission ADD COLUMN ai_analysis TEXT NULL COMMENT ''AI analysis of the submission''',
        'SELECT ''submission.ai_analysis already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'submission'
      AND COLUMN_NAME = 'ai_analysis'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add feedback iteration tracking.
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE comments ADD COLUMN iteration INT DEFAULT 1 COMMENT ''Feedback iteration number''',
        'SELECT ''comments.iteration already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'iteration'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
