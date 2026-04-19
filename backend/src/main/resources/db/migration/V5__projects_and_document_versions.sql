-- Projects workspace (P4) + document versions

CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    course_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_phase VARCHAR(20) NOT NULL DEFAULT 'LITERATURE',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_projects_course (course_id),
    INDEX idx_projects_owner (owner_id),
    CONSTRAINT fk_projects_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE SET NULL,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE project_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member (project_id, user_id),
    INDEX idx_pm_user (user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE project_requirements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    added_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_requirement (project_id, requirement_id),
    INDEX idx_pr_requirement (requirement_id),
    CONSTRAINT fk_pr_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_requirement FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE CASCADE
);

CREATE TABLE project_literatures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    literature_id BIGINT NOT NULL,
    added_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_literature (project_id, literature_id),
    INDEX idx_pl_literature (literature_id),
    CONSTRAINT fk_pl_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pl_literature FOREIGN KEY (literature_id) REFERENCES literature(id) ON DELETE CASCADE
);

CREATE TABLE project_ethics_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    ethics_module_id BIGINT NOT NULL,
    added_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_em (project_id, ethics_module_id),
    INDEX idx_pem_module (ethics_module_id),
    CONSTRAINT fk_pem_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pem_module FOREIGN KEY (ethics_module_id) REFERENCES ethics_module(id) ON DELETE CASCADE
);

CREATE TABLE document_versions (
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
);
