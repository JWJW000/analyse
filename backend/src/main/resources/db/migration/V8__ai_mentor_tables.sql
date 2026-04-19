-- V8: AI Mentor Tables
-- Supporting tables for AI Mentor functionality

-- AI Mentor Conversation History
CREATE TABLE mentor_conversation (
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
CREATE TABLE mentor_suggestion_log (
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

-- Project Phase Checklist Enhancement
-- Add completion hints and tips to existing checklist items
ALTER TABLE project_phase_checklist_item
    ADD COLUMN ai_tip TEXT NULL COMMENT 'AI mentor specific tip for this checklist item';

-- Add project context for AI
ALTER TABLE projects
    ADD COLUMN ai_context JSON NULL COMMENT 'AI mentor context cache';

-- Add fields to track student learning progress
ALTER TABLE submissions
    ADD COLUMN ai_analysis TEXT NULL COMMENT 'AI analysis of the submission';

-- Add feedback iteration tracking
ALTER TABLE comments
    ADD COLUMN iteration INT DEFAULT 1 COMMENT 'Feedback iteration number';
