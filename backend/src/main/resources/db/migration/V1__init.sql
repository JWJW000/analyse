-- Core users (design doc: user)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ethics modules (design: ethics_module; avoid reserved word `case`)
CREATE TABLE ethics_module (
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
);

CREATE TABLE ethics_module_revision (
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
);

-- Literature
CREATE TABLE literature (
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
);

-- Courses & teaching
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64),
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student (course_id, student_id),
    CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assign_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_assign_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Requirements (design: requirement) + diagram + wizard JSON
CREATE TABLE requirement (
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
);

CREATE TABLE submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    teacher_comment TEXT,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    UNIQUE KEY uk_assign_student (assignment_id, student_id),
    CONSTRAINT fk_sub_assign FOREIGN KEY (assignment_id) REFERENCES assignment(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_req FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE CASCADE
);

-- Audit & AI match stats
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64),
    entity_id BIGINT,
    detail_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE match_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    requirement_id BIGINT,
    top_module_ids_json TEXT,
    scores_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_me_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_me_req FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE SET NULL
);

CREATE TABLE system_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value TEXT
);

CREATE TABLE backup_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_path VARCHAR(1024) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) DEFAULT 'COMPLETED',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_backup_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO system_config (config_key, config_value) VALUES
('ai.service.url', 'http://localhost:8001'),
('backup.path', '/data/backups'),
('backup.retention_days', '30');
