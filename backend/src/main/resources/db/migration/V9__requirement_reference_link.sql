CREATE TABLE IF NOT EXISTS requirement_reference_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id BIGINT NOT NULL,
    reference_id BIGINT NOT NULL,
    evidence_note TEXT NOT NULL,
    confidence DOUBLE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_requirement_reference_link (requirement_id, reference_id),
    CONSTRAINT fk_rrl_requirement FOREIGN KEY (requirement_id) REFERENCES requirement(id) ON DELETE CASCADE,
    CONSTRAINT fk_rrl_reference FOREIGN KEY (reference_id) REFERENCES literature(id) ON DELETE CASCADE
);
