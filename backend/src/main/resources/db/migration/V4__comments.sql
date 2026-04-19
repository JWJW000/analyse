-- Comments (requirement/project discussion)
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    requirement_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NULL,
    parent_id BIGINT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_comments_project_created (project_id, created_at),
    INDEX idx_comments_requirement_created (requirement_id, created_at),
    INDEX idx_comments_user_created (user_id, created_at),
    INDEX idx_comments_parent (parent_id),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
