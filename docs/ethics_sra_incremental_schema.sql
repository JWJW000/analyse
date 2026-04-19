-- ============================================================
-- EthicsSRA 增量 Schema
-- 说明: 如果表已存在会报错，用于在现有数据库上添加新功能
-- 如果要重新创建，先执行 DROP 语句
-- ============================================================
USE ethics_sra;

-- ============================================================
-- 如果需要重新创建，先删除（可选，取消注释即可）
-- ============================================================
-- DROP TABLE IF EXISTS mentor_conversation;
-- DROP TABLE IF EXISTS mentor_suggestion_log;
-- ALTER TABLE projects DROP COLUMN IF EXISTS ai_context;
-- ALTER TABLE submission DROP COLUMN IF EXISTS ai_analysis;
-- ALTER TABLE comments DROP COLUMN IF EXISTS iteration;

-- ============================================================
-- 1. AI 导师对话历史表（如果不存在）
-- ============================================================
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

-- ============================================================
-- 2. AI 导师建议日志表（如果不存在）
-- ============================================================
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

-- ============================================================
-- 3. projects 表添加 AI 上下文缓存字段
-- ============================================================
-- 检查字段是否存在，不存在才添加
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'ethics_sra'
    AND TABLE_NAME = 'projects'
    AND COLUMN_NAME = 'ai_context'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE projects ADD COLUMN ai_context JSON NULL COMMENT \'AI mentor context cache\'',
    'SELECT \'Column ai_context already exists\' as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 4. submission 表添加 AI 分析字段
-- ============================================================
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'ethics_sra'
    AND TABLE_NAME = 'submission'
    AND COLUMN_NAME = 'ai_analysis'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE submission ADD COLUMN ai_analysis TEXT NULL COMMENT \'AI analysis of the submission\'',
    'SELECT \'Column ai_analysis already exists\' as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 5. submission 表添加 score 字段（如果不存在）
-- ============================================================
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'ethics_sra'
    AND TABLE_NAME = 'submission'
    AND COLUMN_NAME = 'score'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE submission ADD COLUMN score DOUBLE NULL',
    'SELECT \'Column score already exists\' as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 6. comments 表添加反馈迭代次数字段
-- ============================================================
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'ethics_sra'
    AND TABLE_NAME = 'comments'
    AND COLUMN_NAME = 'iteration'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE comments ADD COLUMN iteration INT DEFAULT 1 COMMENT \'Feedback iteration number\'',
    'SELECT \'Column iteration already exists\' as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Schema update completed successfully!' as status;
