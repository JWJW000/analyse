-- Align MySQL column types with Hibernate enum mapping (ddl-auto=validate)

ALTER TABLE projects
    MODIFY COLUMN status ENUM('active','archived','completed') NOT NULL DEFAULT 'active',
    MODIFY COLUMN current_phase ENUM('literature','requirements','ethics','submission','review') NOT NULL DEFAULT 'literature';

ALTER TABLE project_members
    MODIFY COLUMN role ENUM('owner','editor','member','viewer') NOT NULL DEFAULT 'member';
