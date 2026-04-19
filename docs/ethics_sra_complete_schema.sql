-- ============================================================
-- EthicsSRA 数据库完整 Schema
-- 生成时间: 2026-04-18
-- 说明: 包含所有表结构和初始数据
-- ============================================================

-- 如果数据库不存在则创建
CREATE DATABASE IF NOT EXISTS ethics_sra DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ethics_sra;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. 思政模块表
-- ============================================================
CREATE TABLE IF NOT EXISTS ethics_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    keywords VARCHAR(512),
    description TEXT,
    case_text TEXT,
    reference VARCHAR(255),
    current_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. 思政模块版本表
-- ============================================================
CREATE TABLE IF NOT EXISTS ethics_module_revision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_id BIGINT NOT NULL,
    version INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    keywords VARCHAR(512),
    description TEXT,
    case_text TEXT,
    reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_module_version (module_id, version),
    CONSTRAINT fk_emr_module FOREIGN KEY (module_id) REFERENCES ethics_module(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. 文献表
-- ============================================================
CREATE TABLE IF NOT EXISTS literature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    author VARCHAR(255),
    source VARCHAR(255),
    abstract_text TEXT,
    keywords VARCHAR(512),
    file_path VARCHAR(512),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lit_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. 课程表
-- ============================================================
CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64),
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. 选课表
-- ============================================================
CREATE TABLE IF NOT EXISTS enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student (course_id, student_id),
    CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. 作业表
-- ============================================================
CREATE TABLE IF NOT EXISTS assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assign_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_assign_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. 需求表
-- ============================================================
CREATE TABLE IF NOT EXISTS requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    text_content LONGTEXT,
    embedded_modules TEXT,
    matching_score DOUBLE,
    diagram_json LONGTEXT,
    spec_wizard_json LONGTEXT,
    course_id BIGINT,
    assignment_id BIGINT,
    status VARCHAR(32) DEFAULT 'DRAFT',
    teacher_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_req_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_assign FOREIGN KEY (assignment_id) REFERENCES assignment(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. 提交表
-- ============================================================
CREATE TABLE IF NOT EXISTS submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    teacher_comment TEXT,
    score DOUBLE NULL,
    ai_analysis TEXT NULL,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    UNIQUE KEY uk_assign_student (assignment_id, student_id),
    CONSTRAINT fk_sub_assign FOREIGN KEY (assignment_id) REFERENCES assignment(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_req FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. 审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64),
    entity_id BIGINT,
    detail_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. AI 匹配事件表
-- ============================================================
CREATE TABLE IF NOT EXISTS match_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    requirement_id BIGINT,
    top_module_ids_json TEXT,
    scores_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_me_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_me_req FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. 备份记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS backup_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_path VARCHAR(1024) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) DEFAULT 'COMPLETED',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_backup_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. 项目表
-- ============================================================
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    course_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    status ENUM('active','archived','completed') NOT NULL DEFAULT 'active',
    current_phase ENUM('literature','requirements','ethics','submission','review') NOT NULL DEFAULT 'literature',
    ai_context JSON NULL COMMENT 'AI mentor context cache',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_projects_course (course_id),
    INDEX idx_projects_owner (owner_id),
    CONSTRAINT fk_projects_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE SET NULL,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. 项目成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS project_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('owner','editor','member','viewer') NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member (project_id, user_id),
    INDEX idx_pm_user (user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 16. 项目需求关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS project_requirements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    added_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_requirement (project_id, requirement_id),
    INDEX idx_pr_requirement (requirement_id),
    CONSTRAINT fk_pr_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_requirement FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 17. 项目文献关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS project_literatures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    literature_id BIGINT NOT NULL,
    added_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_literature (project_id, literature_id),
    INDEX idx_pl_literature (literature_id),
    CONSTRAINT fk_pl_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pl_literature FOREIGN KEY (literature_id) REFERENCES literature(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 18. 项目思政模块关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS project_ethics_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    ethics_module_id BIGINT NOT NULL,
    added_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_em (project_id, ethics_module_id),
    INDEX idx_pem_module (ethics_module_id),
    CONSTRAINT fk_pem_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pem_module FOREIGN KEY (ethics_module_id) REFERENCES ethics_module(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 19. 文档版本表
-- ============================================================
CREATE TABLE IF NOT EXISTS document_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    requirement_id BIGINT NULL,
    version_number INT NULL,
    content LONGTEXT NULL,
    change_summary VARCHAR(255) NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dv_project_created (project_id, created_at),
    INDEX idx_dv_requirement_created (requirement_id, created_at),
    INDEX idx_dv_user_created (user_id, created_at),
    CONSTRAINT fk_dv_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT fk_dv_requirement FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE SET NULL,
    CONSTRAINT fk_dv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 20. 课程讨论帖子表
-- ============================================================
CREATE TABLE IF NOT EXISTS course_discussion_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cdp_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_cdp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cdp_course_created ON course_discussion_post (course_id, created_at DESC);

-- ============================================================
-- 21. 评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    requirement_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NULL,
    parent_id BIGINT NULL,
    iteration INT DEFAULT 1 COMMENT 'Feedback iteration number',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_comments_project_created (project_id, created_at),
    INDEX idx_comments_requirement_created (requirement_id, created_at),
    INDEX idx_comments_user_created (user_id, created_at),
    INDEX idx_comments_parent (parent_id),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 22. AI 导师对话历史表
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
-- 23. AI 导师建议日志表
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
-- 初始数据
-- ============================================================
INSERT INTO system_config (config_key, config_value) VALUES
('ai.service.url', 'http://localhost:8001'),
('backup.path', '/data/backups'),
('backup.retention_days', '30');

-- ============================================================
-- 完成
-- ============================================================
