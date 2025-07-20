-- =================================================================
-- Table Creation
-- =================================================================

CREATE TABLE IF NOT EXISTS user_entity (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    last_password_change TIMESTAMP NOT NULL,
    role VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_info (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS element_entity (
    id UUID PRIMARY KEY,
    is_folder BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS folder_entity (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id UUID,
    parent_id UUID,
    element_id_id UUID,
    creation_date TIMESTAMP,
    last_modification TIMESTAMP,
    shared BOOLEAN,
    deleted BOOLEAN,
    FOREIGN KEY (parent_id) REFERENCES folder_entity(id),
    FOREIGN KEY (element_id_id) REFERENCES element_entity(id)
);

CREATE TABLE IF NOT EXISTS file_entity (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    size BIGINT,
    user_id UUID,
    parent_id UUID,
    element_id_id UUID,
    creation_date TIMESTAMP,
    last_modification TIMESTAMP,
    shared BOOLEAN,
    deleted BOOLEAN,
    mime_type VARCHAR(255),
    FOREIGN KEY (parent_id) REFERENCES folder_entity(id),
    FOREIGN KEY (element_id_id) REFERENCES element_entity(id)
);

CREATE TABLE IF NOT EXISTS access_rule (
    id UUID PRIMARY KEY,
    user_id UUID,
    element_id UUID,
    access_type VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS shared_access (
    id UUID PRIMARY KEY,
    user_id UUID,
    element_id UUID,
    root BOOLEAN,
    shared_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trash_record (
    id UUID PRIMARY KEY,
    user_id UUID,
    element_id UUID,
    sharing BOOLEAN,
    access BOOLEAN,
    manager BOOLEAN,
    root BOOLEAN,
    deletion_date TIMESTAMP,
    expiration_date TIMESTAMP,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE'
);

-- =================================================================
-- SyncService Tables
-- =================================================================

CREATE TABLE IF NOT EXISTS snapshot_entity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS snapshot_element_entity (
    id UUID PRIMARY KEY,
    element_id UUID NOT NULL,
    snapshot_id UUID,
    parent_id UUID,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    hash VARCHAR(64) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    FOREIGN KEY (snapshot_id) REFERENCES snapshot_entity(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES snapshot_element_entity(id) ON DELETE CASCADE
);

-- =================================================================
-- Indexes
-- =================================================================
CREATE INDEX IF NOT EXISTS idx_user_info_email ON user_info(email);
CREATE INDEX IF NOT EXISTS idx_folder_user ON folder_entity(user_id);
CREATE INDEX IF NOT EXISTS idx_folder_parent ON folder_entity(parent_id);
CREATE INDEX IF NOT EXISTS idx_file_user ON file_entity(user_id);
CREATE INDEX IF NOT EXISTS idx_file_parent ON file_entity(parent_id);
CREATE INDEX IF NOT EXISTS idx_access_rule_user ON access_rule(user_id);
CREATE INDEX IF NOT EXISTS idx_access_rule_element ON access_rule(element_id);
CREATE INDEX IF NOT EXISTS idx_shared_access_user ON shared_access(user_id);
CREATE INDEX IF NOT EXISTS idx_shared_access_element ON shared_access(element_id);
CREATE INDEX IF NOT EXISTS idx_trash_record_user ON trash_record(user_id);
CREATE INDEX IF NOT EXISTS idx_trash_record_element ON trash_record(element_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_user ON snapshot_entity(user_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_element_snapshot ON snapshot_element_entity(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_element_element ON snapshot_element_entity(element_id);